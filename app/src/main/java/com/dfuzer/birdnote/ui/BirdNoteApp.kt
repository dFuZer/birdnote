package com.dfuzer.birdnote.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.dfuzer.birdnote.ScoreStore
import com.dfuzer.birdnote.SettingsStore
import com.dfuzer.birdnote.findActivity
import com.dfuzer.birdnote.ui.navigation.BirdNoteNavHost
import com.dfuzer.birdnote.ui.screens.PreferredClefsScreen
import com.dfuzer.birdnote.ui.theme.LightBlue

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
                onAppLanguageChange = { language ->
                    val changed = language != appLanguage
                    settingsStore.setAppLanguage(language)
                    if (changed) context.findActivity().recreate()
                },
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
