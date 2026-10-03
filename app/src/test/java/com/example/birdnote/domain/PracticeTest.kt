package com.example.birdnote.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeTest {
    @Test
    fun difficultyRangesExpandOnBothClefs() {
        (MIN_DIFFICULTY until MAX_DIFFICULTY).forEach { level ->
            Clef.entries.forEach { clef ->
                val inner = pitchRange(level, clef)
                val outer = pitchRange(level + 1, clef)
                assertTrue(outer.first <= inner.first)
                assertTrue(outer.last >= inner.last)
                assertTrue(outer.count() > inner.count())
            }
        }
    }

    @Test
    fun rangesCenterThenExpandAwayFromMixedStaffCollision() {
        (MIN_DIFFICULTY..2).forEach { level ->
            assertEquals(32, pitchRange(level, Clef.SOL).average().toInt())
            assertEquals(24, pitchRange(level, Clef.FA).average().toInt())
        }
        assertEquals(28..40, pitchRange(3, Clef.SOL))
        assertEquals(16..28, pitchRange(3, Clef.FA))
        assertEquals(26..42, pitchRange(MAX_DIFFICULTY, Clef.SOL))
        assertEquals(14..30, pitchRange(MAX_DIFFICULTY, Clef.FA))
    }

    @Test
    fun altoAndTenorRangesCenterOnMiddleCThenReachTwoLedgers() {
        (MIN_DIFFICULTY..2).forEach { level ->
            assertEquals(28, pitchRange(level, Clef.ALTO).average().toInt())
            assertEquals(28, pitchRange(level, Clef.TENOR).average().toInt())
        }
        assertEquals(22..34, pitchRange(3, Clef.ALTO))
        assertEquals(20..32, pitchRange(3, Clef.TENOR))
        assertEquals(20..36, pitchRange(MAX_DIFFICULTY, Clef.ALTO))
        assertEquals(18..34, pitchRange(MAX_DIFFICULTY, Clef.TENOR))
        assertEquals(pitchRange(MAX_DIFFICULTY, Clef.ALTO), chordPitchRange(Clef.ALTO))
        assertEquals(pitchRange(MAX_DIFFICULTY, Clef.TENOR), chordPitchRange(Clef.TENOR))
    }

    @Test
    fun previewNotesStayInsideSelectedRanges() {
        ClefMode.entries.forEach { mode ->
            (MIN_DIFFICULTY..MAX_DIFFICULTY).forEach { difficulty ->
                val config = PracticeConfig(difficulty, mode)
                previewNotes(config).forEach { note ->
                    val range = pitchRange(difficulty, note.clef)
                    assertTrue(note.pitch.diatonicStep in range)
                    assertTrue(note.clef in mode.clefs())
                }
            }
        }
    }

    @Test
    fun mixedClefPreviewAlternatesTrebleAndBass() {
        val notes = previewNotes(PracticeConfig(2, ClefMode.SOL_FA))
        assertTrue(notes.size >= 2)
        notes.forEachIndexed { index, note ->
            val expected = if (index % 2 == 0) Clef.SOL else Clef.FA
            assertEquals(expected, note.clef)
        }
    }

    @Test
    fun mixedClefModeAlternatesClefs() {
        val config = PracticeConfig(2, ClefMode.SOL_FA)
        val random = Random(1)
        var previous: StaffNote? = null
        repeat(12) { index ->
            val note = generateNote(config, previous, random)
            val expected = if (index % 2 == 0) Clef.SOL else Clef.FA
            assertEquals(expected, note.clef)
            previous = note
        }
    }

    @Test
    fun generatedNotesStayInRangeAndAvoidImmediatePitchRepeats() {
        val config = PracticeConfig(3, ClefMode.SOL)
        val random = Random(42)
        var previous: StaffNote? = null
        val range = pitchRange(3, Clef.SOL)
        repeat(200) {
            val note = generateNote(config, previous, random)
            assertTrue(note.pitch.diatonicStep in range)
            if (previous != null && range.count() > 1) {
                assertTrue(note.pitch != previous!!.pitch)
            }
            previous = note
        }
    }

    @Test
    fun isQueueAdvanceDetectsSingleStep() {
        val config = PracticeConfig(2, ClefMode.SOL)
        val random = Random(11)
        val queue = generateQueue(config, QUEUE_SIZE, random)
        val advanced = advanceQueue(queue, config, random)
        assertTrue(isQueueAdvance(queue, advanced))
        assertFalse(isQueueAdvance(queue, generateQueue(config, QUEUE_SIZE, Random(12))))
        assertFalse(isQueueAdvance(emptyList(), queue))
    }

    @Test
    fun queueDropsLeftmostNoteAndKeepsFixedSize() {
        val config = PracticeConfig(2, ClefMode.FA)
        val random = Random(7)
        val queue = generateQueue(config, QUEUE_SIZE, random)
        assertEquals(QUEUE_SIZE, queue.size)
        val advanced = advanceQueue(queue, config, random)
        assertEquals(QUEUE_SIZE, advanced.size)
        assertEquals(queue.drop(1), advanced.dropLast(1))
    }

    @Test
    fun everyClefModeShowsTenNotes() {
        ClefMode.entries.forEach { mode ->
            val config = PracticeConfig(1, mode)
            val queue = generateQueue(config, mode.queueSize(), Random(3))
            assertEquals(QUEUE_SIZE, queue.size)
            assertEquals(QUEUE_SIZE, mode.queueSize())
        }
        assertEquals(QUEUE_SIZE, DOUBLE_CLEF_QUEUE_SIZE)
    }

    @Test
    fun intervalDifficultyUnlocksFromSecondeToMax() {
        assertEquals(
            listOf(IntervalName.SECONDE, IntervalName.TIERCE, IntervalName.QUARTE),
            intervalNamesFor(1),
        )
        assertEquals(IntervalName.SIXTE, intervalNamesFor(2).last())
        assertEquals(IntervalName.OCTAVE, intervalNamesFor(3).last())
        assertEquals(IntervalName.NEUVIEME, intervalNamesFor(4).last())
        assertEquals(IntervalName.SECONDE, intervalNamesFor(4).first())
    }

    @Test
    fun intervalPreviewShowsSecondeThenMax() {
        val easy = previewIntervals(IntervalConfig(1))
        assertEquals(2, easy.size)
        assertEquals(IntervalName.SECONDE, easy[0].name)
        assertEquals(IntervalName.QUARTE, easy[1].name)
        val hard = previewIntervals(IntervalConfig(MAX_DIFFICULTY))
        assertEquals(IntervalName.SECONDE, hard[0].name)
        assertEquals(IntervalName.NEUVIEME, hard[1].name)
    }

    @Test
    fun generatedIntervalsStayWithinTwoLedgersAndDifficulty() {
        val config = IntervalConfig(MAX_DIFFICULTY)
        val random = Random(21)
        var previous: StaffInterval? = null
        val range = intervalPitchRange()
        assertEquals(26..42, range)
        repeat(200) {
            val interval = generateInterval(config, previous, random)
            assertTrue(interval.lower.diatonicStep in range)
            assertTrue(interval.upper.diatonicStep in range)
            assertEquals(Clef.SOL, interval.clef)
            assertTrue(interval.name in intervalNamesFor(MAX_DIFFICULTY))
            if (previous != null) {
                assertTrue(interval.name != previous!!.name)
            }
            previous = interval
        }
    }

    @Test
    fun intervalQueueAdvancesLikeNoteQueue() {
        val config = IntervalConfig(2)
        val random = Random(9)
        val queue = generateIntervalQueue(config, QUEUE_SIZE, random)
        assertEquals(QUEUE_SIZE, queue.size)
        val advanced = advanceIntervalQueue(queue, config, random)
        assertTrue(isQueueAdvance(queue, advanced))
        assertEquals(queue.drop(1), advanced.dropLast(1))
    }

    @Test
    fun chordDifficultyUnlocksPedagogically() {
        assertEquals(listOf(ChordQuality.MAJOR, ChordQuality.MINOR), chordQualitiesFor(1))
        assertEquals(4, chordQualitiesFor(2).size)
        assertTrue(ChordQuality.AUGMENTED in chordQualitiesFor(2))
        assertTrue(ChordQuality.DIMINISHED in chordQualitiesFor(2))
        assertEquals(7, chordQualitiesFor(3).size)
        assertTrue(ChordQuality.DOMINANT_7 in chordQualitiesFor(3))
        assertEquals(ChordQuality.entries.toList(), chordQualitiesFor(4).toList())
    }

    @Test
    fun chordPreviewShowsEachUnlockedQualityOnASharedRoot() {
        val easy = previewChords(ChordConfig(1, ClefMode.SOL))
        assertEquals(listOf(ChordQuality.MAJOR, ChordQuality.MINOR), easy.map { it.quality })
        assertTrue(easy.all { it.root.diatonicStep == previewRootStep(Clef.SOL) })
        val hard = previewChords(ChordConfig(MAX_DIFFICULTY, ClefMode.SOL))
        assertEquals(ChordQuality.entries.size, hard.size)
        hard.forEach { chord ->
            chord.notes.forEach { note ->
                assertTrue(note.pitch.staffStep(note.clef) in 0..8)
            }
        }
        val mixed = previewChords(ChordConfig(2, ClefMode.SOL_FA))
        assertEquals(Clef.SOL, mixed[0].clef)
        assertEquals(Clef.FA, mixed[1].clef)
    }

    @Test
    fun chordPreviewSpellsTheLabeledQualityOnEveryClef() {
        ClefMode.entries.forEach { mode ->
            (MIN_DIFFICULTY..MAX_DIFFICULTY).forEach { difficulty ->
                val preview = previewChords(ChordConfig(difficulty, mode))
                assertEquals(chordQualitiesFor(difficulty), preview.map { it.quality })
                preview.forEach { chord ->
                    assertEquals(chord.quality.semitoneIntervals, intervalPattern(chord))
                    chord.notes.forEach { note ->
                        assertTrue(note.pitch.staffStep(note.clef) in 0..8)
                    }
                }
                if (mode != ClefMode.SOL_FA) {
                    val root = previewRootStep(mode.clefs().single())
                    assertTrue(preview.all { it.root.diatonicStep == root })
                }
            }
        }
    }

    @Test
    fun generatedChordsStayInRangeAvoidQualityRepeatsAndFitTheStaff() {
        ClefMode.entries.filter { it.clefs().size == 1 }.forEach { mode ->
            val config = ChordConfig(MAX_DIFFICULTY, mode)
            val random = Random(21)
            var previous: PracticeChord? = null
            val clef = mode.clefs().single()
            val range = chordPitchRange(clef)
            repeat(200) {
                val chord = generateChord(config, previous, random)
                assertTrue(chord.root.diatonicStep in range)
                assertTrue(chord.notes.last().pitch.diatonicStep in range)
                assertEquals(clef, chord.clef)
                assertTrue(chord.quality in chordQualitiesFor(MAX_DIFFICULTY))
                assertEquals(chord.quality.semitoneIntervals, intervalPattern(chord))
                if (previous != null) {
                    assertTrue(chord.quality != previous!!.quality)
                }
                previous = chord
            }
        }
    }

    @Test
    fun mixedClefChordsAlternateStaves() {
        val config = ChordConfig(2, ClefMode.SOL_FA)
        val random = Random(3)
        var previous: PracticeChord? = null
        repeat(8) { index ->
            val chord = generateChord(config, previous, random)
            val expected = if (index % 2 == 0) Clef.SOL else Clef.FA
            assertEquals(expected, chord.clef)
            assertEquals(chord.quality.semitoneIntervals, intervalPattern(chord))
            previous = chord
        }
    }

    @Test
    fun chordQueueAdvancesLikeNoteQueue() {
        val config = ChordConfig(2, ClefMode.FA)
        val random = Random(9)
        val queue = generateChordQueue(config, QUEUE_SIZE, random)
        assertEquals(QUEUE_SIZE, queue.size)
        val advanced = advanceChordQueue(queue, config, random)
        assertTrue(isQueueAdvance(queue, advanced))
        assertEquals(queue.drop(1), advanced.dropLast(1))
    }
}

private fun intervalPattern(chord: PracticeChord): List<Int> {
    val root = chord.notes.first().chromaticSemitone()
    return chord.notes.map { it.chromaticSemitone() - root }
}
