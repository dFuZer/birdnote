package com.dfuzer.birdnote.ui.staff

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaffSlideTest {
    @Test
    fun dropCountRemovesOnlyTheScrolledOffPrefix() {
        assertEquals(0, dropCount(shift = 0.9f, origin = 0, beltSize = 11, visibleCount = 10))
        assertEquals(1, dropCount(shift = 1f, origin = 0, beltSize = 11, visibleCount = 10))
        assertEquals(2, dropCount(shift = 2.4f, origin = 0, beltSize = 12, visibleCount = 10))
        assertEquals(1, dropCount(shift = 2.4f, origin = 1, beltSize = 11, visibleCount = 10))
        assertEquals(1, dropCount(shift = 5f, origin = 0, beltSize = 11, visibleCount = 10))
        assertEquals(0, dropCount(shift = -3f, origin = 0, beltSize = 10, visibleCount = 10))
    }

    @Test
    fun pruneLeavesTheScreenSlotUnchanged() {
        val shift = 2.4f
        val origin = 0
        val dropped = dropCount(shift, origin, beltSize = 12, visibleCount = 10)
        val newOrigin = origin + dropped
        val keptIndex = 5
        assertEquals(
            keptIndex - shift,
            displayedSlot(keptIndex, shift, newOrigin),
            0f,
        )
        assertTrue(keptIndex >= newOrigin)
        val lastDropped = newOrigin - 1
        assertTrue(lastDropped <= shift - 1f)
    }

    @Test
    fun spawnShiftStartsShortOfTheRestingPosition() {
        assertEquals(-9f, spawnShift(10), 0f)
        assertEquals(0f, spawnShift(1), 0f)
    }

    @Test
    fun shiftAtMatchesFineStepsOfTheFollowCurve() {
        val start = -3.5f
        val target = 2f
        val followTimeSeconds = 1.5f
        val minSpeed = 0.05f
        val dt = 0.0002f
        var integrated = start
        var elapsed = 0f
        while (elapsed < 12f) {
            assertEquals(
                integrated,
                shiftAt(elapsed, start, target, followTimeSeconds, minSpeed),
                0.01f,
            )
            integrated = advanceShift(integrated, target, followTimeSeconds, minSpeed, dt)
            elapsed += dt
        }
        assertEquals(target, integrated, 0f)
        assertEquals(target, shiftAt(elapsed, start, target, followTimeSeconds, minSpeed), 0f)
    }

    @Test
    fun shiftAtMovesOnTheFirstSample() {
        val moved = shiftAt(
            elapsedSeconds = 1f / 60f,
            startShift = spawnShift(8),
            targetShift = 0f,
            followTimeSeconds = 1.5f,
            minSpeedSlotsPerSecond = 0.05f,
        )
        assertTrue(moved > spawnShift(8))
    }

    private fun displayedSlot(absoluteIndex: Int, shift: Float, origin: Int): Float =
        (absoluteIndex - origin) - slideOffsetSlots(shift, origin)
}

/**
 * Moves [shift] toward [target]. Speed is the remaining distance divided by
 * [followTimeSeconds], and never slower than [minSpeedSlotsPerSecond].
 */
private fun advanceShift(
    shift: Float,
    target: Float,
    followTimeSeconds: Float,
    minSpeedSlotsPerSecond: Float,
    dtSeconds: Float,
): Float {
    val remaining = target - shift
    val distance = abs(remaining)
    if (distance == 0f) return shift
    val tau = followTimeSeconds.coerceAtLeast(0.001f)
    val speed = max(minSpeedSlotsPerSecond, distance / tau)
    val step = speed * dtSeconds
    if (step >= distance) return target
    return shift + sign(remaining) * step
}
