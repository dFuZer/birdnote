package com.example.birdnote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.birdnote.R
import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.MAX_DIFFICULTY
import com.example.birdnote.domain.MIN_DIFFICULTY
import com.example.birdnote.domain.PracticeConfig
import com.example.birdnote.domain.clefs
import com.example.birdnote.domain.pitchRange
import com.example.birdnote.domain.previewNotes
import com.example.birdnote.ui.LayoutTuning
import com.example.birdnote.ui.staff.StaffCanvas
import com.example.birdnote.ui.staff.StaffRenderModel
import com.example.birdnote.ui.staff.asChords
import com.example.birdnote.ui.theme.DarkBlue
import com.example.birdnote.ui.theme.Neutral

@Composable
fun NoteSetupScreen(
    onStartClick: (difficulty: Int, clefMode: ClefMode) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.Setup
    var difficulty by rememberSaveable { mutableIntStateOf(MIN_DIFFICULTY) }
    var clefModeName by rememberSaveable { mutableStateOf(ClefMode.SOL.name) }
    var clefMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val clefMode = ClefMode.valueOf(clefModeName)
    val config = PracticeConfig(difficulty, clefMode)
    val preview = StaffRenderModel(
        clefMode = clefMode,
        chords = previewNotes(config).asChords(),
        difficulty = difficulty,
        ranges = config.clefMode.clefs().associateWith { pitchRange(difficulty, it) },
    )

    DecoratedScreen(modifier = modifier) {
        BackButton(
            label = stringResource(R.string.back),
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(commonTuning.backButtonMargin),
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = tuning.contentStartPadding,
                    end = tuning.contentEndPadding,
                    top = tuning.contentTopPadding,
                    bottom = tuning.contentBottomPadding,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(tuning.columnsGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(tuning.controlsWeight)
                        .widthIn(max = tuning.controlsMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(tuning.controlsGap),
                ) {
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
                                onClick = { difficulty = level },
                            )
                        }
                    }
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
                            ClefMode.entries.forEach { mode ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = mode.label(),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    },
                                    onClick = {
                                        clefModeName = mode.name
                                        clefMenuExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                Card(
                    shape = RoundedCornerShape(tuning.previewCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = Neutral),
                    modifier = Modifier
                        .weight(tuning.previewWeight)
                        .fillMaxSize(),
                ) {
                    StaffCanvas(
                        model = preview,
                        noteAreaExtraLeftPaddingInLineSpaces =
                            tuning.previewNoteAreaExtraLeftPaddingInLineSpaces,
                        easeChanges = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(tuning.previewInnerPadding),
                    )
                }
            }
        }
        AppButton(
            text = stringResource(R.string.lets_go),
            onClick = { onStartClick(difficulty, clefMode) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = tuning.startButtonBottomMargin),
        )
    }
}
