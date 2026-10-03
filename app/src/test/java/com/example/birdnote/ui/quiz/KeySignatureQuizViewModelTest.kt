package com.example.birdnote.ui.quiz

import com.example.birdnote.domain.Accidental
import com.example.birdnote.domain.Clef
import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.KeySignatureConfig
import com.example.birdnote.domain.MISTAKE_TIME_PENALTY_SECONDS
import com.example.birdnote.domain.MajorKey
import com.example.birdnote.domain.NoteName
import com.example.birdnote.domain.QUIZ_DURATION_SECONDS
import com.example.birdnote.domain.isCorrect
import com.example.birdnote.domain.majorTriad
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
class KeySignatureQuizViewModelTest {
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
    fun correctAnswerIncrementsScoreAndPlaysTheMajorTriad() = runTest(testDispatcher) {
        val viewModel = KeySignatureQuizViewModel(
            KeySignatureConfig(1, ClefMode.SOL, null),
            Random(0),
        )
        try {
            val first = viewModel.state.value.signatures.first()
            assertEquals(Clef.SOL, first.clef)
            viewModel.onAnswer(first.key.tonic)
            assertEquals(true, viewModel.state.value.lastFeedbackCorrect)
            assertEquals(1, viewModel.state.value.score)
            val triad = majorTriad(first.key, Clef.SOL)
            assertEquals(
                triad.map { it.pitch.diatonicStep },
                viewModel.state.value.answerFeedback?.diatonicSteps,
            )
            assertEquals(
                triad.map { it.accidental },
                viewModel.state.value.answerFeedback?.tones?.map { it.accidental },
            )
            assertNotEquals(first.key, viewModel.state.value.signatures.first().key)
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun incorrectAnswerAppliesPenaltyAndStillAdvances() = runTest(testDispatcher) {
        val viewModel = KeySignatureQuizViewModel(
            KeySignatureConfig(1, ClefMode.FA, null),
            Random(1),
        )
        try {
            val first = viewModel.state.value.signatures.first()
            val wrong = NoteName.entries.first { !isCorrect(first, it) }
            viewModel.onAnswer(wrong)
            assertEquals(false, viewModel.state.value.lastFeedbackCorrect)
            assertEquals(0, viewModel.state.value.score)
            assertTrue(viewModel.state.value.answerFeedback?.tones.orEmpty().isEmpty())
            assertEquals(
                (QUIZ_DURATION_SECONDS - MISTAKE_TIME_PENALTY_SECONDS) * 1000L,
                viewModel.remainingMillis.value,
            )
            assertNotEquals(first, viewModel.state.value.signatures.first())
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun solFaScoresOnlyAfterBothTonicsAreCorrect() = runTest(testDispatcher) {
        val viewModel = KeySignatureQuizViewModel(
            KeySignatureConfig(4, ClefMode.SOL_FA, MajorKey.F_SHARP),
            Random(2),
        )
        try {
            assertEquals(Clef.SOL, viewModel.state.value.signatures.first().clef)
            viewModel.onAnswer(NoteName.FA)
            assertEquals(0, viewModel.state.value.score)
            assertTrue(viewModel.state.value.awaitingSecond)
            assertEquals(Clef.FA, viewModel.state.value.signatures.first().clef)
            assertEquals(
                listOf(Accidental.SHARP, Accidental.SHARP, Accidental.SHARP),
                viewModel.state.value.answerFeedback?.tones?.map { it.accidental },
            )
            viewModel.onAnswer(NoteName.FA)
            assertEquals(1, viewModel.state.value.score)
            assertFalse(viewModel.state.value.awaitingSecond)
            assertEquals(Clef.SOL, viewModel.state.value.signatures.first().clef)
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun solFaDoesNotScoreWhenEitherTonicIsWrong() = runTest(testDispatcher) {
        val viewModel = KeySignatureQuizViewModel(
            KeySignatureConfig(1, ClefMode.SOL_FA, MajorKey.C),
            Random(5),
        )
        try {
            viewModel.onAnswer(NoteName.RE)
            assertEquals(0, viewModel.state.value.score)
            assertEquals(Clef.FA, viewModel.state.value.signatures.first().clef)
            viewModel.onAnswer(NoteName.DO)
            assertEquals(0, viewModel.state.value.score)
            assertEquals(Clef.SOL, viewModel.state.value.signatures.first().clef)
            assertFalse(viewModel.state.value.awaitingSecond)
        } finally {
            viewModel.stop()
        }
    }

    @Test
    fun timerExpiryFinishesTheQuiz() = runTest(testDispatcher) {
        val viewModel = KeySignatureQuizViewModel(
            config = KeySignatureConfig(2, ClefMode.SOL, null),
            random = Random(2),
            durationSeconds = 3,
        )
        try {
            assertFalse(viewModel.state.value.finished)
            advanceTimeBy(50)
            runCurrent()
            assertEquals(3_000L - 50L, viewModel.remainingMillis.value)
            advanceTimeBy(2_950)
            runCurrent()
            assertTrue(viewModel.state.value.finished)
            assertEquals(0L, viewModel.remainingMillis.value)
        } finally {
            viewModel.stop()
        }
    }
}
