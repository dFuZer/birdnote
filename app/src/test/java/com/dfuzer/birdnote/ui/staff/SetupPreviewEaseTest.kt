package com.dfuzer.birdnote.ui.staff

import androidx.compose.ui.geometry.Size
import com.dfuzer.birdnote.domain.ChordConfig
import com.dfuzer.birdnote.domain.ChordQuality
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.IntervalConfig
import com.dfuzer.birdnote.domain.MAX_DIFFICULTY
import com.dfuzer.birdnote.domain.MIN_DIFFICULTY
import com.dfuzer.birdnote.domain.PracticeConfig
import com.dfuzer.birdnote.domain.clefs
import com.dfuzer.birdnote.domain.intervalPitchRange
import com.dfuzer.birdnote.domain.pitchRange
import com.dfuzer.birdnote.domain.previewChords
import com.dfuzer.birdnote.domain.previewIntervals
import com.dfuzer.birdnote.domain.previewNotes
import com.dfuzer.birdnote.ui.screens.chordPreviewColumnCount
import com.dfuzer.birdnote.ui.screens.chordTileMotions
import com.dfuzer.birdnote.ui.screens.easedTileFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupPreviewEaseTest {
    private val size = Size(900f, 500f)

    @Test
    fun noteDifficultyChangeSlidesSamplesOnAStableStaff() {
        val from = pose(noteModel(MIN_DIFFICULTY, ClefMode.SOL))
        val to = pose(noteModel(2, ClefMode.SOL))
        val mid = easeSetupPreview(from, to, 0.5f)

        assertEquals(from.staves.single().bottomLineY, to.staves.single().bottomLineY, 0.01f)
        assertEquals(from.staves.single().bottomLineY, mid.staves.single().bottomLineY, 0.01f)
        assertTrue(from.ranges.single().top != to.ranges.single().top)
        assertEquals(midpoint(from.ranges.single().top, to.ranges.single().top), mid.ranges.single().top, 0.01f)
        assertEquals(1f, mid.ranges.single().alpha, 0f)
        val fromStep = from.chords.first().heads.single().step
        val toStep = to.chords.first().heads.single().step
        assertTrue(fromStep != toStep)
        assertEquals(midpoint(fromStep, toStep), mid.chords.first().heads.single().step, 0.01f)
        assertEquals(1f, mid.chords.first().alpha, 0f)
    }

    @Test
    fun endpointsMatchThePosesBeingEased() {
        val from = pose(noteModel(MIN_DIFFICULTY, ClefMode.SOL))
        val to = pose(noteModel(MAX_DIFFICULTY, ClefMode.FA))
        val start = easeSetupPreview(from, to, 0f)
        val end = easeSetupPreview(from, to, 1f)

        assertEquals(from.ranges, start.ranges)
        assertEquals(from.chords, start.chords)
        assertEquals(to.ranges, end.ranges)
        assertEquals(to.chords, end.chords)
    }

    @Test
    fun clefSwapSlidesTheBandAndCrossfadesTheClef() {
        val from = pose(noteModel(2, ClefMode.SOL))
        val to = pose(noteModel(2, ClefMode.FA))
        val mid = easeSetupPreview(from, to, 0.5f)

        assertEquals(1, mid.ranges.size)
        assertEquals(1f, mid.ranges.single().alpha, 0f)
        assertEquals(midpoint(from.ranges.single().top, to.ranges.single().top), mid.ranges.single().top, 0.01f)
        assertEquals(1, mid.staves.size)
        assertEquals(listOf(Clef.SOL, Clef.FA), mid.staves.single().clefs.map { it.clef })
        assertEquals(0.5f, mid.staves.single().clefs[0].alpha, 0.01f)
        assertEquals(0.5f, mid.staves.single().clefs[1].alpha, 0.01f)
        assertEquals(from.chords.size, mid.chords.size)
        assertTrue(mid.chords.all { it.alpha == 1f })
    }

    @Test
    fun grandStaffFadesTheNewStaffInWhileTrebleMoves() {
        val from = pose(noteModel(MIN_DIFFICULTY, ClefMode.SOL))
        val to = pose(noteModel(MIN_DIFFICULTY, ClefMode.SOL_FA))
        val mid = easeSetupPreview(from, to, 0.5f)
        val treble = mid.staves.first { it.clefs.any { mark -> mark.clef == Clef.SOL && mark.alpha == 1f } }
        val bass = mid.staves.first { it.clefs.any { mark -> mark.clef == Clef.FA } }

        assertTrue(from.staves.single().bottomLineY != to.staves.first { it.clef == Clef.SOL }.bottomLineY)
        assertEquals(
            midpoint(from.staves.single().bottomLineY, to.staves.first { it.clef == Clef.SOL }.bottomLineY),
            treble.bottomLineY,
            0.01f,
        )
        assertEquals(0.5f, bass.alpha, 0.01f)
        assertEquals(1f, mid.ranges.first { it.clef == Clef.SOL }.alpha, 0f)
        assertEquals(0.5f, mid.ranges.first { it.clef == Clef.FA }.alpha, 0.01f)
        assertEquals(0.5f, mid.chords.first { it.clef == Clef.FA }.alpha, 0.01f)
        assertTrue(mid.chords.filter { it.clef == Clef.SOL }.all { it.alpha == 1f })
    }

    @Test
    fun intervalDifficultySlidesTheWiderPairInsideTheSameBand() {
        val from = pose(intervalModel(MIN_DIFFICULTY))
        val to = pose(intervalModel(MAX_DIFFICULTY))
        val mid = easeSetupPreview(from, to, 0.5f)

        assertEquals(from.ranges.single().top, to.ranges.single().top, 0.01f)
        assertEquals(from.ranges.single().height, mid.ranges.single().height, 0.01f)
        val fromUpper = from.chords.first { it.chordIndex == 1 }.heads.maxOf { it.step }
        val toUpper = to.chords.first { it.chordIndex == 1 }.heads.maxOf { it.step }
        val midUpper = mid.chords.first { it.chordIndex == 1 }.heads.maxOf { it.step }
        assertTrue(toUpper > fromUpper)
        assertEquals(midpoint(fromUpper, toUpper), midUpper, 0.01f)
    }

    @Test
    fun chordGridShiftsStayingTilesAndFadesNewQualities() {
        val easy = previewChords(ChordConfig(MIN_DIFFICULTY, ClefMode.SOL))
        val wider = previewChords(ChordConfig(2, ClefMode.SOL))
        val motions = chordTileMotions(easy, MIN_DIFFICULTY, wider, 2)
        val major = motions.first { it.quality == ChordQuality.MAJOR }
        val augmented = motions.first { it.quality == ChordQuality.AUGMENTED }

        assertEquals(2, major.fromSlot?.columns)
        assertEquals(1, major.fromSlot?.rows)
        assertEquals(2, major.toSlot?.rows)
        assertNull(augmented.fromSlot)
        assertEquals(0, augmented.toSlot?.column)
        assertEquals(1, augmented.toSlot?.row)

        val start = easedTileFrame(major, 0f, width = 200f, height = 100f, gap = 0f)
        val end = easedTileFrame(major, 1f, width = 200f, height = 100f, gap = 0f)
        val mid = easedTileFrame(major, 0.5f, width = 200f, height = 100f, gap = 0f)
        assertEquals(100f, start.height, 0.01f)
        assertEquals(50f, end.height, 0.01f)
        assertEquals(75f, mid.height, 0.01f)
        assertEquals(1f, mid.alpha, 0f)

        val arriving = easedTileFrame(augmented, 0.5f, width = 200f, height = 100f, gap = 0f)
        assertEquals(0.5f, arriving.alpha, 0.01f)
        assertTrue(arriving.scale in 0.92f..1f)
    }

    @Test
    fun chordGridFadesQualitiesThatLeave() {
        val easy = previewChords(ChordConfig(MIN_DIFFICULTY, ClefMode.SOL))
        val wider = previewChords(ChordConfig(2, ClefMode.SOL))
        val motions = chordTileMotions(wider, 2, easy, MIN_DIFFICULTY)
        val augmented = motions.first { it.quality == ChordQuality.AUGMENTED }

        assertNull(augmented.toSlot)
        assertEquals(1f, easedTileFrame(augmented, 0f, 200f, 100f, 0f).alpha, 0.01f)
        assertEquals(0f, easedTileFrame(augmented, 1f, 200f, 100f, 0f).alpha, 0.01f)
        assertEquals(4, chordPreviewColumnCount(7))
        assertEquals(5, chordPreviewColumnCount(9))
    }

    private fun noteModel(difficulty: Int, clefMode: ClefMode): StaffRenderModel {
        val config = PracticeConfig(difficulty, clefMode)
        return StaffRenderModel(
            clefMode = clefMode,
            chords = previewNotes(config).asChords(),
            difficulty = difficulty,
            ranges = clefMode.clefs().associateWith { pitchRange(difficulty, it) },
        )
    }

    private fun intervalModel(difficulty: Int): StaffRenderModel {
        val config = IntervalConfig(difficulty)
        return StaffRenderModel(
            clefMode = ClefMode.SOL,
            chords = previewIntervals(config).map { it.asChord() },
            difficulty = difficulty,
            ranges = mapOf(Clef.SOL to intervalPitchRange()),
        )
    }

    private fun pose(model: StaffRenderModel): SetupPreviewPose = setupPreviewPose(
        size = size,
        request = StaffDrawRequest(
            model = model,
            visibleSlotCount = null,
            noteAreaExtraLeftPaddingInLineSpaces = 1f,
            compactVertical = false,
            staffScaleOverride = null,
            verticalCenter = 0f,
        ),
    )

    private fun midpoint(start: Float, end: Float): Float = (start + end) / 2f
}
