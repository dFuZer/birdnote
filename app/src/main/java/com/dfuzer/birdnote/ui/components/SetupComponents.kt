package com.dfuzer.birdnote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.MAX_DIFFICULTY
import com.dfuzer.birdnote.domain.MIN_DIFFICULTY
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.theme.DarkBlue
import com.dfuzer.birdnote.ui.theme.Neutral

/** Save both on explicit exits and when the setup leaves composition. */
@Composable
internal fun SaveSetupOnDispose(onSave: () -> Unit) {
    val save by rememberUpdatedState(onSave)
    DisposableEffect(Unit) { onDispose { save() } }
}

@Composable
internal fun SetupScreen(
    onSave: () -> Unit,
    onBack: () -> Unit,
    onStart: () -> Unit,
    modifier: Modifier,
    controls: @Composable ColumnScope.() -> Unit,
    preview: @Composable (notesSingleStaffHeightPx: Float) -> Unit,
) {
    val tuning = LayoutTuning.Setup
    SaveSetupOnDispose(onSave)
    DecoratedScreen(modifier = modifier) {
        BackButton(
            label = stringResource(R.string.back),
            onClick = {
                onSave()
                onBack()
            },
            modifier = Modifier.align(Alignment.TopStart).padding(LayoutTuning.Common.backButtonMargin),
        )
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(
                start = tuning.contentStartPadding,
                end = tuning.contentEndPadding,
                top = tuning.contentTopPadding,
                bottom = tuning.contentBottomPadding,
            ),
        ) {
            val notesSingleStaffHeightPx = with(LocalDensity.current) {
                (maxHeight - tuning.previewInnerPadding * 2).toPx()
            }
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(tuning.columnsGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(tuning.controlsWeight).widthIn(max = tuning.controlsMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(tuning.controlsGap),
                    content = controls,
                )
                Card(
                    shape = RoundedCornerShape(tuning.previewCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = Neutral),
                    modifier = Modifier.weight(tuning.previewWeight).fillMaxSize(),
                ) { preview(notesSingleStaffHeightPx) }
            }
        }
        AppButton(
            text = stringResource(R.string.lets_go),
            onClick = {
                onSave()
                onStart()
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = tuning.startButtonBottomMargin),
        )
    }
}

@Composable
internal fun DifficultyControls(difficulty: Int, onDifficultyChange: (Int) -> Unit) {
    val tuning = LayoutTuning.Setup
    Text(
        text = stringResource(R.string.difficulty),
        style = MaterialTheme.typography.titleMedium,
        color = DarkBlue,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(tuning.difficultyGap)) {
        (MIN_DIFFICULTY..MAX_DIFFICULTY).forEach { level ->
            DifficultyButton(
                selected = difficulty == level,
                text = level.toString(),
                onClick = { onDifficultyChange(level) },
            )
        }
    }
}

@Composable
internal fun ClefSelector(clefMode: ClefMode, clefModes: List<ClefMode>, onClefChange: (ClefMode) -> Unit) {
    val tuning = LayoutTuning.Setup
    var clefMenuExpanded by rememberSaveable { mutableStateOf(false) }
    Text(
        text = stringResource(R.string.clef),
        style = MaterialTheme.typography.titleMedium,
        color = DarkBlue,
    )
    Box {
        AppButton(
            text = clefMode.label(),
            onClick = { clefMenuExpanded = true },
            modifier = Modifier.widthIn(
                min = tuning.clefButtonMinWidth,
                max = tuning.clefButtonMaxWidth,
            ),
        )
        DropdownMenu(
            expanded = clefMenuExpanded,
            onDismissRequest = { clefMenuExpanded = false },
        ) {
            clefModes.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = mode.label(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = {
                        onClefChange(mode)
                        clefMenuExpanded = false
                    },
                )
            }
        }
    }
}
