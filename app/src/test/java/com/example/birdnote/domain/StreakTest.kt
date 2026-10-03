package com.example.birdnote.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreakTest {
    @Test
    fun streakStartsAfterSeveralCorrectAnswers() {
        assertFalse(streakActive(0))
        assertFalse(streakActive(STREAK_REWARD_LENGTH - 1))
        assertTrue(streakActive(STREAK_REWARD_LENGTH))
        assertTrue(streakActive(STREAK_REWARD_LENGTH + 4))
    }

    @Test
    fun answersBelowTheStreakDoNotSparkle() {
        val first = streakAfterAnswer(0, null, correct = true, nowMillis = 1_000L)
        assertEquals(1, first.correctInARow)
        assertNull(first.sparklePopMillis)

        val second = streakAfterAnswer(
            first.correctInARow,
            first.sparklePopMillis,
            correct = true,
            nowMillis = 1_200L,
        )
        assertEquals(STREAK_REWARD_LENGTH - 1, second.correctInARow)
        assertNull(second.sparklePopMillis)
    }

    @Test
    fun reachingTheStreakPopsASparkle() {
        val started = streakAfterAnswer(
            STREAK_REWARD_LENGTH - 1,
            sparklePopMillis = null,
            correct = true,
            nowMillis = 5_000L,
        )
        assertEquals(STREAK_REWARD_LENGTH, started.correctInARow)
        assertEquals(5_000L, started.sparklePopMillis)
    }

    @Test
    fun sparklePopsAtMostOncePerCooldown() {
        val started = streakAfterAnswer(
            STREAK_REWARD_LENGTH - 1,
            sparklePopMillis = null,
            correct = true,
            nowMillis = 10_000L,
        )
        val tooSoon = streakAfterAnswer(
            started.correctInARow,
            started.sparklePopMillis,
            correct = true,
            nowMillis = 10_000L + SPARKLE_COOLDOWN_MILLIS - 1,
        )
        assertEquals(STREAK_REWARD_LENGTH + 1, tooSoon.correctInARow)
        assertEquals(started.sparklePopMillis, tooSoon.sparklePopMillis)

        val ready = streakAfterAnswer(
            tooSoon.correctInARow,
            tooSoon.sparklePopMillis,
            correct = true,
            nowMillis = 10_000L + SPARKLE_COOLDOWN_MILLIS,
        )
        assertEquals(10_000L + SPARKLE_COOLDOWN_MILLIS, ready.sparklePopMillis)
        assertEquals(STREAK_REWARD_LENGTH + 2, ready.correctInARow)
    }

    @Test
    fun wrongAnswerClearsTheStreakAndTheSparkle() {
        val cleared = streakAfterAnswer(
            correctInARow = STREAK_REWARD_LENGTH + 2,
            sparklePopMillis = 4_000L,
            correct = false,
            nowMillis = 8_000L,
        )
        assertEquals(0, cleared.correctInARow)
        assertNull(cleared.sparklePopMillis)
    }
}
