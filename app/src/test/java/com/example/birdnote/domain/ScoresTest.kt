package com.example.birdnote.domain

import org.junit.Assert.assertEquals
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
    fun firstRunHasNoEarlierRecord() {
        assertEquals(BestComparison.FirstRun, compareToBest(null, 0))
        assertEquals(BestComparison.FirstRun, compareToBest(null, 4))
    }

    @Test
    fun newBestIsTheMarginAboveThePreviousBest() {
        assertEquals(BestComparison.AboveBest(3), compareToBest(7, 10))
        assertEquals(BestComparison.AboveBest(1), compareToBest(0, 1))
    }

    @Test
    fun comparingWithTheSavedBestWouldHideANewRecord() {
        val previous = 7
        val score = 10
        assertEquals(score, improvedBest(previous, score))
        assertEquals(BestComparison.Tie, compareToBest(score, score))
        assertEquals(BestComparison.AboveBest(3), compareToBest(previous, score))
    }

    @Test
    fun shortfallIsTheMarginBelowTheBest() {
        assertEquals(BestComparison.ShortOfBest(2), compareToBest(9, 7))
    }

    @Test
    fun tieMatchesTheStoredBest() {
        assertEquals(BestComparison.Tie, compareToBest(7, 7))
        assertEquals(BestComparison.Tie, compareToBest(0, 0))
    }

    @Test
    fun comparisonUsesTheBestForThatSessionCell() {
        val scores = mapOf(
            "NOTES:SOL:1" to 10,
            "NOTES:FA:1" to 4,
            "INTERVALS:-:1" to 7,
            "CHORDS:SOL:2" to 5,
        )
        assertEquals(
            BestComparison.AboveBest(3),
            compareToBest(bestScore(scores, PracticeMode.NOTES, 1, ClefMode.SOL), 13),
        )
        assertEquals(
            BestComparison.ShortOfBest(2),
            compareToBest(bestScore(scores, PracticeMode.NOTES, 1, ClefMode.FA), 2),
        )
        assertEquals(
            BestComparison.Tie,
            compareToBest(bestScore(scores, PracticeMode.INTERVALS, 1, null), 7),
        )
        assertEquals(
            BestComparison.Tie,
            compareToBest(bestScore(scores, PracticeMode.CHORDS, 2, ClefMode.SOL), 5),
        )
        assertEquals(
            BestComparison.FirstRun,
            compareToBest(bestScore(scores, PracticeMode.CHORDS, 2, ClefMode.FA), 1),
        )
        assertEquals(
            BestComparison.FirstRun,
            compareToBest(bestScore(scores, PracticeMode.INTERVALS, 2, null), 3),
        )
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
    }
}
