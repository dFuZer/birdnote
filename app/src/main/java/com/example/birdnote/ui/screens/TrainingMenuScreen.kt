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
fun TrainingMenuScreen(
    onNotesClick: () -> Unit,
    onIntervalsClick: () -> Unit,
    onChordsClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Common
    DecoratedScreen(modifier = modifier) {
        BackButton(
            label = stringResource(R.string.back),
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(tuning.backButtonMargin),
        )
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
                text = stringResource(R.string.training_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = tuning.titleBottomSpacing),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(tuning.menuButtonGap),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                MenuButton(text = stringResource(R.string.notes), onClick = onNotesClick)
                MenuButton(text = stringResource(R.string.intervals), onClick = onIntervalsClick)
                MenuButton(text = stringResource(R.string.chords), onClick = onChordsClick)
            }
        }
    }
}
