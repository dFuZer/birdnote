package com.example.birdnote.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.birdnote.domain.Accidental
import com.example.birdnote.domain.MISTAKE_TIME_PENALTY_SECONDS
import com.example.birdnote.domain.NoteName
import com.example.birdnote.domain.NoteNaming
import com.example.birdnote.domain.PracticeConfig
import com.example.birdnote.domain.QUIZ_DURATION_SECONDS
import com.example.birdnote.domain.StaffNote
import com.example.birdnote.domain.advanceQueue
import com.example.birdnote.domain.generateQueue
import com.example.birdnote.domain.isCorrect
import com.example.birdnote.domain.label
import com.example.birdnote.domain.queueSize
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SoundTone(
    val diatonicStep: Int,
    val accidental: Accidental = Accidental.NONE,
)

data class AnswerFeedback(
    val id: Int,
    val correct: Boolean,
    val tones: List<SoundTone>,
) {
    val diatonicSteps: List<Int>
        get() = tones.map { it.diatonicStep }
}

data class MissHint(
    val id: Int,
    val label: String,
)

data class QuizUiState(
    val notes: List<StaffNote> = emptyList(),
    val score: Int = 0,
    val durationMillis: Long = QUIZ_DURATION_SECONDS * 1000L,
    val answersLocked: Boolean = false,
    val lastFeedbackCorrect: Boolean? = null,
    val answerFeedback: AnswerFeedback? = null,
    val missHint: MissHint? = null,
    val finished: Boolean = false,
)

private const val TIMER_TICK_MILLIS = 50L

class QuizViewModel(
    private val config: PracticeConfig,
    private val random: Random = Random.Default,
    durationSeconds: Int = QUIZ_DURATION_SECONDS,
    private val noteNaming: NoteNaming = NoteNaming.SOLFEGE,
) : ViewModel() {

    private val durationMillis = durationSeconds * 1000L
    private val penaltyMillis = MISTAKE_TIME_PENALTY_SECONDS * 1000L

    private val _state = MutableStateFlow(
        QuizUiState(
            notes = generateQueue(config, config.clefMode.queueSize(), random),
            durationMillis = durationMillis,
        ),
    )
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private val _remainingMillis = MutableStateFlow(durationMillis)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private var timerJob: Job? = null

    init {
        startTimer()
    }

    fun onAnswer(answer: NoteName) {
        val current = _state.value
        if (current.finished || current.answersLocked || current.notes.isEmpty()) return
        val answered = current.notes.first()
        val correct = isCorrect(answered, answer)
        val remaining = if (correct) {
            _remainingMillis.value
        } else {
            (_remainingMillis.value - penaltyMillis).coerceAtLeast(0L)
        }
        _remainingMillis.value = remaining
        val finished = remaining <= 0L
        val feedbackId = (current.answerFeedback?.id ?: 0) + 1
        _state.value = current.copy(
            notes = advanceQueue(current.notes, config, random),
            score = if (correct) current.score + 1 else current.score,
            lastFeedbackCorrect = correct,
            answerFeedback = AnswerFeedback(
                id = feedbackId,
                correct = correct,
                tones = listOf(SoundTone(answered.pitch.diatonicStep)),
            ),
            missHint = if (correct) {
                current.missHint
            } else {
                MissHint(feedbackId, answered.pitch.noteName.label(noteNaming))
            },
            finished = finished,
            answersLocked = current.answersLocked || finished,
        )
        if (finished) timerJob?.cancel()
    }

    fun stop() {
        timerJob?.cancel()
        _state.update { it.copy(finished = true, answersLocked = true) }
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
                            current.copy(finished = true, answersLocked = true)
                        }
                    }
                    break
                }
            }
        }
    }

    companion object {
        fun factory(
            config: PracticeConfig,
            noteNaming: NoteNaming = NoteNaming.SOLFEGE,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return QuizViewModel(config, noteNaming = noteNaming) as T
                }
            }
    }
}
