package com.dfuzer.birdnote

import android.content.Context
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

class PublicationReadinessTest {
    @get:Rule(order = 0) val settings = object : TestWatcher() {
        override fun starting(description: Description) {
            InstrumentationRegistry.getInstrumentation().targetContext
                .getSharedPreferences("birdnote_settings", Context.MODE_PRIVATE)
                .edit().clear().putString("app_language", "en")
                .putBoolean("preferred_clefs_chosen", true).commit()
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    @Test fun quizHasNotationAndTimerSemanticsAndPausesAcrossBackgrounding() {
        compose.onNodeWithText("Practice").performClick()
        compose.onNodeWithText("Notes").performClick()
        compose.onNodeWithText("Let's go!").performClick()
        compose.onNode(SemanticsMatcher("Describes staff position") { node ->
            node.config.getOrElse(SemanticsProperties.ContentDescription) { emptyList() }
                .any { it.contains("Treble") && (it.contains("Line") || it.contains("Space") || it.contains("staff steps")) }
        }).assertIsDisplayed()
        val countdown = compose.onNodeWithContentDescription("Time remaining")
        assertTrue(countdown.fetchSemanticsNode().config.contains(SemanticsProperties.ProgressBarRangeInfo))
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        // Read the framework-saved checkpoint rather than a stopped Compose tree.
        Thread.sleep(1_000)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        countdown.assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Stop").assertIsDisplayed()
        compose.onNodeWithText("Stop").performClick()
        compose.onNodeWithText("Great job!").assertIsDisplayed()
    }

    @Test fun soundPreferencePersistsAndHelpAndFontLicenseAreAccessible() {
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithText("Sound").performScrollTo().performClick()
        val prefs = compose.activity.getSharedPreferences("birdnote_settings", Context.MODE_PRIVATE)
        compose.runOnIdle { assertEquals(false, prefs.getBoolean("sound_enabled", true)) }
        compose.activityRule.scenario.recreate()
        compose.runOnIdle { assertEquals(false, prefs.getBoolean("sound_enabled", true)) }
        compose.onNodeWithText("Source code").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("How to play").performScrollTo().performClick()
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Open-source licenses").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onNodeWithText("SIL OPEN FONT LICENSE", substring = true)
                .runCatching { fetchSemanticsNode() }.isSuccess
        }
        compose.onNodeWithText("SIL OPEN FONT LICENSE", substring = true).assertIsDisplayed()
    }
}
