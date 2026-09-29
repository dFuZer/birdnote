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
import com.example.birdnote.ui.LayoutTuning

@Composable
fun HomeScreen(
    onTrainClick: () -> Unit,
    onScoresClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Common
    DecoratedScreen(modifier = modifier, homePlacement = true) {
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
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = tuning.titleBottomSpacing),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(tuning.menuButtonGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MenuButton(text = stringResource(R.string.train), onClick = onTrainClick)
                MenuButton(text = stringResource(R.string.my_scores), onClick = onScoresClick)
                MenuButton(text = stringResource(R.string.settings), onClick = onSettingsClick)
            }
        }
    }
}

@Composable
internal fun MenuButton(
    text: String,
    onClick: () -> Unit = {},
    enabled: Boolean = true,
) {
    AppButton(
        text = text,
        onClick = onClick,
        enabled = enabled,
    )
}
