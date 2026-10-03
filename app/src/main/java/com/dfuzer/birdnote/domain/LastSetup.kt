package com.dfuzer.birdnote.domain

data class StoredSetup(
    val difficulty: Int,
    val clefMode: ClefMode?,
)

fun storedSetup(mode: PracticeMode, difficulty: Int?, clefName: String?): StoredSetup {
    val restoredDifficulty = difficulty?.takeIf { it in MIN_DIFFICULTY..MAX_DIFFICULTY }
        ?: MIN_DIFFICULTY
    val restoredClef = when (mode) {
        PracticeMode.INTERVALS -> null
        PracticeMode.NOTES, PracticeMode.CHORDS ->
            ClefMode.entries.firstOrNull { it.name == clefName } ?: ClefMode.SOL
    }
    return StoredSetup(restoredDifficulty, restoredClef)
}
