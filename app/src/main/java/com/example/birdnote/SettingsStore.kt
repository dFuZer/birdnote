package com.example.birdnote

import android.content.Context
import com.example.birdnote.domain.NoteNaming
import com.example.birdnote.domain.storedNoteNaming
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

    private companion object {
        const val KEY_NOTE_NAMING = "note_naming"
    }
}
