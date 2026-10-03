package com.example.birdnote

import android.content.Context
import com.example.birdnote.domain.AppLanguage
import com.example.birdnote.domain.Clef
import com.example.birdnote.domain.NoteNaming
import com.example.birdnote.domain.preferredClefsStorage
import com.example.birdnote.domain.resolveAppLanguage
import com.example.birdnote.domain.storedNoteNaming
import com.example.birdnote.domain.storedPreferredClefs
import com.example.birdnote.domain.withClefToggled
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
    private val _appLanguage = MutableStateFlow(
        resolveAppLanguage(prefs.getString(KEY_APP_LANGUAGE, null), systemDeviceLocale()),
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()
    private val _preferredClefs = MutableStateFlow(
        storedPreferredClefs(prefs.getString(KEY_PREFERRED_CLEFS, null)),
    )
    val preferredClefs: StateFlow<Set<Clef>> = _preferredClefs.asStateFlow()
    private val _preferredClefsChosen = MutableStateFlow(
        prefs.getBoolean(KEY_PREFERRED_CLEFS_CHOSEN, false),
    )
    val preferredClefsChosen: StateFlow<Boolean> = _preferredClefsChosen.asStateFlow()

    fun setNoteNaming(naming: NoteNaming) {
        prefs.edit().putString(KEY_NOTE_NAMING, naming.name).apply()
        _noteNaming.value = naming
    }

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.tag).apply()
        _appLanguage.value = language
    }

    fun togglePreferredClef(clef: Clef) {
        val next = withClefToggled(_preferredClefs.value, clef)
        if (next == _preferredClefs.value) return
        prefs.edit().putString(KEY_PREFERRED_CLEFS, preferredClefsStorage(next)).apply()
        _preferredClefs.value = next
    }

    fun confirmPreferredClefs() {
        prefs.edit()
            .putString(KEY_PREFERRED_CLEFS, preferredClefsStorage(_preferredClefs.value))
            .putBoolean(KEY_PREFERRED_CLEFS_CHOSEN, true)
            .apply()
        _preferredClefsChosen.value = true
    }

    private companion object {
        const val KEY_NOTE_NAMING = "note_naming"
        const val KEY_PREFERRED_CLEFS = "preferred_clefs"
        const val KEY_PREFERRED_CLEFS_CHOSEN = "preferred_clefs_chosen"
    }
}
