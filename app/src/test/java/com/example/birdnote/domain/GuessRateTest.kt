package com.example.birdnote.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GuessRateTest {
    @Test
    fun emptyWindowIsZero() {
        assertEquals(0, guessesPerMinute(emptyList(), nowMillis = 0L))
        assertEquals(0, guessesPerMinute(emptyList(), nowMillis = 5_000L))
    }

    @Test
    fun oneTapInsideTheWindowIsThirtyPerMinute() {
        assertEquals(2_000L, GUESS_RATE_WINDOW_MILLIS)
        assertEquals(30, guessesPerMinute(listOf(0L), nowMillis = 0L))
        assertEquals(30, guessesPerMinute(listOf(1_500L), nowMillis = 1_500L))
    }

    @Test
    fun tapsScaleWithHowManyLandInTheWindow() {
        assertEquals(60, guessesPerMinute(listOf(0L, 100L), nowMillis = 100L))
        assertEquals(90, guessesPerMinute(listOf(0L, 0L, 400L), nowMillis = 400L))
    }

    @Test
    fun earlyRoundUsesTheFullWindowNotElapsedTime() {
        val taps = listOf(0L, 400L)
        assertEquals(60, guessesPerMinute(taps, nowMillis = 500L))
    }

    @Test
    fun tapOnTheWindowEdgeStillCounts() {
        assertEquals(30, guessesPerMinute(listOf(0L), nowMillis = 2_000L))
    }

    @Test
    fun tapJustOutsideTheWindowIsDropped() {
        assertEquals(0, guessesPerMinute(listOf(0L), nowMillis = 2_001L))
        assertEquals(30, guessesPerMinute(listOf(0L, 1_000L), nowMillis = 2_001L))
    }

    @Test
    fun futureTapsAreIgnored() {
        assertEquals(30, guessesPerMinute(listOf(100L, 5_000L), nowMillis = 100L))
    }

    @Test
    fun orderDoesNotMatter() {
        val taps = listOf(1_800L, 200L, 900L)
        assertEquals(90, guessesPerMinute(taps, nowMillis = 2_000L))
        assertEquals(60, guessesPerMinute(taps, nowMillis = 2_201L))
    }

    @Test
    fun customWindowScalesToAMinute() {
        assertEquals(60, guessesPerMinute(listOf(0L), nowMillis = 0L, windowMillis = 1_000L))
        assertEquals(120, guessesPerMinute(listOf(0L, 100L), nowMillis = 500L, windowMillis = 1_000L))
    }
}
