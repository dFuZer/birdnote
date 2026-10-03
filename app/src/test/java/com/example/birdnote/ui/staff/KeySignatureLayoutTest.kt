package com.example.birdnote.ui.staff

import com.example.birdnote.domain.Accidental
import com.example.birdnote.domain.Clef
import com.example.birdnote.domain.KeySignature
import com.example.birdnote.domain.MajorKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeySignatureLayoutTest {
    private val lineSpacing = 20f
    private val left = 40f
    private val bottom = 200f

    @Test
    fun trebleSharpSitsOnTheTopLine() {
        val layout = layoutKeySignature(
            KeySignature(MajorKey.G, Clef.SOL),
            left,
            lineSpacing,
            bottom,
        )
        assertEquals(1, layout.accidentals.size)
        assertEquals(Accidental.SHARP, layout.accidentals.single().accidental)
        assertEquals(8, layout.accidentals.single().step)
        assertTrue(layout.heads.isEmpty())
    }

    @Test
    fun trebleFlatSitsOnTheMiddleLine() {
        val layout = layoutKeySignature(
            KeySignature(MajorKey.F, Clef.SOL),
            left,
            lineSpacing,
            bottom,
        )
        assertEquals(Accidental.FLAT, layout.accidentals.single().accidental)
        assertEquals(4, layout.accidentals.single().step)
    }

    @Test
    fun bassSharpSitsOnTheFourthLine() {
        val layout = layoutKeySignature(
            KeySignature(MajorKey.G, Clef.FA),
            left,
            lineSpacing,
            bottom,
        )
        assertEquals(Accidental.SHARP, layout.accidentals.single().accidental)
        assertEquals(6, layout.accidentals.single().step)
    }

    @Test
    fun bassFlatSitsOnTheSecondLine() {
        val layout = layoutKeySignature(
            KeySignature(MajorKey.F, Clef.FA),
            left,
            lineSpacing,
            bottom,
        )
        assertEquals(Accidental.FLAT, layout.accidentals.single().accidental)
        assertEquals(2, layout.accidentals.single().step)
    }

    @Test
    fun sevenSharpsOnTrebleRunLeftToRightInSignatureOrder() {
        val layout = layoutKeySignature(
            KeySignature(MajorKey.C_SHARP, Clef.SOL),
            left,
            lineSpacing,
            bottom,
        )
        assertEquals(listOf(8, 5, 9, 6, 3, 7, 4), layout.accidentals.map { it.step })
        val xs = layout.accidentals.map { it.x }
        assertEquals(xs.sorted(), xs)
        assertTrue(xs.zipWithNext().all { (previous, next) -> next > previous })
    }

    @Test
    fun sevenFlatsOnBassEndInTheSpaceBelowTheStaff() {
        val layout = layoutKeySignature(
            KeySignature(MajorKey.C_FLAT, Clef.FA),
            left,
            lineSpacing,
            bottom,
        )
        assertEquals(listOf(2, 5, 1, 4, 0, 3, -1), layout.accidentals.map { it.step })
        assertTrue(layout.accidentals.zipWithNext().all { (previous, next) -> next.x > previous.x })
    }

    @Test
    fun cMajorKeepsAnEmptyColumnAfterTheClef() {
        val layout = layoutKeySignature(
            KeySignature(MajorKey.C, Clef.SOL),
            left,
            lineSpacing,
            bottom,
        )
        assertTrue(layout.accidentals.isEmpty())
        assertTrue(layout.bounds.right > layout.bounds.left)
    }
}
