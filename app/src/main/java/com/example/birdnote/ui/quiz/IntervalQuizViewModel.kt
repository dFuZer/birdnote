package com.example.birdnote.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.birdnote.domain.IntervalConfig
import com.example.birdnote.domain.IntervalName
import com.example.birdnote.domain.MISTAKE_TIME_PENALTY_SECONDS
import com.example.birdnote.domain.QUEUE_SIZE
import com.example.birdnote.domain.QUIZ_DURATION_SECONDS
import com.example.birdnote.domain.StaffInterval
import com.example.birdnote.domain.advanceIntervalQueue
import com.example.birdnote.domain.generateIntervalQueue
import com.example.birdnote.domain.isCorrect
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
    val remainingMillis: Long = QUIZ_DURATION_SECONDS * 1000L,
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
            remainingMillis = durationMillis,
            durationMillis = durationMillis,
        ),
    )
    val state: StateFlow<IntervalQuizUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    init {
        startTimer()
    }

    fun onAnswer(answer: IntervalName) {
        var shouldStop = false
        _state.update { current ->
            if (current.finished || current.answersLocked || current.intervals.isEmpty()) {
                current
            } else {
                val answered = current.intervals.first()
                val correct = isCorrect(answered, answer)
                val remaining = if (correct) {
                    current.remainingMillis
                } else {
                    (current.remainingMillis - penaltyMillis).coerceAtLeast(0L)
                }
                val finished = remaining <= 0L
                shouldStop = finished
                current.copy(
                    intervals = advanceIntervalQueue(current.intervals, config, random),
                    score = if (correct) current.score + 1 else current.score,
                    remainingMillis = remaining,
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
            }
        }
        if (shouldStop) timerJob?.cancel()
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
                var shouldFinish = false
                _state.update { current ->
                    if (current.finished) {
                        current
                    } else {
                        val next = (current.remainingMillis - TIMER_TICK_MILLIS).coerceAtLeast(0L)
                        shouldFinish = next <= 0L
                        current.copy(
                            remainingMillis = next,
                            finished = shouldFinish,
                            answersLocked = current.answersLocked || shouldFinish,
                        )
                    }
                }
                if (shouldFinish) break
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
