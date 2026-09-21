package com.example.birdnote.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = DarkBlue,
    onPrimary = Neutral,
    secondary = DisabledGrey,
    onSecondary = Neutral,
    error = Highlight,
    background = LightBlue,
    onBackground = DarkBlue,
    surface = Neutral,
    onSurface = DarkBlue,
    surfaceVariant = Neutral,
    onSurfaceVariant = DarkBlue,
)

@Composable
fun BirdNoteTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}