package com.example.birdnote.ui.staff

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
    fun followSpeedIncreasesWithDistance() {
        val dt = 0.25f
        val near = advanceShift(
            shift = 0f,
            target = 1f,
            followTimeSeconds = 2f,
            minSpeedSlotsPerSecond = 0.05f,
            dtSeconds = dt,
        )
        val far = advanceShift(
            shift = 0f,
            target = 5f,
            followTimeSeconds = 2f,
            minSpeedSlotsPerSecond = 0.05f,
            dtSeconds = dt,
        )
        assertEquals(0.125f, near, 0.0001f)
        assertEquals(0.625f, far, 0.0001f)
    }

    @Test
    fun followSpeedUsesTheMinimumWhenClose() {
        val next = advanceShift(
            shift = 0f,
            target = 0.04f,
            followTimeSeconds = 2f,
            minSpeedSlotsPerSecond = 0.05f,
            dtSeconds = 0.25f,
        )
        assertEquals(0.0125f, next, 0.0001f)
    }

    @Test
    fun motionBlurMatchesFrameTravel() {
        assertEquals(
            0f,
            motionBlurSigmaPx(
                travelSlots = 0.01f,
                slotWidth = 80f,
                minTravelPx = 1f,
                sigmaPerTravelPx = 0.85f,
                maxSigmaPx = 40f,
            ),
            0f,
        )
        assertEquals(
            6.8f,
            motionBlurSigmaPx(
                travelSlots = -0.1f,
                slotWidth = 80f,
                minTravelPx = 1f,
                sigmaPerTravelPx = 0.85f,
                maxSigmaPx = 40f,
            ),
            0.001f,
        )
        assertEquals(
            40f,
            motionBlurSigmaPx(
                travelSlots = 2f,
                slotWidth = 80f,
                minTravelPx = 1f,
                sigmaPerTravelPx = 0.85f,
                maxSigmaPx = 40f,
            ),
            0.001f,
        )
    }

    private fun displayedSlot(absoluteIndex: Int, shift: Float, origin: Int): Float =
        (absoluteIndex - origin) - slideOffsetSlots(shift, origin)
}
