package com.dfuzer.birdnote

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.dfuzer.birdnote.ui.BirdNoteApp
import com.dfuzer.birdnote.ui.theme.BirdNoteTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withResolvedLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BirdNoteTheme {
                BirdNoteApp()
            }
        }
    }
}
