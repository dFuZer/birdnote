package com.dfuzer.birdnote.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dfuzer.birdnote.audio.AnswerFeedback
import com.dfuzer.birdnote.audio.SoundTone
import com.dfuzer.birdnote.domain.Accidental
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.Pitch
import com.dfuzer.birdnote.domain.spellChord
import com.dfuzer.birdnote.domain.ChordConfig
import com.dfuzer.birdnote.domain.ChordQuality
import com.dfuzer.birdnote.domain.IntervalConfig
import com.dfuzer.birdnote.domain.IntervalName
import com.dfuzer.birdnote.domain.MISTAKE_TIME_PENALTY_SECONDS
import com.dfuzer.birdnote.domain.NoteName
import com.dfuzer.birdnote.domain.PracticeChord
import com.dfuzer.birdnote.domain.PracticeConfig
import com.dfuzer.birdnote.domain.QUEUE_SIZE
import com.dfuzer.birdnote.domain.QUIZ_DURATION_SECONDS
import com.dfuzer.birdnote.domain.StaffInterval
import com.dfuzer.birdnote.domain.StaffNote
import com.dfuzer.birdnote.domain.advanceChordQueue
import com.dfuzer.birdnote.domain.advanceIntervalQueue
import com.dfuzer.birdnote.domain.advanceQueue
import com.dfuzer.birdnote.domain.generateChordQueue
import com.dfuzer.birdnote.domain.generateIntervalQueue
import com.dfuzer.birdnote.domain.generateQueue
import com.dfuzer.birdnote.domain.isCorrect
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuizUiState<Question>(
    val questions: List<Question>,
    val score: Int = 0,
    val durationMillis: Long = QUIZ_DURATION_SECONDS * 1000L,
    val answerFeedback: AnswerFeedback? = null,
    val finished: Boolean = false,
    val paused: Boolean = false,
    val lastQuestion: Question? = null,
)

private const val TIMER_TICK_MILLIS = 50L

class QuizViewModel<Question, Answer> private constructor(
    initialQuestions: List<Question>,
    private val advance: (List<Question>) -> List<Question>,
    private val correctAnswer: (Question, Answer) -> Boolean,
    private val tones: (Question) -> List<SoundTone>,
    durationSeconds: Int,
    private val savedState: SavedStateHandle,
    private val decodeQuestion: (String) -> Question,
    private val nowMillis: () -> Long,
) : ViewModel() {
    private val durationMillis = durationSeconds * 1000L
    private val penaltyMillis = MISTAKE_TIME_PENALTY_SECONDS * 1000L

    private val _state = MutableStateFlow(
        QuizUiState(
            questions = savedState.get<ArrayList<String>>("questions")?.let { stored ->
                runCatching { stored.map(decodeQuestion) }.getOrNull()?.takeIf { it.isNotEmpty() }
            } ?: initialQuestions,
            score = savedState["score"] ?: 0,
            finished = savedState["finished"] ?: false,
            paused = savedState.contains("remaining"),
            durationMillis = durationMillis,
        ),
    )
    val state: StateFlow<QuizUiState<Question>> = _state.asStateFlow()

    private val _remainingMillis = MutableStateFlow(savedState.get<Long>("remaining") ?: durationMillis)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private var timerJob: Job? = null
    private var startedAt = nowMillis()
    private var remainingAtStart = _remainingMillis.value

    init {
        persistBoard()
        if (!_state.value.finished && !_state.value.paused) startTimer()
    }

    fun onAnswer(answer: Answer): AnswerFeedback? {
        if (_state.value.finished || _state.value.paused) return null
        updateRemaining()
        val current = _state.value
        if (current.finished || current.questions.isEmpty()) return null
        val answered = current.questions.first()
        val correct = correctAnswer(answered, answer)
        val remaining = if (correct) {
            _remainingMillis.value
        } else {
            (_remainingMillis.value - penaltyMillis).coerceAtLeast(0L)
        }
        _remainingMillis.value = remaining
        remainingAtStart = remaining
        startedAt = nowMillis()
        val finished = remaining <= 0L
        val feedback = AnswerFeedback(
            id = (current.answerFeedback?.id ?: 0) + 1,
            correct = correct,
            tones = tones(answered),
        )
        _state.value = current.copy(
            questions = advance(current.questions),
            lastQuestion = answered,
            score = if (correct) current.score + 1 else current.score,
            answerFeedback = feedback,
            finished = finished,
        )
        persistBoard()
        if (finished) timerJob?.cancel()
        return feedback
    }

    fun stop() {
        if (_state.value.finished) return
        if (!_state.value.paused) updateRemaining()
        timerJob?.cancel()
        _state.update { it.copy(finished = true) }
        persistBoard()
    }

    fun pause() {
        if (_state.value.finished || _state.value.paused) return
        updateRemaining()
        timerJob?.cancel()
        _state.update { it.copy(paused = true) }
        persistBoard()
    }

    fun resume() {
        if (_state.value.finished || !_state.value.paused) return
        _state.update { it.copy(paused = false) }
        startTimer()
    }

    private fun persistBoard() {
        savedState["questions"] = ArrayList(_state.value.questions.map(::encodeQuestion))
        savedState["score"] = _state.value.score
        savedState["finished"] = _state.value.finished
        savedState["remaining"] = _remainingMillis.value
    }

    private fun updateRemaining() {
        val elapsed = (nowMillis() - startedAt).coerceAtLeast(0L)
        val next = (remainingAtStart - elapsed).coerceAtLeast(0L)
        _remainingMillis.value = next
        if (next == 0L && !_state.value.finished) {
            _state.update { it.copy(finished = true) }
            persistBoard()
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    private fun startTimer() {
        timerJob?.cancel()
        startedAt = nowMillis()
        remainingAtStart = _remainingMillis.value
        timerJob = viewModelScope.launch {
            while (!_state.value.finished && !_state.value.paused) {
                delay(TIMER_TICK_MILLIS)
                updateRemaining()
            }
        }
    }

    companion object {
        fun notes(
            config: PracticeConfig,
            random: Random = Random.Default,
            durationSeconds: Int = QUIZ_DURATION_SECONDS,
            savedState: SavedStateHandle = SavedStateHandle(),
            nowMillis: () -> Long = { System.nanoTime() / 1_000_000L },
        ): QuizViewModel<StaffNote, NoteName> = QuizViewModel(
            initialQuestions = generateQueue(config, QUEUE_SIZE, random),
            advance = { advanceQueue(it, config, random) },
            correctAnswer = ::isCorrect,
            tones = { listOf(SoundTone(it.pitch.diatonicStep)) },
            durationSeconds = durationSeconds,
            savedState = savedState,
            decodeQuestion = ::decodeNote,
            nowMillis = nowMillis,
        )

        fun intervals(
            config: IntervalConfig,
            random: Random = Random.Default,
            durationSeconds: Int = QUIZ_DURATION_SECONDS,
            savedState: SavedStateHandle = SavedStateHandle(),
            nowMillis: () -> Long = { System.nanoTime() / 1_000_000L },
        ): QuizViewModel<StaffInterval, IntervalName> = QuizViewModel(
            initialQuestions = generateIntervalQueue(config, QUEUE_SIZE, random),
            advance = { advanceIntervalQueue(it, config, random) },
            correctAnswer = ::isCorrect,
            tones = { listOf(SoundTone(it.lower.diatonicStep), SoundTone(it.upper.diatonicStep)) },
            durationSeconds = durationSeconds,
            savedState = savedState,
            decodeQuestion = ::decodeInterval,
            nowMillis = nowMillis,
        )

        fun chords(
            config: ChordConfig,
            random: Random = Random.Default,
            durationSeconds: Int = QUIZ_DURATION_SECONDS,
            savedState: SavedStateHandle = SavedStateHandle(),
            nowMillis: () -> Long = { System.nanoTime() / 1_000_000L },
        ): QuizViewModel<PracticeChord, ChordQuality> = QuizViewModel(
            initialQuestions = generateChordQueue(config, QUEUE_SIZE, random),
            advance = { advanceChordQueue(it, config, random) },
            correctAnswer = ::isCorrect,
            tones = { chord -> chord.notes.map { SoundTone(it.pitch.diatonicStep, it.accidental) } },
            durationSeconds = durationSeconds,
            savedState = savedState,
            decodeQuestion = ::decodeChord,
            nowMillis = nowMillis,
        )

        fun <Question, Answer> factory(
            create: (SavedStateHandle) -> QuizViewModel<Question, Answer>,
        ): ViewModelProvider.Factory = viewModelFactory { initializer { create(createSavedStateHandle()) } }
    }
}

private fun encodeQuestion(question: Any?): String = when (question) {
    is StaffNote -> "${question.pitch.diatonicStep},${question.clef.name},${question.accidental.name}"
    is StaffInterval -> "${question.lower.diatonicStep},${question.upper.diatonicStep},${question.clef.name}"
    is PracticeChord -> "${question.root.diatonicStep},${question.quality.name},${question.clef.name}"
    else -> error("Unsupported quiz question")
}

private fun decodeNote(value: String): StaffNote = value.split(',').let {
    StaffNote(Pitch(it[0].toInt()), Clef.valueOf(it[1]), Accidental.valueOf(it[2]))
}
private fun decodeInterval(value: String): StaffInterval = value.split(',').let {
    StaffInterval(Pitch(it[0].toInt()), Pitch(it[1].toInt()), Clef.valueOf(it[2]))
}
private fun decodeChord(value: String): PracticeChord = value.split(',').let {
    checkNotNull(spellChord(Pitch(it[0].toInt()), ChordQuality.valueOf(it[1]), Clef.valueOf(it[2])))
}
