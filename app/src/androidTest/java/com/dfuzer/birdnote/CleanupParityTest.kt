package com.dfuzer.birdnote

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.dfuzer.birdnote.domain.ClefMode
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Runs against either APK: uses only navigation and stable resource/domain APIs. */
class CleanupParityTest {
    @get:Rule(order = 0)
    val settings = object : TestWatcher() {
        override fun starting(description: Description) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("birdnote_settings", Context.MODE_PRIVATE).edit().clear()
                .putString("app_language", "en")
                .putString("preferred_clefs", "SOL,FA,ALTO,TENOR")
                .putBoolean("preferred_clefs_chosen", true)
                .commit()
            context.getSharedPreferences("birdnote_scores", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun captureEverySetupDifficultyAndClef() {
        capture("home")
        for ((mode, title) in listOf("notes" to R.string.notes, "intervals" to R.string.intervals, "chords" to R.string.chords)) {
            click(R.string.train)
            click(title)
            val clefs = if (mode == "intervals") listOf(ClefMode.SOL) else ClefMode.entries
            var shown = ClefMode.SOL
            for (clef in clefs) {
                if (shown != clef) {
                    compose.onNodeWithText(clefLabel(shown)).performClick()
                    compose.onAllNodesWithText(clefLabel(clef)).onLast().performClick()
                    shown = clef
                }
                for (difficulty in 1..4) {
                    compose.onNodeWithText(difficulty.toString()).performClick()
                    capture("${mode}_${clef}_$difficulty")
                }
            }
            back()
            back()
        }
        click(R.string.settings)
        capture("settings")
        back()
        click(R.string.my_scores)
        capture("scores")
    }

    @Test
    fun staffSurvivesBackgroundResizeRapidAnswersAndRestart() {
        val device = InstrumentationRegistry.getInstrumentation().uiAutomation
        for (title in listOf(R.string.notes, R.string.intervals, R.string.chords)) {
            click(R.string.train)
            click(title)
            click(R.string.lets_go)
            compose.onNodeWithText(compose.activity.getString(R.string.stop)).assertIsDisplayed()
            // A real stop/start forces the external surface through destruction and recreation.
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
            compose.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
            compose.waitForIdle()
            val answer = when (title) {
                R.string.notes -> "C"
                R.string.intervals -> compose.activity.getString(R.string.interval_second)
                else -> compose.activity.getString(R.string.chord_major)
            }
            repeat(8) { compose.onNodeWithText(answer).performClick() }
            // Exercise surface resizing without recreating the activity.
            compose.runOnUiThread {
                val decor = compose.activity.window.decorView
                decor.setPadding(12, 0, 12, 0)
            }
            compose.waitForIdle()
            compose.runOnUiThread { compose.activity.window.decorView.setPadding(0, 0, 0, 0) }
            compose.waitForIdle()
            Thread.sleep(150)
            val bitmap = checkNotNull(device.takeScreenshot())
            // Staff card lies in the middle of the landscape window: it must have notation ink.
            var darkPixels = 0
            for (y in bitmap.height / 3..bitmap.height * 2 / 3 step 3) {
                for (x in bitmap.width / 3..bitmap.width * 2 / 3 step 3) {
                    val color = bitmap.getPixel(x, y)
                    if (android.graphics.Color.red(color) < 70 && android.graphics.Color.green(color) < 70 && android.graphics.Color.blue(color) < 70) darkPixels++
                }
            }
            bitmap.recycle()
            org.junit.Assert.assertTrue("Staff disappeared after surface recreation", darkPixels > 10)
            click(R.string.stop)
            capture("result_$title")
            click(R.string.restart)
            click(R.string.stop)
            click(R.string.menu)
        }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        // Allow the asynchronously loaded SVGs and SurfaceFlinger to present their buffers.
        Thread.sleep(250)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.filesDir, "cleanup-parity").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        screenshot.recycle()
    }

    private fun click(resource: Int) = compose.onNodeWithText(compose.activity.getString(resource)).performClick()
    private fun back() = compose.onNodeWithContentDescription(compose.activity.getString(R.string.back)).performClick()
    private fun clefLabel(mode: ClefMode): String = compose.activity.getString(when (mode) {
        ClefMode.SOL -> R.string.clef_sol
        ClefMode.FA -> R.string.clef_fa
        ClefMode.SOL_FA -> R.string.clef_sol_fa
        ClefMode.ALTO -> R.string.clef_alto
        ClefMode.TENOR -> R.string.clef_tenor
    })
}
