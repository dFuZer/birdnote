package com.example.birdnote

import android.content.Context
import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.NoteNaming
import com.example.birdnote.domain.PracticeMode
import com.example.birdnote.domain.StoredSetup
import com.example.birdnote.domain.storedNoteNaming
import com.example.birdnote.domain.storedSetup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val SETTINGS_PREFS_NAME = "birdnote_settings"

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        SETTINGS_PREFS_NAME,
        Context.MODE_PRIVATE,
    )
    private val _noteNaming = MutableStateFlow(
        storedNoteNaming(prefs.getString(KEY_NOTE_NAMING, null)),
    )
    val noteNaming: StateFlow<NoteNaming> = _noteNaming.asStateFlow()

    fun setNoteNaming(naming: NoteNaming) {
        prefs.edit().putString(KEY_NOTE_NAMING, naming.name).apply()
        _noteNaming.value = naming
    }

    fun lastSetup(mode: PracticeMode): StoredSetup {
        val difficulty = prefs.all[difficultyKey(mode)] as? Int
        val clefName = prefs.getString(clefKey(mode), null)
        return storedSetup(mode, difficulty, clefName)
    }

    fun saveLastSetup(mode: PracticeMode, difficulty: Int, clefMode: ClefMode?) {
        val editor = prefs.edit().putInt(difficultyKey(mode), difficulty)
        when (mode) {
            PracticeMode.INTERVALS -> editor.remove(clefKey(mode))
            PracticeMode.NOTES, PracticeMode.CHORDS ->
                editor.putString(clefKey(mode), checkNotNull(clefMode).name)
        }
        editor.commit()
    }

    private fun difficultyKey(mode: PracticeMode) = "setup_${mode.name}_difficulty"

    private fun clefKey(mode: PracticeMode) = "setup_${mode.name}_clef"

    private companion object {
        const val KEY_NOTE_NAMING = "note_naming"
    }
}
