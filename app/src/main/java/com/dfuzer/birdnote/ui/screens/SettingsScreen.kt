package com.dfuzer.birdnote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.AppLanguage
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.NoteNaming
import com.dfuzer.birdnote.domain.scalePreview
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.theme.DarkBlue

@Composable
fun SettingsScreen(
    noteNaming: NoteNaming,
    onNoteNamingChange: (NoteNaming) -> Unit,
    appLanguage: AppLanguage,
    onAppLanguageChange: (AppLanguage) -> Unit,
    preferredClefs: Set<Clef>,
    onPreferredClefToggle: (Clef) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Common
    DecoratedScreen(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(maxWidth)
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(
                        horizontal = tuning.screenHorizontalPadding,
                        vertical = tuning.screenVerticalPadding,
                    ),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(tuning.titleBottomSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ChoiceMenu(
                        label = stringResource(R.string.language),
                        selected = appLanguage.label(),
                        options = AppLanguage.entries.map { it.label() },
                        onSelect = { onAppLanguageChange(AppLanguage.entries[it]) },
                    )
                    ChoiceMenu(
                        label = stringResource(R.string.note_names),
                        selected = noteNaming.scalePreview(),
                        options = NoteNaming.entries.map { it.scalePreview() },
                        onSelect = { onNoteNamingChange(NoteNaming.entries[it]) },
                    )
                    ClefMenu(
                        selected = preferredClefs,
                        onToggle = onPreferredClefToggle,
                    )
                }
            }
        }
        BackButton(
            label = stringResource(R.string.back),
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(tuning.backButtonMargin),
        )
    }
}

@Composable
private fun ChoiceMenu(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
) {
    val tuning = LayoutTuning.Common
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(tuning.menuButtonGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = DarkBlue,
        )
        Box {
            AppButton(
                text = selected,
                onClick = { menuExpanded = true },
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(tuning.buttonMaxWidth),
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        onClick = {
                            onSelect(index)
                            menuExpanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ClefMenu(
    selected: Set<Clef>,
    onToggle: (Clef) -> Unit,
) {
    val tuning = LayoutTuning.Common
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    val summary = Clef.entries
        .filter { it in selected }
        .map { it.label() }
        .joinToString(", ")
    Column(
        verticalArrangement = Arrangement.spacedBy(tuning.menuButtonGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.clefs),
            style = MaterialTheme.typography.titleMedium,
            color = DarkBlue,
        )
        Box {
            AppButton(
                text = summary,
                onClick = { menuExpanded = true },
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier.width(tuning.buttonMaxWidth),
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier.width(tuning.buttonMaxWidth),
            ) {
                Clef.entries.forEach { clef ->
                    val on = clef in selected
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = clef.label(),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        },
                        trailingIcon = if (on) {
                            { Text(text = "✓", style = MaterialTheme.typography.bodyLarge) }
                        } else {
                            null
                        },
                        enabled = !on || selected.size > 1,
                        onClick = { onToggle(clef) },
                        modifier = Modifier.semantics { this.selected = on },
                    )
                }
            }
        }
    }
}
