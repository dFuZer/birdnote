package com.example.birdnote.ui.quiz

import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.ChordConfig
import com.example.birdnote.domain.ChordQuality
import com.example.birdnote.domain.MISTAKE_TIME_PENALTY_SECONDS
import com.example.birdnote.domain.QUIZ_DURATION_SECONDS
import com.example.birdnote.domain.isCorrect
import com.example.birdnote.domain.label
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
class ChordQuizViewModelTest {
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
    fun correctAnswerIncrementsScoreAndAdvances() = runTest(testDispatcher) {
        val viewModel = ChordQuizViewModel(ChordConfig(1, ClefMode.SOL), Random(0))
        try {
            val first = viewModel.state.value.chords.first()
            viewModel.onAnswer(first.quality)
            assertTrue(viewModel.state.value.lastFeedbackCorrect == true)
            assertEquals(1, viewModel.state.value.score)
            assertEquals(null, viewModel.state.value.missHint)
            assertEquals(
                first.notes.map { it.pitch.diatonicStep },
                viewModel.state.value.answerFeedback?.diatonicSteps,
            )
            assertEquals(
                first.notes.map { it.accidental },
                viewModel.state.value.answerFeedback?.tones?.map { it.accidental },
            )
            assertNotEquals(first, viewModel.state.value.chords.first())
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun incorrectAnswerAppliesPenaltyAndStillAdvances() = runTest(testDispatcher) {
        val viewModel = ChordQuizViewModel(ChordConfig(1, ClefMode.SOL), Random(1))
        try {
            val first = viewModel.state.value.chords.first()
            val wrong = ChordQuality.entries.first { !isCorrect(first, it) }
            viewModel.onAnswer(wrong)
            assertEquals(false, viewModel.state.value.lastFeedbackCorrect)
            assertEquals(first.quality.label(), viewModel.state.value.missHint?.label)
            assertEquals(0, viewModel.state.value.score)
            assertEquals(
                (QUIZ_DURATION_SECONDS - MISTAKE_TIME_PENALTY_SECONDS) * 1000L,
                viewModel.remainingMillis.value,
            )
            assertNotEquals(first, viewModel.state.value.chords.first())
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun timerExpiryFinishesTheQuiz() = runTest(testDispatcher) {
        val viewModel = ChordQuizViewModel(
            config = ChordConfig(2, ClefMode.FA),
            random = Random(2),
            durationSeconds = 3,
        )
        try {
            assertFalse(viewModel.state.value.finished)
            val before = viewModel.state.value
            advanceTimeBy(50)
            runCurrent()
            assertEquals(before, viewModel.state.value)
            assertEquals(before.durationMillis - 50L, viewModel.remainingMillis.value)
            advanceTimeBy(2_950)
            runCurrent()
            assertTrue(viewModel.state.value.finished)
            assertEquals(0L, viewModel.remainingMillis.value)
        } finally {
            viewModel.stop()
        }
    }
}
