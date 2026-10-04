package com.dfuzer.birdnote

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dfuzer.birdnote.audio.AnswerFeedback
import com.dfuzer.birdnote.audio.QuizSounds
import com.dfuzer.birdnote.audio.SoundTone
import com.dfuzer.birdnote.data.ScoreStore
import com.dfuzer.birdnote.data.SettingsStore
import com.dfuzer.birdnote.domain.AppLanguage
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.NoteNaming
import com.dfuzer.birdnote.domain.PracticeMode
import com.dfuzer.birdnote.domain.StoredSetup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreCompatibilityTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test
    fun legacyKeysAndValuesSurviveThePackageMoveAndStoreRecreation() {
        val prefs = context.getSharedPreferences("birdnote_settings", Context.MODE_PRIVATE)
        prefs.edit().clear()
            .putString("app_language", "en")
            .putString("note_naming", "GERMAN")
            .putString("preferred_clefs", "FA,ALTO")
            .putBoolean("preferred_clefs_chosen", true)
            .putInt("setup_NOTES_difficulty", 4)
            .putString("setup_NOTES_clef", "ALTO")
            .putInt("setup_CHORDS_difficulty", 3)
            .putString("setup_CHORDS_clef", "FA")
            .putInt("setup_INTERVALS_difficulty", 2)
            .commit()
        val scores = context.getSharedPreferences("birdnote_scores", Context.MODE_PRIVATE)
        scores.edit().clear().putInt("NOTES:ALTO:4", 25).putInt("INTERVALS:-:2", 13).commit()
        val settings = SettingsStore(context)
        assertEquals(AppLanguage.ENGLISH, settings.appLanguage.value)
        assertEquals(NoteNaming.GERMAN, settings.noteNaming.value)
        assertEquals(setOf(Clef.FA, Clef.ALTO), settings.preferredClefs.value)
        assertTrue(settings.preferredClefsChosen.value)
        assertEquals(StoredSetup(4, ClefMode.ALTO), settings.lastSetup(PracticeMode.NOTES))
        assertEquals(StoredSetup(3, ClefMode.FA), settings.lastSetup(PracticeMode.CHORDS))
        assertEquals(StoredSetup(2, null), settings.lastSetup(PracticeMode.INTERVALS))
        val store = ScoreStore(context)
        store.record(PracticeMode.NOTES, 4, ClefMode.ALTO, 10)
        assertEquals(25, scores.getInt("NOTES:ALTO:4", -1))
        store.record(PracticeMode.NOTES, 4, ClefMode.ALTO, 30)
        settings.saveLastSetup(PracticeMode.NOTES, 3, ClefMode.ALTO)
        settings.setNoteNaming(NoteNaming.PORTUGUESE)
        val recreated = SettingsStore(context)
        assertEquals(StoredSetup(3, ClefMode.ALTO), recreated.lastSetup(PracticeMode.NOTES))
        assertEquals(NoteNaming.PORTUGUESE, recreated.noteNaming.value)
        assertEquals("PORTUGUESE", prefs.getString("note_naming", null))
        assertEquals(30, ScoreStore(context).scores.value["NOTES:ALTO:4"])
        assertEquals(13, ScoreStore(context).scores.value["INTERVALS:-:2"])
    }

    @Test
    fun audioCanBeReleasedTwiceDuringLoadingAndIgnoresLatePlayback() {
        instrumentation.runOnMainSync {
            val sounds = QuizSounds(context.assets)
            sounds.play(AnswerFeedback(1, true, listOf(SoundTone(28))))
            sounds.release()
            sounds.release()
            sounds.play(AnswerFeedback(2, false, emptyList()))
        }
        val deadline = android.os.SystemClock.uptimeMillis() + 5_000
        while (Thread.getAllStackTraces().keys.any { it.isAlive && it.name == "QuizSounds" } &&
            android.os.SystemClock.uptimeMillis() < deadline) Thread.sleep(20)
        assertTrue("Audio worker leaked after release", Thread.getAllStackTraces().keys.none { it.isAlive && it.name == "QuizSounds" })
    }
}
