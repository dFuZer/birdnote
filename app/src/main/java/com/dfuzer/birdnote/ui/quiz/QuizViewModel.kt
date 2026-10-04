package com.dfuzer.birdnote.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dfuzer.birdnote.audio.AnswerFeedback
import com.dfuzer.birdnote.audio.SoundTone
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
)

private const val TIMER_TICK_MILLIS = 50L

class QuizViewModel<Question, Answer> private constructor(
    initialQuestions: List<Question>,
    private val advance: (List<Question>) -> List<Question>,
    private val correctAnswer: (Question, Answer) -> Boolean,
    private val tones: (Question) -> List<SoundTone>,
    durationSeconds: Int,
) : ViewModel() {
    private val durationMillis = durationSeconds * 1000L
    private val penaltyMillis = MISTAKE_TIME_PENALTY_SECONDS * 1000L

    private val _state = MutableStateFlow(
        QuizUiState(
            questions = initialQuestions,
            durationMillis = durationMillis,
        ),
    )
    val state: StateFlow<QuizUiState<Question>> = _state.asStateFlow()

    private val _remainingMillis = MutableStateFlow(durationMillis)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private var timerJob: Job? = null

    init {
        startTimer()
    }

    fun onAnswer(answer: Answer) {
        val current = _state.value
        if (current.finished || current.questions.isEmpty()) return
        val answered = current.questions.first()
        val correct = correctAnswer(answered, answer)
        val remaining = if (correct) {
            _remainingMillis.value
        } else {
            (_remainingMillis.value - penaltyMillis).coerceAtLeast(0L)
        }
        _remainingMillis.value = remaining
        val finished = remaining <= 0L
        _state.value = current.copy(
            questions = advance(current.questions),
            score = if (correct) current.score + 1 else current.score,
            answerFeedback = AnswerFeedback(
                id = (current.answerFeedback?.id ?: 0) + 1,
                correct = correct,
                tones = tones(answered),
            ),
            finished = finished,
        )
        if (finished) timerJob?.cancel()
    }

    fun stop() {
        timerJob?.cancel()
        _state.update { it.copy(finished = true) }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                delay(TIMER_TICK_MILLIS)
                if (_state.value.finished) break
                val next = (_remainingMillis.value - TIMER_TICK_MILLIS).coerceAtLeast(0L)
                _remainingMillis.value = next
                if (next <= 0L) {
                    _state.update { current ->
                        if (current.finished) {
                            current
                        } else {
                            current.copy(finished = true)
                        }
                    }
                    break
                }
            }
        }
    }

    companion object {
        fun notes(
            config: PracticeConfig,
            random: Random = Random.Default,
            durationSeconds: Int = QUIZ_DURATION_SECONDS,
        ): QuizViewModel<StaffNote, NoteName> = QuizViewModel(
            initialQuestions = generateQueue(config, QUEUE_SIZE, random),
            advance = { advanceQueue(it, config, random) },
            correctAnswer = ::isCorrect,
            tones = { listOf(SoundTone(it.pitch.diatonicStep)) },
            durationSeconds = durationSeconds,
        )

        fun intervals(
            config: IntervalConfig,
            random: Random = Random.Default,
            durationSeconds: Int = QUIZ_DURATION_SECONDS,
        ): QuizViewModel<StaffInterval, IntervalName> = QuizViewModel(
            initialQuestions = generateIntervalQueue(config, QUEUE_SIZE, random),
            advance = { advanceIntervalQueue(it, config, random) },
            correctAnswer = ::isCorrect,
            tones = { listOf(SoundTone(it.lower.diatonicStep), SoundTone(it.upper.diatonicStep)) },
            durationSeconds = durationSeconds,
        )

        fun chords(
            config: ChordConfig,
            random: Random = Random.Default,
            durationSeconds: Int = QUIZ_DURATION_SECONDS,
        ): QuizViewModel<PracticeChord, ChordQuality> = QuizViewModel(
            initialQuestions = generateChordQueue(config, QUEUE_SIZE, random),
            advance = { advanceChordQueue(it, config, random) },
            correctAnswer = ::isCorrect,
            tones = { chord -> chord.notes.map { SoundTone(it.pitch.diatonicStep, it.accidental) } },
            durationSeconds = durationSeconds,
        )

        fun <Question, Answer> factory(
            create: () -> QuizViewModel<Question, Answer>,
        ): ViewModelProvider.Factory = viewModelFactory { initializer { create() } }
    }
}
