package com.dfuzer.birdnote

import android.content.Context
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.PracticeMode
import com.dfuzer.birdnote.domain.improvedBest
import com.dfuzer.birdnote.domain.scoreStorageKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val SCORES_PREFS_NAME = "birdnote_scores"

class ScoreStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        SCORES_PREFS_NAME,
        Context.MODE_PRIVATE,
    )
    private val _scores = MutableStateFlow(readAll())
    val scores: StateFlow<Map<String, Int>> = _scores.asStateFlow()

    fun record(mode: PracticeMode, difficulty: Int, clefMode: ClefMode?, score: Int) {
        val key = scoreStorageKey(mode, difficulty, clefMode)
        val updated = improvedBest(_scores.value[key], score) ?: return
        prefs.edit().putInt(key, updated).commit()
        _scores.value = _scores.value + (key to updated)
    }

    private fun readAll(): Map<String, Int> =
        prefs.all.mapNotNull { (key, value) ->
            val score = value as? Int ?: return@mapNotNull null
            key to score
        }.toMap()
}
