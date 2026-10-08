package com.dfuzer.birdnote

import android.app.Application
import android.content.Context
import com.dfuzer.birdnote.audio.QuizSounds
import com.dfuzer.birdnote.data.KEY_SOUND_ENABLED
import com.dfuzer.birdnote.data.SETTINGS_PREFS_NAME

class BirdNoteApplication : Application() {
    private val soundsLock = Any()
    @Volatile private var sounds: QuizSounds? = null

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base.withResolvedLocale())
    }

    override fun onCreate() {
        super.onCreate()
        val enabled = getSharedPreferences(SETTINGS_PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_SOUND_ENABLED, true)
        if (enabled) setSoundEnabled(true)
    }

    fun quizSounds(): QuizSounds? = sounds

    fun setSoundEnabled(enabled: Boolean) {
        synchronized(soundsLock) {
            if (enabled) {
                if (sounds == null) sounds = QuizSounds(assets)
            } else {
                sounds?.release()
                sounds = null
            }
        }
    }
}
