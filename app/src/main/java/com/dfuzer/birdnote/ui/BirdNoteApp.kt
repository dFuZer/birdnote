package com.dfuzer.birdnote.ui

import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.dfuzer.birdnote.data.ScoreStore
import com.dfuzer.birdnote.data.SettingsStore
import com.dfuzer.birdnote.domain.AppLanguage
import com.dfuzer.birdnote.ui.navigation.BirdNoteNavHost
import com.dfuzer.birdnote.ui.screens.PreferredClefsScreen
import com.dfuzer.birdnote.ui.theme.LightBlue
import com.dfuzer.birdnote.withLocale

@Composable
fun BirdNoteApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }
    val scoreStore = remember { ScoreStore(context) }
    val noteNaming by settingsStore.noteNaming.collectAsStateWithLifecycle()
    val appLanguage by settingsStore.appLanguage.collectAsStateWithLifecycle()
    val preferredClefs by settingsStore.preferredClefs.collectAsStateWithLifecycle()
    val preferredClefsChosen by settingsStore.preferredClefsChosen.collectAsStateWithLifecycle()
    ProvideAppLanguage(appLanguage) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = LightBlue,
        ) { innerPadding ->
            if (preferredClefsChosen) {
                BirdNoteNavHost(
                    navController = navController,
                    noteNaming = noteNaming,
                    onNoteNamingChange = settingsStore::setNoteNaming,
                    appLanguage = appLanguage,
                    onAppLanguageChange = settingsStore::setAppLanguage,
                    preferredClefs = preferredClefs,
                    onPreferredClefToggle = settingsStore::togglePreferredClef,
                    settingsStore = settingsStore,
                    scoreStore = scoreStore,
                    modifier = Modifier.padding(innerPadding),
                )
            } else {
                PreferredClefsScreen(
                    selected = preferredClefs,
                    onToggle = settingsStore::togglePreferredClef,
                    onContinue = settingsStore::confirmPreferredClefs,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

/**
 * Point string lookups at the selected language without restarting the activity.
 * A restart tears the whole screen down, which is what freezes the settings tab.
 */
@Composable
private fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val base = LocalContext.current
    val localized = remember(base, language) { base.withLocale(language) }
    val configuration = remember(localized) {
        Configuration(localized.resources.configuration)
    }
    CompositionLocalProvider(
        LocalResources provides localized.resources,
        LocalConfiguration provides configuration,
        LocalLayoutDirection provides configuration.toLayoutDirection(),
        content = content,
    )
}

private fun Configuration.toLayoutDirection(): LayoutDirection =
    if (layoutDirection == View.LAYOUT_DIRECTION_RTL) LayoutDirection.Rtl else LayoutDirection.Ltr
