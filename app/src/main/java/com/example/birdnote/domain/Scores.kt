package com.example.birdnote.domain

enum class PracticeMode {
    NOTES,
    INTERVALS,
    CHORDS,
}

data class ScoreCell(
    val difficulty: Int,
    val clefMode: ClefMode? = null,
)

fun scoreGrid(
    mode: PracticeMode,
    clefModes: List<ClefMode> = ClefMode.entries,
): List<ScoreCell> {
    val difficulties = MIN_DIFFICULTY..MAX_DIFFICULTY
    return when (mode) {
        PracticeMode.INTERVALS -> difficulties.map { ScoreCell(it) }
        PracticeMode.NOTES, PracticeMode.CHORDS ->
            clefModes.flatMap { clef ->
                difficulties.map { ScoreCell(it, clef) }
            }
    }
}

fun scoreStorageKey(mode: PracticeMode, difficulty: Int, clefMode: ClefMode?): String {
    require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY) {
        "Difficulty must be between $MIN_DIFFICULTY and $MAX_DIFFICULTY"
    }
    val clef = when (mode) {
        PracticeMode.INTERVALS -> {
            require(clefMode == null) { "Intervals have no clef" }
            NO_CLEF
        }
        PracticeMode.NOTES, PracticeMode.CHORDS ->
            requireNotNull(clefMode) { "$mode scores keep a clef" }.name
    }
    return "${mode.name}:$clef:$difficulty"
}

fun bestScore(
    scores: Map<String, Int>,
    mode: PracticeMode,
    difficulty: Int,
    clefMode: ClefMode?,
): Int? = scores[scoreStorageKey(mode, difficulty, clefMode)]

/** Returns [candidate] when it should replace [current], otherwise null. */
fun improvedBest(current: Int?, candidate: Int): Int? {
    require(candidate >= 0) { "Score cannot be negative" }
    return if (current == null || candidate > current) candidate else null
}

private const val NO_CLEF = "-"
