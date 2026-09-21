package com.example.birdnote

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
            InstrumentationRegistry.getInstrumentation().targetContext
                .getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun notesTrainingFlowReachesResultsAndCanRestartOrReturnHome() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.my_scores))
            .assertIsNotEnabled()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.leaderboard))
            .assertIsNotEnabled()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.settings))
            .assertIsEnabled()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.intervals))
            .assertIsEnabled()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.chords))
            .assertIsEnabled()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.rhythms))
            .assertIsNotEnabled()

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
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.intervals)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.lets_go)).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.stop))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Seconde").assertIsDisplayed()
        composeRule.onNodeWithText("Quarte").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.stop)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.bravo)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.menu)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).assertIsDisplayed()
    }

    @Test
    fun chordsTrainingFlowReachesResults() {
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.chords)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.lets_go)).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.stop))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Majeur").assertIsDisplayed()
        composeRule.onNodeWithText("Mineur").assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.stop)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.bravo)).assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.menu)).performClick()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.train)).assertIsDisplayed()
    }

    @Test
    fun settingsPersistEnglishNoteNamesOnTheQuiz() {
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
}
