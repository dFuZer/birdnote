package com.dfuzer.birdnote.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
    fun altoStaffStepsPutMiddleCOnTheMiddleLine() {
        assertEquals(0, Pitch(24).staffStep(Clef.ALTO)) // F3 bottom line
        assertEquals(4, Pitch(28).staffStep(Clef.ALTO)) // C4, third line
        assertEquals(8, Pitch(32).staffStep(Clef.ALTO)) // G4 top line
    }

    @Test
    fun tenorStaffStepsPutMiddleCOnTheFourthLine() {
        assertEquals(0, Pitch(22).staffStep(Clef.TENOR)) // D3 bottom line
        assertEquals(6, Pitch(28).staffStep(Clef.TENOR)) // C4, fourth line
        assertEquals(8, Pitch(30).staffStep(Clef.TENOR)) // E4 top line
    }

    @Test
    fun solFaStillUsesOnlyTrebleAndBass() {
        assertEquals(listOf(Clef.SOL, Clef.FA), ClefMode.SOL_FA.clefs())
        assertEquals(listOf(Clef.ALTO), ClefMode.ALTO.clefs())
        assertEquals(listOf(Clef.TENOR), ClefMode.TENOR.clefs())
    }

    @Test
    fun preferredClefsKeepSolFaOnlyWhenBothAreOn() {
        assertEquals(
            listOf(ClefMode.SOL, ClefMode.FA, ClefMode.SOL_FA),
            clefModesFor(setOf(Clef.SOL, Clef.FA)),
        )
        assertEquals(listOf(ClefMode.SOL), clefModesFor(setOf(Clef.SOL)))
        assertEquals(listOf(ClefMode.ALTO, ClefMode.TENOR), clefModesFor(setOf(Clef.ALTO, Clef.TENOR)))
        assertFalse(ClefMode.SOL_FA in clefModesFor(setOf(Clef.SOL, Clef.ALTO)))
        assertEquals(
            listOf(ClefMode.SOL, ClefMode.FA, ClefMode.SOL_FA, ClefMode.ALTO, ClefMode.TENOR),
            clefModesFor(Clef.entries.toSet()),
        )
    }

    @Test
    fun lastPreferredClefStaysOn() {
        val onlyTenor = setOf(Clef.TENOR)
        assertEquals(onlyTenor, withClefToggled(onlyTenor, Clef.TENOR))
        assertEquals(setOf(Clef.TENOR, Clef.ALTO), withClefToggled(onlyTenor, Clef.ALTO))
        assertEquals(setOf(Clef.ALTO), withClefToggled(setOf(Clef.TENOR, Clef.ALTO), Clef.TENOR))
    }

    @Test
    fun setupOpensOnAClefThatIsStillPreferred() {
        assertEquals(ClefMode.ALTO, openingClefMode(setOf(Clef.ALTO)))
        assertEquals(
            ClefMode.FA,
            openingClefMode(setOf(Clef.FA, Clef.ALTO), remembered = ClefMode.SOL),
        )
        assertEquals(
            ClefMode.ALTO,
            openingClefMode(setOf(Clef.SOL, Clef.ALTO), remembered = ClefMode.ALTO),
        )
        assertEquals(
            ClefMode.SOL,
            openingClefMode(setOf(Clef.SOL), remembered = ClefMode.SOL_FA),
        )
    }

    @Test
    fun preferredClefsRoundTripInClefOrder() {
        assertEquals("SOL,TENOR", preferredClefsStorage(setOf(Clef.TENOR, Clef.SOL)))
        assertEquals(setOf(Clef.SOL, Clef.TENOR), storedPreferredClefs("SOL,TENOR"))
        assertEquals(Clef.entries.toSet(), storedPreferredClefs(null))
        assertEquals(Clef.entries.toSet(), storedPreferredClefs(""))
        assertEquals(Clef.entries.toSet(), storedPreferredClefs("NOPE"))
    }

    @Test
    fun pianoAssetPathUsesScientificPitchNames() {
        assertEquals("notes/2C.ogg", Pitch(14).pianoAssetPath())
        assertEquals("notes/3A.ogg", Pitch(26).pianoAssetPath())
        assertEquals("notes/4C.ogg", Pitch(28).pianoAssetPath())
        assertEquals("notes/4G.ogg", Pitch(32).pianoAssetPath())
        assertEquals("notes/6C.ogg", Pitch(42).pianoAssetPath())
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
        assertEquals("Dó Ré Mi Fá Sol Lá Si", NoteNaming.PORTUGUESE.scalePreview())
        assertEquals("ド レ ミ ファ ソ ラ シ", NoteNaming.JAPANESE.scalePreview())
        assertEquals("Do Re Mi Fa Sol La Si", NoteNaming.TURKISH.scalePreview())
        assertEquals("до ре ми фа соль ля си", NoteNaming.RUSSIAN.scalePreview())
        assertEquals("دو ري مي فا صول لا سي", NoteNaming.ARABIC.scalePreview())
    }

    @Test
    fun unsetNoteNamingFollowsTheAppLanguage() {
        assertEquals(NoteNaming.SOLFEGE, storedNoteNaming(null, AppLanguage.FRENCH))
        assertEquals(NoteNaming.SOLFEGE, storedNoteNaming(null, AppLanguage.ITALIAN))
        assertEquals(NoteNaming.SOLFEGE, storedNoteNaming(null, AppLanguage.SPANISH_LATIN_AMERICA))
        assertEquals(NoteNaming.SOLFEGE, storedNoteNaming(null, AppLanguage.INDONESIAN))
        assertEquals(NoteNaming.PORTUGUESE, storedNoteNaming(null, AppLanguage.PORTUGUESE_BRAZIL))
        assertEquals(NoteNaming.ENGLISH, storedNoteNaming(null, AppLanguage.ENGLISH))
        assertEquals(NoteNaming.ENGLISH, storedNoteNaming(null, AppLanguage.HINDI))
        assertEquals(NoteNaming.GERMAN, storedNoteNaming(null, AppLanguage.GERMAN))
        assertEquals(NoteNaming.JAPANESE, storedNoteNaming(null, AppLanguage.JAPANESE))
        assertEquals(NoteNaming.TURKISH, storedNoteNaming(null, AppLanguage.TURKISH))
        assertEquals(NoteNaming.RUSSIAN, storedNoteNaming(null, AppLanguage.RUSSIAN))
        assertEquals(NoteNaming.ARABIC, storedNoteNaming(null, AppLanguage.ARABIC))
        assertEquals(NoteNaming.ENGLISH, storedNoteNaming("ENGLISH", AppLanguage.FRENCH))
        assertEquals(NoteNaming.SOLFEGE, storedNoteNaming("SOLFEGE", AppLanguage.ENGLISH))
        assertEquals(NoteNaming.GERMAN, storedNoteNaming("unknown", AppLanguage.GERMAN))
        AppLanguage.entries.forEach { language ->
            assertEquals(7, language.defaultNoteNaming().scalePreview().split(" ").size)
        }
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
    fun spellChordMatchesTextbookSolfegeOnEveryNaturalRoot() {
        val do4 = 28
        assertSpelling(do4, ChordQuality.MAJOR, "DO", "MI", "SOL")
        assertSpelling(do4, ChordQuality.MINOR, "DO", "MIb", "SOL")
        assertSpelling(do4, ChordQuality.AUGMENTED, "DO", "MI", "SOL#")
        assertSpelling(do4, ChordQuality.DIMINISHED, "DO", "MIb", "SOLb")
        assertSpelling(do4, ChordQuality.DOMINANT_7, "DO", "MI", "SOL", "SIb")
        assertSpelling(do4, ChordQuality.MAJOR_7, "DO", "MI", "SOL", "SI")
        assertSpelling(do4, ChordQuality.MINOR_7, "DO", "MIb", "SOL", "SIb")
        assertSpelling(do4, ChordQuality.HALF_DIMINISHED_7, "DO", "MIb", "SOLb", "SIb")
        assertUnspellable(do4, ChordQuality.DIMINISHED_7)

        val re4 = 29
        assertSpelling(re4, ChordQuality.MAJOR, "RE", "FA#", "LA")
        assertSpelling(re4, ChordQuality.MINOR, "RE", "FA", "LA")
        assertSpelling(re4, ChordQuality.AUGMENTED, "RE", "FA#", "LA#")
        assertSpelling(re4, ChordQuality.DIMINISHED, "RE", "FA", "LAb")
        assertSpelling(re4, ChordQuality.DOMINANT_7, "RE", "FA#", "LA", "DO")
        assertSpelling(re4, ChordQuality.MAJOR_7, "RE", "FA#", "LA", "DO#")
        assertSpelling(re4, ChordQuality.MINOR_7, "RE", "FA", "LA", "DO")
        assertSpelling(re4, ChordQuality.HALF_DIMINISHED_7, "RE", "FA", "LAb", "DO")
        assertSpelling(re4, ChordQuality.DIMINISHED_7, "RE", "FA", "LAb", "DOb")

        val mi4 = 30
        assertSpelling(mi4, ChordQuality.MAJOR, "MI", "SOL#", "SI")
        assertSpelling(mi4, ChordQuality.MINOR, "MI", "SOL", "SI")
        assertSpelling(mi4, ChordQuality.AUGMENTED, "MI", "SOL#", "SI#")
        assertSpelling(mi4, ChordQuality.DIMINISHED, "MI", "SOL", "SIb")
        assertSpelling(mi4, ChordQuality.DOMINANT_7, "MI", "SOL#", "SI", "RE")
        assertSpelling(mi4, ChordQuality.MAJOR_7, "MI", "SOL#", "SI", "RE#")
        assertSpelling(mi4, ChordQuality.MINOR_7, "MI", "SOL", "SI", "RE")
        assertSpelling(mi4, ChordQuality.HALF_DIMINISHED_7, "MI", "SOL", "SIb", "RE")
        assertSpelling(mi4, ChordQuality.DIMINISHED_7, "MI", "SOL", "SIb", "REb")

        val fa4 = 31
        assertSpelling(fa4, ChordQuality.MAJOR, "FA", "LA", "DO")
        assertSpelling(fa4, ChordQuality.MINOR, "FA", "LAb", "DO")
        assertSpelling(fa4, ChordQuality.AUGMENTED, "FA", "LA", "DO#")
        assertSpelling(fa4, ChordQuality.DIMINISHED, "FA", "LAb", "DOb")
        assertSpelling(fa4, ChordQuality.DOMINANT_7, "FA", "LA", "DO", "MIb")
        assertSpelling(fa4, ChordQuality.MAJOR_7, "FA", "LA", "DO", "MI")
        assertSpelling(fa4, ChordQuality.MINOR_7, "FA", "LAb", "DO", "MIb")
        assertSpelling(fa4, ChordQuality.HALF_DIMINISHED_7, "FA", "LAb", "DOb", "MIb")
        assertUnspellable(fa4, ChordQuality.DIMINISHED_7)

        val sol4 = 32
        assertSpelling(sol4, ChordQuality.MAJOR, "SOL", "SI", "RE")
        assertSpelling(sol4, ChordQuality.MINOR, "SOL", "SIb", "RE")
        assertSpelling(sol4, ChordQuality.AUGMENTED, "SOL", "SI", "RE#")
        assertSpelling(sol4, ChordQuality.DIMINISHED, "SOL", "SIb", "REb")
        assertSpelling(sol4, ChordQuality.DOMINANT_7, "SOL", "SI", "RE", "FA")
        assertSpelling(sol4, ChordQuality.MAJOR_7, "SOL", "SI", "RE", "FA#")
        assertSpelling(sol4, ChordQuality.MINOR_7, "SOL", "SIb", "RE", "FA")
        assertSpelling(sol4, ChordQuality.HALF_DIMINISHED_7, "SOL", "SIb", "REb", "FA")
        assertSpelling(sol4, ChordQuality.DIMINISHED_7, "SOL", "SIb", "REb", "FAb")

        val la4 = 33
        assertSpelling(la4, ChordQuality.MAJOR, "LA", "DO#", "MI")
        assertSpelling(la4, ChordQuality.MINOR, "LA", "DO", "MI")
        assertSpelling(la4, ChordQuality.AUGMENTED, "LA", "DO#", "MI#")
        assertSpelling(la4, ChordQuality.DIMINISHED, "LA", "DO", "MIb")
        assertSpelling(la4, ChordQuality.DOMINANT_7, "LA", "DO#", "MI", "SOL")
        assertSpelling(la4, ChordQuality.MAJOR_7, "LA", "DO#", "MI", "SOL#")
        assertSpelling(la4, ChordQuality.MINOR_7, "LA", "DO", "MI", "SOL")
        assertSpelling(la4, ChordQuality.HALF_DIMINISHED_7, "LA", "DO", "MIb", "SOL")
        assertSpelling(la4, ChordQuality.DIMINISHED_7, "LA", "DO", "MIb", "SOLb")

        val si4 = 34
        assertSpelling(si4, ChordQuality.MAJOR, "SI", "RE#", "FA#")
        assertSpelling(si4, ChordQuality.MINOR, "SI", "RE", "FA#")
        assertUnspellable(si4, ChordQuality.AUGMENTED)
        assertSpelling(si4, ChordQuality.DIMINISHED, "SI", "RE", "FA")
        assertSpelling(si4, ChordQuality.DOMINANT_7, "SI", "RE#", "FA#", "LA")
        assertSpelling(si4, ChordQuality.MAJOR_7, "SI", "RE#", "FA#", "LA#")
        assertSpelling(si4, ChordQuality.MINOR_7, "SI", "RE", "FA#", "LA")
        assertSpelling(si4, ChordQuality.HALF_DIMINISHED_7, "SI", "RE", "FA", "LA")
        assertSpelling(si4, ChordQuality.DIMINISHED_7, "SI", "RE", "FA", "LAb")
    }

    @Test
    fun everySpellableChordKeepsStackedThirdsAndQualityIntervals() {
        (14..42).forEach { step ->
            ChordQuality.entries.forEach { quality ->
                val chord = spellChord(Pitch(step), quality, Clef.SOL) ?: return@forEach
                assertEquals(quality, chord.quality)
                assertEquals(Pitch(step), chord.root)
                chord.notes.forEachIndexed { index, note ->
                    assertEquals(
                        step + quality.diatonicOffsets[index],
                        note.pitch.diatonicStep,
                    )
                }
                assertEquals(quality.semitoneIntervals, intervalPattern(chord))
            }
        }
    }

    @Test
    fun isCorrectChordComparesQualityOnly() {
        val chord = spellChord(Pitch(28), ChordQuality.MAJOR, Clef.SOL)!!
        assertTrue(isCorrect(chord, ChordQuality.MAJOR))
        assertFalse(isCorrect(chord, ChordQuality.MINOR))
    }
}

private fun assertSpelling(rootStep: Int, quality: ChordQuality, vararg expected: String) {
    val chord = spellChord(Pitch(rootStep), quality, Clef.SOL)
    assertTrue("$quality on ${Pitch(rootStep).noteName} should spell", chord != null)
    assertEquals(expected.toList(), chord!!.notes.map { spelled(it) })
    assertEquals(quality.semitoneIntervals, intervalPattern(chord))
}

private fun assertUnspellable(rootStep: Int, quality: ChordQuality) {
    assertNull(
        "$quality on ${Pitch(rootStep).noteName} needs a double accidental",
        spellChord(Pitch(rootStep), quality, Clef.SOL),
    )
}

private fun intervalPattern(chord: PracticeChord): List<Int> {
    val root = chord.notes.first().chromaticSemitone()
    return chord.notes.map { it.chromaticSemitone() - root }
}

private fun spelled(note: StaffNote): String {
    val suffix = when (note.accidental) {
        Accidental.SHARP -> "#"
        Accidental.FLAT -> "b"
        Accidental.NONE, Accidental.NATURAL -> ""
    }
    return note.pitch.noteName.name + suffix
}
