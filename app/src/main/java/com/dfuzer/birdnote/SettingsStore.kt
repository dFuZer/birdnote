package com.dfuzer.birdnote

import android.content.Context
import com.dfuzer.birdnote.domain.AppLanguage
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.NoteNaming
import com.dfuzer.birdnote.domain.PracticeMode
import com.dfuzer.birdnote.domain.StoredSetup
import com.dfuzer.birdnote.domain.preferredClefsStorage
import com.dfuzer.birdnote.domain.resolveAppLanguage
import com.dfuzer.birdnote.domain.storedNoteNaming
import com.dfuzer.birdnote.domain.storedPreferredClefs
import com.dfuzer.birdnote.domain.storedSetup
import com.dfuzer.birdnote.domain.withClefToggled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val SETTINGS_PREFS_NAME = "birdnote_settings"

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        SETTINGS_PREFS_NAME,
        Context.MODE_PRIVATE,
    )
    private val _appLanguage = MutableStateFlow(
        resolveAppLanguage(prefs.getString(KEY_APP_LANGUAGE, null), systemDeviceLocale()),
    )
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()
    private val _noteNaming = MutableStateFlow(
        storedNoteNaming(prefs.getString(KEY_NOTE_NAMING, null), _appLanguage.value),
    )
    val noteNaming: StateFlow<NoteNaming> = _noteNaming.asStateFlow()
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

    fun setAppLanguage(language: AppLanguage) {
        prefs.edit().putString(KEY_APP_LANGUAGE, language.tag).apply()
        _appLanguage.value = language
        _noteNaming.value = storedNoteNaming(prefs.getString(KEY_NOTE_NAMING, null), language)
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
