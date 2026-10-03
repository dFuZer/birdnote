package com.dfuzer.birdnote.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.dfuzer.birdnote.domain.IntervalConfig
import com.dfuzer.birdnote.domain.IntervalName
import com.dfuzer.birdnote.domain.MISTAKE_TIME_PENALTY_SECONDS
import com.dfuzer.birdnote.domain.QUEUE_SIZE
import com.dfuzer.birdnote.domain.QUIZ_DURATION_SECONDS
import com.dfuzer.birdnote.domain.StaffInterval
import com.dfuzer.birdnote.domain.advanceIntervalQueue
import com.dfuzer.birdnote.domain.generateIntervalQueue
import com.dfuzer.birdnote.domain.isCorrect
import kotlin.random.Random
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class IntervalQuizUiState(
    val intervals: List<StaffInterval> = emptyList(),
    val score: Int = 0,
    val durationMillis: Long = QUIZ_DURATION_SECONDS * 1000L,
    val answersLocked: Boolean = false,
    val lastFeedbackCorrect: Boolean? = null,
    val answerFeedback: AnswerFeedback? = null,
    val finished: Boolean = false,
)

class IntervalQuizViewModel(
    private val config: IntervalConfig,
    private val random: Random = Random.Default,
    durationSeconds: Int = QUIZ_DURATION_SECONDS,
) : ViewModel() {

    private val durationMillis = durationSeconds * 1000L
    private val penaltyMillis = MISTAKE_TIME_PENALTY_SECONDS * 1000L

    private val _state = MutableStateFlow(
        IntervalQuizUiState(
            intervals = generateIntervalQueue(config, QUEUE_SIZE, random),
            durationMillis = durationMillis,
        ),
    )
    val state: StateFlow<IntervalQuizUiState> = _state.asStateFlow()

    private val _remainingMillis = MutableStateFlow(durationMillis)
    val remainingMillis: StateFlow<Long> = _remainingMillis.asStateFlow()

    private var timerJob: Job? = null

    init {
        startTimer()
    }

    fun onAnswer(answer: IntervalName) {
        val current = _state.value
        if (current.finished || current.answersLocked || current.intervals.isEmpty()) return
        val answered = current.intervals.first()
        val correct = isCorrect(answered, answer)
        val remaining = if (correct) {
            _remainingMillis.value
        } else {
            (_remainingMillis.value - penaltyMillis).coerceAtLeast(0L)
        }
        _remainingMillis.value = remaining
        val finished = remaining <= 0L
        _state.value = current.copy(
            intervals = advanceIntervalQueue(current.intervals, config, random),
            score = if (correct) current.score + 1 else current.score,
            lastFeedbackCorrect = correct,
            answerFeedback = AnswerFeedback(
                id = (current.answerFeedback?.id ?: 0) + 1,
                correct = correct,
                tones = listOf(
                    SoundTone(answered.lower.diatonicStep),
                    SoundTone(answered.upper.diatonicStep),
                ),
            ),
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
        fun factory(config: IntervalConfig): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return IntervalQuizViewModel(config) as T
                }
            }
    }
}

private const val TIMER_TICK_MILLIS = 50L
