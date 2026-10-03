package com.example.birdnote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.birdnote.R
import com.example.birdnote.domain.Clef
import com.example.birdnote.ui.LayoutTuning
import com.example.birdnote.ui.theme.DarkBlue

@Composable
fun PreferredClefsScreen(
    selected: Set<Clef>,
    onToggle: (Clef) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Common
    val clefTuning = LayoutTuning.PreferredClefs
    DecoratedScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = tuning.screenHorizontalPadding,
                    vertical = tuning.screenVerticalPadding,
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.preferred_clefs_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = tuning.titleBottomSpacing),
            )
            Text(
                text = stringResource(R.string.preferred_clefs_hint),
                style = MaterialTheme.typography.bodyLarge,
                color = DarkBlue,
                modifier = Modifier.padding(bottom = clefTuning.hintBottomSpacing),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(tuning.menuButtonGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.clefs),
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkBlue,
                )
                PreferredClefToggles(selected = selected, onToggle = onToggle)
                AppButton(
                    text = stringResource(R.string.continue_action),
                    onClick = onContinue,
                )
            }
        }
    }
}
