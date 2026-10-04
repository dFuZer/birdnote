package com.dfuzer.birdnote

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.dfuzer.birdnote.domain.AppLanguage
import com.dfuzer.birdnote.domain.NoteName
import com.dfuzer.birdnote.domain.NoteNaming
import com.dfuzer.birdnote.domain.defaultNoteNaming
import com.dfuzer.birdnote.domain.label
import com.dfuzer.birdnote.domain.resolveAppLanguage
import com.dfuzer.birdnote.domain.scalePreview
import com.dfuzer.birdnote.ui.screens.labelRes
import java.util.Locale
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class AppFlowTest {
    @get:Rule(order = 0)
    val clearSettingsRule = object : TestWatcher() {
        override fun starting(description: Description) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            listOf(SETTINGS_PREFS_NAME, SCORES_PREFS_NAME).forEach { name ->
                context.getSharedPreferences(name, Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
            }
        }
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun notesTrainingFlowReachesResultsAndCanRestartOrReturnHome() {
        continueFromClefChoice()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.my_scores))
            .assertIsEnabled()
        composeRule.onNodeWithText("Classement").assertDoesNotExist()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.settings))
            .assertIsEnabled()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.intervals))
            .assertIsEnabled()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.chords))
            .assertIsEnabled()
        composeRule.onNodeWithText("Rythmes").assertDoesNotExist()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.notes)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.lets_go)).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.stop))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.stop)).assertIsDisplayed()
        val naming = unsetNoteNaming()
        composeRule.onNodeWithText(NoteName.DO.label(naming)).assertIsDisplayed()
        composeRule.onNodeWithText(NoteName.SI.label(naming)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.stop)).performClick()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.bravo)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.restart)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.stop)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.stop)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.menu)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).assertIsDisplayed()
    }

    @Test
    fun intervalsTrainingFlowReachesResults() {
        continueFromClefChoice()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.intervals)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.lets_go)).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.stop))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.interval_second))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.interval_fourth))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.stop)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.bravo)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.menu)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).assertIsDisplayed()
    }

    @Test
    fun chordsTrainingFlowReachesResults() {
        continueFromClefChoice()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.chords)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.lets_go)).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.stop))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.chord_major))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.chord_minor))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.stop)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.bravo)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.menu)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).assertIsDisplayed()
    }

    @Test
    fun switchingLanguageUpdatesTheOpenSettingsScreen() {
        continueFromClefChoice()
        val activity = composeRule.activity
        val starting = resolveAppLanguage(null, systemDeviceLocale())
        val target = if (starting == AppLanguage.ENGLISH) AppLanguage.FRENCH else AppLanguage.ENGLISH
        activity.getString(R.string.settings).let { title ->
            composeRule.onNodeWithText(title).performClick()
        }
        val forcedNaming = NoteNaming.GERMAN
        if (unsetNoteNaming() != forcedNaming) {
            composeRule.onNodeWithText(unsetNoteNaming().scalePreview()).performClick()
            composeRule.onNodeWithText(forcedNaming.scalePreview()).performClick()
        }
        composeRule.onNodeWithText(activity.getString(starting.labelRes())).performClick()
        composeRule.onNodeWithText(activity.getString(target.labelRes()))
            .performScrollTo()
            .performClick()

        assertSame(activity, composeRule.activity)
        val localized = localizedResources(target)
        composeRule.onNodeWithText(localized.getString(R.string.language)).assertIsDisplayed()
        composeRule.onNodeWithText(localized.getString(R.string.note_names)).assertIsDisplayed()
        composeRule.onNodeWithText(target.defaultNoteNaming().scalePreview()).assertIsDisplayed()
        composeRule.onNodeWithText(forcedNaming.scalePreview()).assertDoesNotExist()
    }

    @Test
    fun settingsPersistEnglishNoteNamesOnTheQuiz() {
        continueFromClefChoice()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.settings)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.note_names))
            .assertIsDisplayed()
        chooseEnglishNoteNames()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.back))
            .performClick()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.notes)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.lets_go)).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.stop))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(NoteName.DO.label(NoteNaming.ENGLISH)).assertIsDisplayed()
        composeRule.onNodeWithText(NoteName.SI.label(NoteNaming.ENGLISH)).assertIsDisplayed()
    }

    @Test
    fun bestScoreIsSavedForTheClefAndDifficultyJustPlayed() {
        continueFromClefChoice()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.notes)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.lets_go)).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.stop))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.stop)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.menu)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.my_scores)).performClick()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.clef)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.clef_sol)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.clef_fa)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.clef_alto)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.clef_tenor)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.clef_sol_fa)).assertIsDisplayed()
        composeRule.onNodeWithText("0").assertIsDisplayed()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.intervals)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.score_best)).assertIsDisplayed()
        composeRule.onNodeWithText("0").assertDoesNotExist()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.clef_sol)).assertDoesNotExist()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.chords)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.clef_sol)).assertIsDisplayed()
        composeRule.onNodeWithText("0").assertDoesNotExist()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.notes)).performClick()
        composeRule.onNodeWithText("0").assertIsDisplayed()
    }

    private fun localizedResources(language: AppLanguage) =
        Configuration(composeRule.activity.resources.configuration).run {
            setLocale(Locale.forLanguageTag(language.tag))
            composeRule.activity.createConfigurationContext(this).resources
        }

    private fun continueFromClefChoice() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.continue_action))
            .performClick()
    }

    private fun unsetNoteNaming(): NoteNaming =
        resolveAppLanguage(null, systemDeviceLocale()).defaultNoteNaming()

    private fun chooseEnglishNoteNames() {
        val current = unsetNoteNaming()
        if (current == NoteNaming.ENGLISH) {
            composeRule.onNodeWithText(NoteNaming.ENGLISH.scalePreview()).performClick()
            composeRule.onNodeWithText(NoteNaming.SOLFEGE.scalePreview()).performClick()
        }
        val shown = if (current == NoteNaming.ENGLISH) NoteNaming.SOLFEGE else current
        composeRule.onNodeWithText(shown.scalePreview()).performClick()
        composeRule.onNodeWithText(NoteNaming.ENGLISH.scalePreview()).performClick()
    }
}
