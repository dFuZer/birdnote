package com.example.birdnote.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KeySignatureTest {
    @Test
    fun sharpAndFlatOrdersFollowTheCircleOfFifths() {
        assertEquals(
            listOf(
                NoteName.FA,
                NoteName.DO,
                NoteName.SOL,
                NoteName.RE,
                NoteName.LA,
                NoteName.MI,
                NoteName.SI,
            ),
            SHARP_ORDER,
        )
        assertEquals(SHARP_ORDER.asReversed(), FLAT_ORDER)
    }

    @Test
    fun signatureNotesTakeTheStandardPrefix() {
        assertEquals(emptyList<NoteName>(), MajorKey.C.signatureNotes())
        assertEquals(SHARP_ORDER.take(1), MajorKey.G.signatureNotes())
        assertEquals(SHARP_ORDER.take(5), MajorKey.B.signatureNotes())
        assertEquals(SHARP_ORDER, MajorKey.C_SHARP.signatureNotes())
        assertEquals(FLAT_ORDER.take(1), MajorKey.F.signatureNotes())
        assertEquals(FLAT_ORDER.take(3), MajorKey.E_FLAT.signatureNotes())
        assertEquals(FLAT_ORDER, MajorKey.C_FLAT.signatureNotes())
    }

    @Test
    fun difficultyBandsGrowAlongTheCircleAndAlwaysIncludeC() {
        assertEquals(listOf(MajorKey.C, MajorKey.G, MajorKey.F), majorKeysFor(1))
        assertEquals(7, majorKeysFor(2).size)
        assertTrue(MajorKey.E_FLAT in majorKeysFor(2))
        assertFalse(MajorKey.E in majorKeysFor(2))
        assertEquals(11, majorKeysFor(3).size)
        assertEquals(MajorKey.entries.toList(), majorKeysFor(4))
        (MIN_DIFFICULTY..MAX_DIFFICULTY).forEach { difficulty ->
            assertTrue(MajorKey.C in majorKeysFor(difficulty))
        }
        assertTrue(
            majorKeysFor(1).maxOf { it.accidentalCount } <
                majorKeysFor(4).maxOf { it.accidentalCount },
        )
    }

    @Test
    fun oneFlatIsFMajorNotItsRelativeMinor() {
        val signature = KeySignature(MajorKey.F, Clef.SOL)
        assertEquals(1, signature.key.accidentalCount)
        assertTrue(isCorrect(signature, NoteName.FA))
        assertFalse(isCorrect(signature, NoteName.RE))
    }

    @Test
    fun tonicAnswerIgnoresTheAccidentalOnTheKeyName() {
        assertTrue(isCorrect(KeySignature(MajorKey.C_SHARP, Clef.SOL), NoteName.DO))
        assertTrue(isCorrect(KeySignature(MajorKey.C_FLAT, Clef.FA), NoteName.DO))
        assertTrue(isCorrect(KeySignature(MajorKey.B_FLAT, Clef.FA), NoteName.SI))
        assertFalse(isCorrect(KeySignature(MajorKey.G, Clef.SOL), NoteName.DO))
    }

    @Test
    fun generatedSignaturesStayInsideTheTonicFilterAndClef() {
        val focused = KeySignatureConfig(4, ClefMode.FA, MajorKey.B_FLAT)
        repeat(30) { seed ->
            val signature = generateKeySignature(focused, null, Random(seed))
            assertEquals(MajorKey.B_FLAT, signature.key)
            assertEquals(Clef.FA, signature.clef)
        }
        val open = KeySignatureConfig(1, ClefMode.SOL, null)
        val allowed = majorKeysFor(1).toSet()
        repeat(40) { seed ->
            val signature = generateKeySignature(open, null, Random(seed))
            assertTrue(signature.key in allowed)
            assertEquals(Clef.SOL, signature.clef)
        }
    }

    @Test
    fun mixedClefQueueAsksTrebleThenBass() {
        val config = KeySignatureConfig(2, ClefMode.SOL_FA, null)
        val queue = generateKeySignatureQueue(config, QUEUE_SIZE, Random(3))
        assertEquals(QUEUE_SIZE, queue.size)
        queue.forEachIndexed { index, signature ->
            val expected = if (index % 2 == 0) Clef.SOL else Clef.FA
            assertEquals(expected, signature.clef)
            assertTrue(signature.key in majorKeysFor(2))
        }
        val advanced = advanceKeySignatureQueue(queue, config, Random(4))
        assertEquals(Clef.FA, advanced.first().clef)
        assertEquals(Clef.SOL, advanced.last().clef)
    }

    @Test
    fun aFocusedKeyStillSlidesToTheNextSignature() {
        val config = KeySignatureConfig(1, ClefMode.SOL, MajorKey.C)
        val queue = generateKeySignatureQueue(config, QUEUE_SIZE, Random(8))
        val advanced = advanceKeySignatureQueue(queue, config, Random(8))
        assertTrue(queue.all { it.key == MajorKey.C && it.clef == Clef.SOL })
        assertNotEquals(queue, advanced)
        assertTrue(isQueueAdvance(queue, advanced))
    }

    @Test
    fun everyMajorKeySpellsAMajorTriadOnBothClefs() {
        MajorKey.entries.forEach { key ->
            Clef.entries.forEach { clef ->
                val notes = majorTriad(key, clef)
                assertEquals(3, notes.size)
                assertEquals(key.tonic, notes.first().pitch.noteName)
                assertEquals(key.tonicAccidental, notes.first().accidental)
                val root = notes[0].chromaticSemitone()
                val third = notes[1].chromaticSemitone()
                val fifth = notes[2].chromaticSemitone()
                assertEquals(4, third - root)
                assertEquals(7, fifth - root)
                assertTrue(notes.all { it.pitch.diatonicStep in 14..42 })
            }
        }
        val fSharp = majorTriad(MajorKey.F_SHARP, Clef.SOL).map { it.accidental }
        assertEquals(listOf(Accidental.SHARP, Accidental.SHARP, Accidental.SHARP), fSharp)
        val bFlat = majorTriad(MajorKey.B_FLAT, Clef.FA).map { it.accidental }
        assertEquals(listOf(Accidental.FLAT, Accidental.NONE, Accidental.NONE), bFlat)
    }
}
