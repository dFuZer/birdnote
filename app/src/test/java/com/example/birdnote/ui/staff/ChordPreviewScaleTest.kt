package com.example.birdnote.ui.staff

import androidx.compose.ui.geometry.Size
import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.MAX_DIFFICULTY
import com.example.birdnote.domain.MIN_DIFFICULTY
import com.example.birdnote.ui.LayoutTuning
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
            centerVertically = true,
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
}
