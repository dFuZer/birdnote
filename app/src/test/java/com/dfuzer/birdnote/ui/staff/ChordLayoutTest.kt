package com.dfuzer.birdnote.ui.staff

import com.dfuzer.birdnote.domain.Accidental
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.Pitch
import com.dfuzer.birdnote.domain.StaffNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ChordLayoutTest {
    private val lineSpacing = 10f
    private val bottom = 100f
    private val slotX = 50f

    @Test
    fun secondOffsetsLowerHeadLeftWhenStemUp() {
        val lower = StaffNote(Pitch(32), Clef.SOL) // G4, staff step 2
        val upper = StaffNote(Pitch(33), Clef.SOL) // A4, staff step 3
        val layout = layoutChord(listOf(lower, upper), slotX, lineSpacing, bottom)
        val offset = stemToHeadOffset(lineSpacing)
        assertEquals(2, layout.heads.size)
        assertEquals(slotX - offset, layout.heads[0].x, 0.01f)
        assertEquals(slotX + offset, layout.heads[1].x, 0.01f)
        assertNotNull(layout.stem)
        assertEquals(slotX, layout.stem!!.x, 0.01f)
    }

    @Test
    fun triadUsesOneStemAndNoHeadOffset() {
        val notes = listOf(28, 30, 32).map { StaffNote(Pitch(it), Clef.SOL) }
        val layout = layoutChord(notes, slotX, lineSpacing, bottom)
        val offset = stemToHeadOffset(lineSpacing)
        assertTrue(layout.heads.all { it.x == slotX })
        assertNotNull(layout.stem)
        assertEquals(slotX + offset, layout.stem!!.x, 0.01f)
        assertTrue(abs(layout.stem!!.endY - layout.stem!!.startY) > lineSpacing)
    }

    @Test
    fun stemAttachesLeftOfHeadsWhenStemDown() {
        val notes = listOf(36, 38, 40).map { StaffNote(Pitch(it), Clef.SOL) } // D5 triad
        val layout = layoutChord(notes, slotX, lineSpacing, bottom)
        val offset = stemToHeadOffset(lineSpacing)
        assertTrue(layout.heads.first().stemDown)
        assertEquals(slotX - offset, layout.stem!!.x, 0.01f)
        assertTrue(layout.heads.all { it.x == slotX })
    }

    @Test
    fun stemOffsetSitsOnOvalEdgeNotDestRect() {
        val headWidth = lineSpacing * ChordLayoutMetrics().noteWidthInLineSpaces
        val visualHalf = noteHeadVisualHalfWidth(headWidth)
        val offset = stemToHeadOffset(lineSpacing)
        assertTrue(visualHalf < headWidth / 2f)
        assertTrue(offset < visualHalf)
        assertTrue(offset > headWidth * 0.30f)
    }

    @Test
    fun threeStackedFlatsUseThreeColumns() {
        val notes = listOf(
            StaffNote(Pitch(28), Clef.SOL, Accidental.FLAT),
            StaffNote(Pitch(30), Clef.SOL, Accidental.FLAT),
            StaffNote(Pitch(32), Clef.SOL, Accidental.FLAT),
        )
        val layout = layoutChord(notes, slotX, lineSpacing, bottom)
        val xs = layout.accidentals.map { it.x }.distinct()
        assertEquals(3, layout.accidentals.size)
        assertEquals(3, xs.size)
        assertTrue(layout.accidentals.all { it.x < slotX })
    }

    @Test
    fun sixthApartAccidentalsShareAColumn() {
        val lower = StaffNote(Pitch(28), Clef.SOL, Accidental.SHARP) // C
        val upper = StaffNote(Pitch(33), Clef.SOL, Accidental.SHARP) // A, 5 steps
        assertEquals(5, upper.pitch.diatonicStep - lower.pitch.diatonicStep)
        val layout = layoutChord(listOf(lower, upper), slotX, lineSpacing, bottom)
        assertEquals(2, layout.accidentals.size)
        assertEquals(layout.accidentals[0].x, layout.accidentals[1].x)
    }

    @Test
    fun boundsIncludeAccidentalsToTheLeft() {
        val natural = layoutChord(
            listOf(StaffNote(Pitch(32), Clef.SOL)),
            slotX,
            lineSpacing,
            bottom,
        )
        val withFlat = layoutChord(
            listOf(StaffNote(Pitch(32), Clef.SOL, Accidental.FLAT)),
            slotX,
            lineSpacing,
            bottom,
        )
        assertTrue(withFlat.bounds.left < natural.bounds.left)
        assertEquals(1, withFlat.accidentals.size)
        assertNull(withFlat.stem)
        assertTrue(withFlat.useCompleteNote)
    }
}
