package com.dfuzer.birdnote.ui.quiz

import com.dfuzer.birdnote.domain.ChordConfig
import com.dfuzer.birdnote.domain.ChordQuality
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.IntervalConfig
import com.dfuzer.birdnote.domain.IntervalName
import com.dfuzer.birdnote.domain.NoteName
import com.dfuzer.birdnote.domain.PracticeConfig
import com.dfuzer.birdnote.domain.PracticeMode
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
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class QuizSessionTest(private val mode: PracticeMode) {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun timerTicksDoNotPublishBoardStateAndExpiryRejectsFurtherAnswers() = runTest(dispatcher) {
        withQuiz(1) { vm, correct, _ ->
            val initial = vm.state.value
            advanceTimeBy(50)
            runCurrent()
            assertSame(initial, vm.state.value)
            assertEquals(950L, vm.remainingMillis.value)
            advanceTimeBy(950)
            runCurrent()
            val final = vm.state.value
            assertTrue(final.finished)
            assertEquals(0L, vm.remainingMillis.value)
            correct()
            assertSame(final, vm.state.value)
        }
    }

    @Test
    fun rapidAnswersAdvanceExactlyOnceEachAndApplyOnlyMistakePenalties() = runTest(dispatcher) {
        withQuiz(45) { vm, correct, wrong ->
            repeat(50) { correct() }
            assertEquals(50, vm.state.value.score)
            assertEquals(50, vm.state.value.answerFeedback?.id)
            assertEquals(45_000L, vm.remainingMillis.value)
            assertFalse(vm.state.value.finished)
            wrong()
            assertEquals(50, vm.state.value.score)
            assertEquals(51, vm.state.value.answerFeedback?.id)
            assertEquals(false, vm.state.value.answerFeedback?.correct)
            assertEquals(42_000L, vm.remainingMillis.value)
        }
    }

    @Test
    fun terminalMistakeStillAdvancesAndEmitsFeedbackExactlyOnce() = runTest(dispatcher) {
        withQuiz(2) { vm, correct, wrong ->
            val initial = vm.state.value.questions
            wrong()
            val final = vm.state.value
            assertTrue(final.finished)
            assertEquals(0L, vm.remainingMillis.value)
            assertEquals(0, final.score)
            assertEquals(1, final.answerFeedback?.id)
            assertEquals(initial.drop(1), final.questions.dropLast(1))
            correct()
            wrong()
            advanceTimeBy(5_000)
            runCurrent()
            assertEquals(final, vm.state.value)
            assertEquals(0L, vm.remainingMillis.value)
        }
    }

    @Test
    fun stopIsImmediateIdempotentAndCancelsTheTimer() = runTest(dispatcher) {
        withQuiz(45) { vm, correct, wrong ->
            correct()
            vm.stop()
            val final = vm.state.value
            assertTrue(final.finished)
            vm.stop()
            correct()
            wrong()
            advanceTimeBy(5_000)
            runCurrent()
            assertEquals(final, vm.state.value)
            assertEquals(45_000L, vm.remainingMillis.value)
        }
    }

    private fun withQuiz(
        duration: Int,
        check: (QuizViewModel<*, *>, correct: () -> Unit, wrong: () -> Unit) -> Unit,
    ) {
        fun <Q, A> run(vm: QuizViewModel<Q, A>, answer: (Q) -> A, wrong: (Q) -> A) {
            try {
                check(vm, { vm.onAnswer(answer(vm.state.value.questions.first())) },
                    { vm.onAnswer(wrong(vm.state.value.questions.first())) })
            } finally {
                vm.stop()
            }
        }
        when (mode) {
            PracticeMode.NOTES -> run(QuizViewModel.notes(PracticeConfig(4, ClefMode.SOL_FA), Random(582), duration),
                { it.pitch.noteName }, { note -> NoteName.entries.first { it != note.pitch.noteName } })
            PracticeMode.INTERVALS -> run(QuizViewModel.intervals(IntervalConfig(4), Random(582), duration),
                { it.name }, { interval -> IntervalName.entries.first { it != interval.name } })
            PracticeMode.CHORDS -> run(QuizViewModel.chords(ChordConfig(4, ClefMode.ALTO), Random(582), duration),
                { it.quality }, { chord -> ChordQuality.entries.first { it != chord.quality } })
        }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun modes(): List<Array<PracticeMode>> = PracticeMode.entries.map { arrayOf(it) }
    }
}
