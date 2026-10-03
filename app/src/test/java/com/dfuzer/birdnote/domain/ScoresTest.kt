package com.dfuzer.birdnote.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScoresTest {
    @Test
    fun firstScoreBecomesTheBest() {
        assertEquals(0, improvedBest(null, 0))
        assertEquals(7, improvedBest(null, 7))
    }

    @Test
    fun higherScoreReplacesTheBest() {
        assertEquals(8, improvedBest(7, 8))
    }

    @Test
    fun lowerOrEqualScoreKeepsTheBest() {
        assertNull(improvedBest(7, 7))
        assertNull(improvedBest(7, 3))
    }

    @Test
    fun noteAndChordGridsCoverEveryClefAndDifficulty() {
        listOf(PracticeMode.NOTES, PracticeMode.CHORDS).forEach { mode ->
            val cells = scoreGrid(mode)
            assertEquals(ClefMode.entries.size * (MAX_DIFFICULTY - MIN_DIFFICULTY + 1), cells.size)
            ClefMode.entries.forEach { clef ->
                (MIN_DIFFICULTY..MAX_DIFFICULTY).forEach { difficulty ->
                    assertTrue(ScoreCell(difficulty, clef) in cells)
                }
            }
        }
    }

    @Test
    fun intervalGridIsDifficultyOnly() {
        assertEquals(
            (MIN_DIFFICULTY..MAX_DIFFICULTY).map { ScoreCell(it) },
            scoreGrid(PracticeMode.INTERVALS),
        )
    }

    @Test
    fun storageKeysStayUnique() {
        val keys = PracticeMode.entries.flatMap { mode ->
            scoreGrid(mode).map { cell ->
                scoreStorageKey(mode, cell.difficulty, cell.clefMode)
            }
        }
        assertEquals(keys.toSet().size, keys.size)
        assertEquals("NOTES:SOL:1", scoreStorageKey(PracticeMode.NOTES, 1, ClefMode.SOL))
        assertEquals("INTERVALS:-:4", scoreStorageKey(PracticeMode.INTERVALS, 4, null))
        assertEquals("CHORDS:SOL_FA:2", scoreStorageKey(PracticeMode.CHORDS, 2, ClefMode.SOL_FA))
        assertEquals(12, bestScore(mapOf("NOTES:SOL:1" to 12), PracticeMode.NOTES, 1, ClefMode.SOL))
        assertNull(bestScore(emptyMap(), PracticeMode.NOTES, 1, ClefMode.FA))
        assertEquals("NOTES:ALTO:3", scoreStorageKey(PracticeMode.NOTES, 3, ClefMode.ALTO))
        assertEquals("CHORDS:TENOR:4", scoreStorageKey(PracticeMode.CHORDS, 4, ClefMode.TENOR))
    }

    @Test
    fun hiddenClefDropsFromTheGridAndKeepsItsStoredBest() {
        val stored = mapOf(scoreStorageKey(PracticeMode.NOTES, 2, ClefMode.ALTO) to 9)
        val visible = clefModesFor(setOf(Clef.SOL, Clef.FA))
        val hiddenGrid = scoreGrid(PracticeMode.NOTES, visible)
        assertFalse(hiddenGrid.any { it.clefMode == ClefMode.ALTO })
        assertEquals(9, bestScore(stored, PracticeMode.NOTES, 2, ClefMode.ALTO))

        val shown = scoreGrid(PracticeMode.NOTES, clefModesFor(setOf(Clef.SOL, Clef.ALTO)))
        assertTrue(ScoreCell(2, ClefMode.ALTO) in shown)
        assertFalse(shown.any { it.clefMode == ClefMode.SOL_FA })
        assertEquals(9, bestScore(stored, PracticeMode.NOTES, 2, ClefMode.ALTO))
    }
}
