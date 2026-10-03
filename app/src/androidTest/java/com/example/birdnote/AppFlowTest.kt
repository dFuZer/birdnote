package com.example.birdnote

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.example.birdnote.domain.NoteName
import com.example.birdnote.domain.NoteNaming
import com.example.birdnote.domain.label
import com.example.birdnote.domain.scalePreview
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
        composeRule.onNodeWithText(NoteName.DO.label(NoteNaming.SOLFEGE)).assertIsDisplayed()
        composeRule.onNodeWithText(NoteName.SI.label(NoteNaming.SOLFEGE)).assertIsDisplayed()
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
    fun settingsPersistEnglishNoteNamesOnTheQuiz() {
        continueFromClefChoice()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.settings)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.note_names))
            .assertIsDisplayed()
        composeRule.onNodeWithText(NoteNaming.SOLFEGE.scalePreview()).performClick()
        composeRule.onNodeWithText(NoteNaming.ENGLISH.scalePreview()).performClick()
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

    private fun continueFromClefChoice() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.continue_action))
            .performClick()
    }
}
