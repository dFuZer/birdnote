package com.dfuzer.birdnote.ui.staff

import androidx.compose.ui.geometry.Size
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.MAX_DIFFICULTY
import com.dfuzer.birdnote.domain.MIN_DIFFICULTY
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.screens.easedChordTileStaff
import com.dfuzer.birdnote.ui.screens.settledStaffHeightPx
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChordPreviewScaleTest {
    @Test
    fun difficulty1MatchesNotesSingleStaffLineSpacing() {
        val notesHeight = 900f
        val tileHeight = 620f
        val scale = chordPreviewStaffScale(MIN_DIFFICULTY, tileHeight, notesHeight)
        val chordSpacing = lineSpacingForHeight(tileHeight, compactVertical = true) * scale
        assertEquals(notesSingleStaffLineSpacing(notesHeight), chordSpacing, 0.001f)
    }

    @Test
    fun difficulty2KeepsTheCurrentScale() {
        assertEquals(1f, chordPreviewStaffScale(2, tileHeightPx = 400f, notesSingleStaffHeightPx = 800f), 0.001f)
    }

    @Test
    fun difficulty1CentersTheStaff() {
        val size = Size(400f, 800f)
        val model = StaffRenderModel(clefMode = ClefMode.SOL, chords = emptyList())
        val centered = staffGeometry(
            size = size,
            model = model,
            visibleSlotCount = 1,
            noteAreaExtraLeftPaddingInLineSpaces = 0f,
            compactVertical = true,
            staffScaleOverride = 0.6f,
            verticalCenter = 1f,
        )
        val bottomAnchored = staffGeometry(
            size = size,
            model = model,
            visibleSlotCount = 1,
            noteAreaExtraLeftPaddingInLineSpaces = 0f,
            compactVertical = true,
            staffScaleOverride = 0.6f,
        )
        val paddingY = size.height * LayoutTuning.Staff.compactVerticalPadding
        val blockCenterY = paddingY + (size.height - paddingY * 2f) / 2f
        assertEquals(blockCenterY, centered.bottomLineYs[0] - 2f * centered.lineSpacing, 0.01f)
        assertTrue(centered.bottomLineYs[0] < bottomAnchored.bottomLineYs[0])
    }

    @Test
    fun difficulty3And4AreThirtyPercentSmaller() {
        val reduced = LayoutTuning.Setup.chordPreviewReducedElementScale
        assertEquals(0.70f, reduced, 0.001f)
        assertEquals(reduced, chordPreviewStaffScale(3, 400f, 800f), 0.001f)
        assertEquals(reduced, chordPreviewStaffScale(MAX_DIFFICULTY, 400f, 800f), 0.001f)
    }

    @Test
    fun difficulty1ScaleOnASmallInFlightTileIsMuchLargerThanTheSettledScale() {
        val notesHeight = 900f
        val fromStaff = 200f
        val toStaff = 500f
        val inFlight = chordPreviewStaffScale(MIN_DIFFICULTY, fromStaff, notesHeight)
        val settled = chordPreviewStaffScale(MIN_DIFFICULTY, toStaff, notesHeight)
        assertTrue(inFlight > settled * 1.5f)
    }

    @Test
    fun difficulty2To1StartsAtTheSmallTileScaleAndEasesLineSpacing() {
        val notesHeight = 900f
        val fromStaff = 200f
        val toStaff = 500f
        val start = easedChordTileStaff(
            fromDifficulty = 2,
            toDifficulty = MIN_DIFFICULTY,
            fraction = 0f,
            fromStaffHeightPx = fromStaff,
            toStaffHeightPx = toStaff,
            staffHeightPx = fromStaff,
            notesSingleStaffHeightPx = notesHeight,
        )
        val end = easedChordTileStaff(
            fromDifficulty = 2,
            toDifficulty = MIN_DIFFICULTY,
            fraction = 1f,
            fromStaffHeightPx = fromStaff,
            toStaffHeightPx = toStaff,
            staffHeightPx = toStaff,
            notesSingleStaffHeightPx = notesHeight,
        )
        val midStaff = (fromStaff + toStaff) / 2f
        val mid = easedChordTileStaff(
            fromDifficulty = 2,
            toDifficulty = MIN_DIFFICULTY,
            fraction = 0.5f,
            fromStaffHeightPx = fromStaff,
            toStaffHeightPx = toStaff,
            staffHeightPx = midStaff,
            notesSingleStaffHeightPx = notesHeight,
        )
        assertEquals(1f, start.staffScale, 0.001f)
        assertEquals(0f, start.verticalCenter, 0f)
        assertEquals(chordPreviewStaffScale(MIN_DIFFICULTY, toStaff, notesHeight), end.staffScale, 0.001f)
        assertEquals(1f, end.verticalCenter, 0f)
        val startSpacing = lineSpacingForHeight(fromStaff, compactVertical = true) * start.staffScale
        val endSpacing = lineSpacingForHeight(toStaff, compactVertical = true) * end.staffScale
        val midSpacing = lineSpacingForHeight(midStaff, compactVertical = true) * mid.staffScale
        assertEquals((startSpacing + endSpacing) / 2f, midSpacing, 0.01f)
        assertEquals(0.5f, mid.verticalCenter, 0.01f)
        assertTrue(start.staffScale < chordPreviewStaffScale(MIN_DIFFICULTY, fromStaff, notesHeight))
    }

    @Test
    fun settledStaffHeightStripsTheLabelFromTheDestinationTile() {
        assertEquals(180f, settledStaffHeightPx(slotHeightPx = 200f, tileHeightPx = 100f, staffHeightPx = 80f), 0.01f)
    }

    @Test
    fun verticalCenterMidpointSitsBetweenAnchoredAndCentered() {
        val size = Size(400f, 800f)
        val model = StaffRenderModel(clefMode = ClefMode.SOL, chords = emptyList())
        fun geometry(center: Float) = staffGeometry(
            size = size,
            model = model,
            visibleSlotCount = 1,
            noteAreaExtraLeftPaddingInLineSpaces = 0f,
            compactVertical = true,
            staffScaleOverride = 0.6f,
            verticalCenter = center,
        )
        val start = geometry(0f)
        val end = geometry(1f)
        val mid = geometry(0.5f)
        assertEquals((start.bottomLineYs[0] + end.bottomLineYs[0]) / 2f, mid.bottomLineYs[0], 0.01f)
    }
}
