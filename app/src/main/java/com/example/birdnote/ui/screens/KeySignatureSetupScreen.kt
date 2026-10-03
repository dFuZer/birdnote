package com.example.birdnote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.example.birdnote.domain.ALL_MAJOR_KEYS
import com.example.birdnote.domain.Clef
import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.KeySignatureConfig
import com.example.birdnote.domain.MAX_DIFFICULTY
import com.example.birdnote.domain.MIN_DIFFICULTY
import com.example.birdnote.domain.MajorKey
import com.example.birdnote.domain.NoteNaming
import com.example.birdnote.domain.label
import com.example.birdnote.domain.majorKeysFor
import com.example.birdnote.domain.previewKeySignatures
import com.example.birdnote.ui.LayoutTuning
import com.example.birdnote.ui.staff.StaffCanvas
import com.example.birdnote.ui.staff.StaffRenderModel
import com.example.birdnote.ui.staff.asChord
import com.example.birdnote.ui.theme.DarkBlue
import com.example.birdnote.ui.theme.Neutral

@Composable
fun KeySignatureSetupScreen(
    noteNaming: NoteNaming,
    onStartClick: (difficulty: Int, clefMode: ClefMode, key: MajorKey?) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.Setup
    var difficulty by rememberSaveable { mutableIntStateOf(MIN_DIFFICULTY) }
    var clefModeName by rememberSaveable { mutableStateOf(ClefMode.SOL.name) }
    var keyName by rememberSaveable { mutableStateOf(ALL_MAJOR_KEYS) }
    var clefMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var keyMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val clefMode = ClefMode.valueOf(clefModeName)
    val available = majorKeysFor(difficulty)
    val selectedKey = available.firstOrNull { it.name == keyName }
    val config = KeySignatureConfig(difficulty, clefMode, selectedKey)
    val preview = previewKeySignatures(config)

    DecoratedScreen(modifier = modifier) {
        BackButton(
            label = stringResource(R.string.back),
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(commonTuning.backButtonMargin),
        )
        Box(
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
                                onClick = {
                                    difficulty = level
                                    val stillAllowed = majorKeysFor(level).any { it.name == keyName }
                                    if (!stillAllowed) keyName = ALL_MAJOR_KEYS
                                },
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
                    Text(
                        text = stringResource(R.string.tonics),
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkBlue,
                    )
                    Box {
                        AppButton(
                            text = selectedKey?.label(noteNaming)
                                ?: stringResource(R.string.all_tonics),
                            onClick = { keyMenuExpanded = true },
                            modifier = Modifier.widthIn(
                                min = tuning.clefButtonMinWidth,
                                max = tuning.clefButtonMaxWidth,
                            ),
                        )
                        DropdownMenu(
                            expanded = keyMenuExpanded,
                            onDismissRequest = { keyMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.all_tonics),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                },
                                onClick = {
                                    keyName = ALL_MAJOR_KEYS
                                    keyMenuExpanded = false
                                },
                            )
                            available.forEach { key ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = key.label(noteNaming),
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                    },
                                    onClick = {
                                        keyName = key.name
                                        keyMenuExpanded = false
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
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(tuning.previewInnerPadding),
                    ) {
                        preview.forEach { signature ->
                            StaffCanvas(
                                model = StaffRenderModel(
                                    clefMode = signature.clef.toKeySignatureMode(),
                                    chords = listOf(signature.asChord()),
                                ),
                                visibleSlotCount = 1,
                                noteAreaExtraLeftPaddingInLineSpaces =
                                    tuning.previewNoteAreaExtraLeftPaddingInLineSpaces,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
        AppButton(
            text = stringResource(R.string.lets_go),
            onClick = { onStartClick(difficulty, clefMode, selectedKey) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = tuning.startButtonBottomMargin),
        )
    }
}

private fun Clef.toKeySignatureMode(): ClefMode = when (this) {
    Clef.SOL -> ClefMode.SOL
    Clef.FA -> ClefMode.FA
}
