package com.example.birdnote.domain

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicTest {
    @Test
    fun trebleStaffStepsMatchStandardPositions() {
        assertEquals(0, Pitch(30).staffStep(Clef.SOL)) // E4 bottom line
        assertEquals(2, Pitch(32).staffStep(Clef.SOL)) // G4
        assertEquals(8, Pitch(38).staffStep(Clef.SOL)) // F5 top line
    }

    @Test
    fun bassStaffStepsMatchStandardPositions() {
        assertEquals(0, Pitch(18).staffStep(Clef.FA)) // G2 bottom line
        assertEquals(6, Pitch(24).staffStep(Clef.FA)) // F3
        assertEquals(8, Pitch(26).staffStep(Clef.FA)) // A3 top line
    }

    @Test
    fun pianoAssetPathUsesScientificPitchNames() {
        assertEquals("notes/2C.wav", Pitch(14).pianoAssetPath())
        assertEquals("notes/3A.wav", Pitch(26).pianoAssetPath())
        assertEquals("notes/4C.wav", Pitch(28).pianoAssetPath())
        assertEquals("notes/4G.wav", Pitch(32).pianoAssetPath())
        assertEquals("notes/6C.wav", Pitch(42).pianoAssetPath())
    }

    @Test
    fun pitchUsesFixedDoNoteNames() {
        assertEquals(NoteName.DO, Pitch(28).noteName) // C4
        assertEquals(NoteName.RE, Pitch(29).noteName)
        assertEquals(NoteName.MI, Pitch(30).noteName)
        assertEquals(NoteName.FA, Pitch(31).noteName)
        assertEquals(NoteName.SOL, Pitch(32).noteName)
        assertEquals(NoteName.LA, Pitch(33).noteName)
        assertEquals(NoteName.SI, Pitch(34).noteName)
        assertEquals(NoteName.DO, Pitch(35).noteName) // C5
    }

    @Test
    fun isCorrectComparesSolfegeOnly() {
        val note = StaffNote(Pitch(28), Clef.SOL)
        assertTrue(isCorrect(note, NoteName.DO))
        assertFalse(isCorrect(note, NoteName.RE))
    }

    @Test
    fun intervalNameFollowsDiatonicDistance() {
        assertEquals(
            IntervalName.SECONDE,
            StaffInterval(Pitch(32), Pitch(33)).name,
        )
        assertEquals(
            IntervalName.QUARTE,
            StaffInterval(Pitch(30), Pitch(33)).name,
        )
        assertEquals(
            IntervalName.NEUVIEME,
            StaffInterval(Pitch(30), Pitch(38)).name,
        )
        assertEquals(1, IntervalName.SECONDE.diatonicDistance)
        assertEquals(8, IntervalName.NEUVIEME.diatonicDistance)
    }

    @Test
    fun intervalLabelsAreFrench() {
        assertEquals("Seconde", IntervalName.SECONDE.label())
        assertEquals("Septième", IntervalName.SEPTIEME.label())
        assertEquals("Neuvième", IntervalName.NEUVIEME.label())
    }

    @Test
    fun isCorrectIntervalComparesNameOnly() {
        val interval = StaffInterval(Pitch(32), Pitch(34), Clef.SOL)
        assertTrue(isCorrect(interval, IntervalName.TIERCE))
        assertFalse(isCorrect(interval, IntervalName.SECONDE))
    }

    @Test
    fun noteLabelsFollowNamingConvention() {
        assertEquals("Do Ré Mi Fa Sol La Si", NoteNaming.SOLFEGE.scalePreview())
        assertEquals("C D E F G A B", NoteNaming.ENGLISH.scalePreview())
        assertEquals("H", NoteName.SI.label(NoteNaming.GERMAN))
        assertEquals("Ti", NoteName.SI.label(NoteNaming.SOLFEGE_TI))
        assertEquals("Re", NoteName.RE.label(NoteNaming.SOLFEGE_TI))
        assertEquals("Ré", NoteName.RE.label(NoteNaming.SOLFEGE))
    }

    @Test
    fun storedNoteNamingFallsBackToSolfege() {
        assertEquals(NoteNaming.ENGLISH, storedNoteNaming("ENGLISH"))
        assertEquals(NoteNaming.SOLFEGE, storedNoteNaming(null))
        assertEquals(NoteNaming.SOLFEGE, storedNoteNaming("unknown"))
    }

    @Test
    fun chromaticSemitoneUsesNaturalPitchClassPlusAccidental() {
        assertEquals(48, chromaticSemitone(Pitch(28))) // C4
        assertEquals(49, chromaticSemitone(Pitch(28), Accidental.SHARP))
        assertEquals(47, chromaticSemitone(Pitch(28), Accidental.FLAT))
        assertEquals(50, chromaticSemitone(Pitch(29))) // D4
        assertEquals(52, chromaticSemitone(Pitch(30))) // E4
    }

    @Test
    fun spellChordWritesAccidentalsWithoutDoubleSigns() {
        val cMajor = spellChord(Pitch(28), ChordQuality.MAJOR, Clef.SOL)!!
        assertEquals(listOf(Accidental.NONE, Accidental.NONE, Accidental.NONE), cMajor.notes.map { it.accidental })
        val cMinor = spellChord(Pitch(28), ChordQuality.MINOR, Clef.SOL)!!
        assertEquals(Accidental.FLAT, cMinor.notes[1].accidental)
        assertEquals(Accidental.NONE, cMinor.notes[2].accidental)
        val cDim7 = spellChord(Pitch(28), ChordQuality.DIMINISHED_7, Clef.SOL)
        assertEquals(null, cDim7)
        val dDim7 = spellChord(Pitch(29), ChordQuality.DIMINISHED_7, Clef.SOL)!!
        assertEquals(ChordQuality.DIMINISHED_7, dDim7.quality)
        assertTrue(dDim7.notes.all { it.accidental != Accidental.NATURAL })
        assertEquals(listOf(0, 3, 6, 9), dDim7.notes.map { it.chromaticSemitone() - chromaticSemitone(dDim7.root) })
    }

    @Test
    fun everyQualityHasASingleAccidentalSpellingOnD() {
        ChordQuality.entries.forEach { quality ->
            val chord = spellChord(Pitch(36), quality, Clef.SOL)
            assertTrue("$quality should spell on D5", chord != null)
            chord!!.notes.forEach { note ->
                assertTrue(note.accidental in listOf(Accidental.NONE, Accidental.SHARP, Accidental.FLAT))
            }
        }
    }

    @Test
    fun chordLabelsAreFrench() {
        assertEquals("Majeur", ChordQuality.MAJOR.label())
        assertEquals("7e demi-diminuée", ChordQuality.HALF_DIMINISHED_7.label())
        assertEquals("7e diminuée", ChordQuality.DIMINISHED_7.label())
    }

    @Test
    fun isCorrectChordComparesQualityOnly() {
        val chord = spellChord(Pitch(28), ChordQuality.MAJOR, Clef.SOL)!!
        assertTrue(isCorrect(chord, ChordQuality.MAJOR))
        assertFalse(isCorrect(chord, ChordQuality.MINOR))
    }
}
