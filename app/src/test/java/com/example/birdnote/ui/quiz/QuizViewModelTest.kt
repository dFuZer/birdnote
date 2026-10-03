package com.example.birdnote.ui.quiz

import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.MISTAKE_TIME_PENALTY_SECONDS
import com.example.birdnote.domain.NoteName
import com.example.birdnote.domain.PracticeConfig
import com.example.birdnote.domain.QUIZ_DURATION_SECONDS
import com.example.birdnote.domain.RoundMode
import com.example.birdnote.domain.isCorrect
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun correctAnswerIncrementsScoreAndAdvanceChangesQueue() = runTest(testDispatcher) {
        val viewModel = QuizViewModel(PracticeConfig(1, ClefMode.SOL), Random(0))
        try {
            val first = viewModel.state.value.notes.first()
            viewModel.onAnswer(first.pitch.noteName)
            assertTrue(viewModel.state.value.lastFeedbackCorrect == true)
            assertEquals(true, viewModel.state.value.answerFeedback?.correct)
            assertEquals(listOf(first.pitch.diatonicStep), viewModel.state.value.answerFeedback?.diatonicSteps)
            assertEquals(1, viewModel.state.value.score)
            assertFalse(viewModel.state.value.answersLocked)
            assertNotEquals(first, viewModel.state.value.notes.first())
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun incorrectAnswerDoesNotIncrementScoreButStillAdvances() = runTest(testDispatcher) {
        val viewModel = QuizViewModel(PracticeConfig(1, ClefMode.SOL), Random(1))
        try {
            val first = viewModel.state.value.notes.first()
            val wrong = NoteName.entries.first { !isCorrect(first, it) }
            viewModel.onAnswer(wrong)
            assertEquals(false, viewModel.state.value.lastFeedbackCorrect)
            assertEquals(false, viewModel.state.value.answerFeedback?.correct)
            assertEquals(0, viewModel.state.value.score)
            assertEquals(
                (QUIZ_DURATION_SECONDS - MISTAKE_TIME_PENALTY_SECONDS) * 1000L,
                viewModel.remainingMillis.value,
            )
            assertFalse(viewModel.state.value.answersLocked)
            assertNotEquals(first, viewModel.state.value.notes.first())
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun timerExpiryFinishesTheQuiz() = runTest(testDispatcher) {
        val viewModel = QuizViewModel(
            config = PracticeConfig(2, ClefMode.FA),
            random = Random(2),
            durationSeconds = 3,
        )
        try {
            assertFalse(viewModel.state.value.finished)
            advanceTimeBy(3_000)
            runCurrent()
            assertTrue(viewModel.state.value.finished)
            assertEquals(0L, viewModel.remainingMillis.value)
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun timerTickLeavesTheBoardUnchanged() = runTest(testDispatcher) {
        val viewModel = QuizViewModel(PracticeConfig(1, ClefMode.SOL), Random(5))
        try {
            val before = viewModel.state.value
            advanceTimeBy(50)
            runCurrent()
            assertEquals(before, viewModel.state.value)
            assertEquals(before.durationMillis - 50L, viewModel.remainingMillis.value)
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun mistakeThatExhaustsTimeFinishesTheQuiz() = runTest(testDispatcher) {
        val viewModel = QuizViewModel(
            config = PracticeConfig(1, ClefMode.SOL),
            random = Random(4),
            durationSeconds = 2,
        )
        try {
            val first = viewModel.state.value.notes.first()
            val wrong = NoteName.entries.first { !isCorrect(first, it) }
            viewModel.onAnswer(wrong)
            assertTrue(viewModel.state.value.finished)
            assertEquals(0L, viewModel.remainingMillis.value)
            assertTrue(viewModel.state.value.answersLocked)
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun stopFinishesImmediately() = runTest(testDispatcher) {
        val viewModel = QuizViewModel(PracticeConfig(1, ClefMode.SOL_FA), Random(3))
        viewModel.stop()
        assertTrue(viewModel.state.value.finished)
    }

    @Test
    fun practiceMissDoesNotPenalizeAndCountsGuessesOverTwoSeconds() = runTest(testDispatcher) {
        val viewModel = QuizViewModel(
            PracticeConfig(1, ClefMode.SOL),
            Random(1),
            roundMode = RoundMode.PRACTICE,
        )
        try {
            val started = viewModel.remainingMillis.value
            val first = viewModel.state.value.notes.first()
            val wrong = NoteName.entries.first { !isCorrect(first, it) }
            viewModel.onAnswer(wrong)
            assertFalse(viewModel.state.value.finished)
            assertFalse(viewModel.state.value.answersLocked)
            assertEquals(0, viewModel.state.value.score)
            assertEquals(false, viewModel.state.value.answerFeedback?.correct)
            assertEquals(started, viewModel.remainingMillis.value)
            assertNotEquals(first, viewModel.state.value.notes.first())
            assertEquals(30, viewModel.guessesPerMinute.value)

            advanceTimeBy(1_000)
            runCurrent()
            val second = viewModel.state.value.notes.first()
            viewModel.onAnswer(second.pitch.noteName)
            assertEquals(1, viewModel.state.value.score)
            assertEquals(60, viewModel.guessesPerMinute.value)
            assertFalse(viewModel.state.value.finished)

            advanceTimeBy(1_050)
            runCurrent()
            assertEquals(30, viewModel.guessesPerMinute.value)
            assertFalse(viewModel.state.value.finished)

            advanceTimeBy(QUIZ_DURATION_SECONDS * 1000L)
            runCurrent()
            assertFalse(viewModel.state.value.finished)
            assertEquals(started, viewModel.remainingMillis.value)
            assertEquals(0, viewModel.guessesPerMinute.value)
            viewModel.stop()
            assertTrue(viewModel.state.value.finished)
        } finally {
            viewModel.stop()
        }
    }
}
