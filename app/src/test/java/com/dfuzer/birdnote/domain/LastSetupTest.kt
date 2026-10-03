package com.dfuzer.birdnote.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastSetupTest {
    @Test
    fun nothingSavedOpensTheExistingDefaults() {
        PracticeMode.entries.forEach { mode ->
            val setup = storedSetup(mode, null, null)
            assertEquals(MIN_DIFFICULTY, setup.difficulty)
            when (mode) {
                PracticeMode.INTERVALS -> assertNull(setup.clefMode)
                PracticeMode.NOTES, PracticeMode.CHORDS ->
                    assertEquals(ClefMode.SOL, setup.clefMode)
            }
        }
    }

    @Test
    fun notesAndChordsRestoreDifficultyAndClefOnTheirOwn() {
        assertEquals(
            StoredSetup(3, ClefMode.FA),
            storedSetup(PracticeMode.NOTES, 3, ClefMode.FA.name),
        )
        assertEquals(
            StoredSetup(4, ClefMode.SOL_FA),
            storedSetup(PracticeMode.CHORDS, 4, ClefMode.SOL_FA.name),
        )
        assertEquals(
            StoredSetup(2, ClefMode.SOL),
            storedSetup(PracticeMode.NOTES, 2, ClefMode.SOL.name),
        )
    }

    @Test
    fun intervalsRestoreDifficultyOnly() {
        assertEquals(
            StoredSetup(2, null),
            storedSetup(PracticeMode.INTERVALS, 2, ClefMode.FA.name),
        )
        assertEquals(
            StoredSetup(MAX_DIFFICULTY, null),
            storedSetup(PracticeMode.INTERVALS, MAX_DIFFICULTY, null),
        )
    }

    @Test
    fun invalidValuesFallBackPerField() {
        assertEquals(
            StoredSetup(MIN_DIFFICULTY, ClefMode.FA),
            storedSetup(PracticeMode.NOTES, 0, ClefMode.FA.name),
        )
        assertEquals(
            StoredSetup(MAX_DIFFICULTY, ClefMode.SOL),
            storedSetup(PracticeMode.NOTES, MAX_DIFFICULTY, "alto"),
        )
        assertEquals(
            StoredSetup(MIN_DIFFICULTY, ClefMode.SOL),
            storedSetup(PracticeMode.CHORDS, 9, null),
        )
        assertEquals(
            StoredSetup(MIN_DIFFICULTY, null),
            storedSetup(PracticeMode.INTERVALS, null, ClefMode.SOL_FA.name),
        )
    }
}
