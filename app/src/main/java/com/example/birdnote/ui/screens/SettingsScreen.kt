package com.example.birdnote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.example.birdnote.R
import com.example.birdnote.domain.AppLanguage
import com.example.birdnote.domain.Clef
import com.example.birdnote.domain.NoteNaming
import com.example.birdnote.domain.scalePreview
import com.example.birdnote.ui.LayoutTuning
import com.example.birdnote.ui.theme.DarkBlue

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
                text = stringResource(R.string.settings),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = tuning.titleBottomSpacing),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(tuning.menuButtonGap * 6),
                verticalAlignment = Alignment.Top,
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
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(tuning.menuButtonGap),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = tuning.menuButtonGap),
            ) {
                Text(
                    text = stringResource(R.string.clefs),
                    style = MaterialTheme.typography.titleMedium,
                    color = DarkBlue,
                )
                PreferredClefToggles(
                    selected = preferredClefs,
                    onToggle = onPreferredClefToggle,
                )
            }
        }
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
