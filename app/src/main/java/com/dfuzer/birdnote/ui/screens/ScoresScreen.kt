package com.dfuzer.birdnote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.MAX_DIFFICULTY
import com.dfuzer.birdnote.domain.MIN_DIFFICULTY
import com.dfuzer.birdnote.domain.PracticeMode
import com.dfuzer.birdnote.domain.bestScore
import com.dfuzer.birdnote.domain.scoreGrid
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.components.BackButton
import com.dfuzer.birdnote.ui.components.DecoratedScreen
import com.dfuzer.birdnote.ui.components.label
import com.dfuzer.birdnote.ui.theme.DarkBlue
import com.dfuzer.birdnote.ui.theme.DisabledGrey
import com.dfuzer.birdnote.ui.theme.Highlight
import com.dfuzer.birdnote.ui.theme.Neutral

@Composable
fun ScoresScreen(
    scores: Map<String, Int>,
    onBackClick: () -> Unit,
    clefModes: List<ClefMode>,
    modifier: Modifier = Modifier,
) {
    val common = LayoutTuning.Common
    val tuning = LayoutTuning.Scores
    var modeName by rememberSaveable { mutableStateOf(PracticeMode.NOTES.name) }
    val mode = PracticeMode.valueOf(modeName)
    DecoratedScreen(modifier = modifier) {
        BackButton(
            label = stringResource(R.string.back),
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(common.backButtonMargin),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = common.screenHorizontalPadding,
                    vertical = common.screenVerticalPadding,
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.my_scores),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = tuning.titleBottomSpacing),
            )
            ModeSwitcher(
                selected = mode,
                onSelect = { modeName = it.name },
                modifier = Modifier
                    .panelWidth()
                    .padding(bottom = tuning.sectionGap),
            )
            ScoresTable(
                mode = mode,
                scores = scores,
                clefModes = clefModes,
                modifier = Modifier.panelWidth(),
            )
        }
    }
}

@Composable
private fun ModeSwitcher(
    selected: PracticeMode,
    onSelect: (PracticeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Scores
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(tuning.modeButtonGap),
    ) {
        PracticeMode.entries.forEach { mode ->
            ModeButton(
                text = mode.label(),
                selected = mode == selected,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Common
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(tuning.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Highlight else DarkBlue,
            contentColor = Neutral,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = tuning.buttonElevation),
        contentPadding = PaddingValues(horizontal = 8.dp),
        modifier = modifier
            .semantics { this.selected = selected }
            .height(tuning.buttonHeight),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun ScoresTable(
    mode: PracticeMode,
    scores: Map<String, Int>,
    clefModes: List<ClefMode>,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Scores
    val difficulties = (MIN_DIFFICULTY..MAX_DIFFICULTY).toList()
    val rows = scoreGrid(mode, clefModes).groupBy { it.clefMode }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(tuning.tableCornerRadius),
        colors = CardDefaults.cardColors(containerColor = Neutral),
        elevation = CardDefaults.cardElevation(defaultElevation = LayoutTuning.Common.buttonElevation),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(tuning.tablePadding),
        ) {
            DifficultyCaption()
            TableRow(
                label = if (mode == PracticeMode.INTERVALS) "" else stringResource(R.string.clef),
                cells = difficulties.map { level ->
                    TableCell(level.toString(), header = true)
                },
            )
            rows.forEach { (clef, cells) ->
                HorizontalDivider(color = DarkBlue.copy(alpha = 0.2f))
                TableRow(
                    label = clef?.label() ?: stringResource(R.string.score_best),
                    cells = cells.sortedBy { it.difficulty }.map { cell ->
                        val score = bestScore(scores, mode, cell.difficulty, cell.clefMode)
                        TableCell(
                            text = score?.toString() ?: stringResource(R.string.score_empty),
                            recorded = score != null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun DifficultyCaption() {
    val tuning = LayoutTuning.Scores
    val columnCount = MAX_DIFFICULTY - MIN_DIFFICULTY + 1
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(tuning.clefColumnWeight))
        Text(
            text = stringResource(R.string.difficulty),
            modifier = Modifier.weight(tuning.scoreColumnWeight * columnCount),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            color = DarkBlue,
        )
    }
}

@Composable
private fun TableRow(
    label: String,
    cells: List<TableCell>,
) {
    val tuning = LayoutTuning.Scores
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = tuning.rowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(tuning.clefColumnWeight)
                .padding(end = tuning.labelEndPadding),
            style = MaterialTheme.typography.titleMedium,
            color = DarkBlue,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        cells.forEach { cell ->
            Text(
                text = cell.text,
                modifier = Modifier.weight(tuning.scoreColumnWeight),
                textAlign = TextAlign.Center,
                style = if (cell.header) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodyLarge
                },
                color = when {
                    cell.header -> DarkBlue
                    cell.recorded -> Highlight
                    else -> DisabledGrey
                },
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PracticeMode.label(): String = when (this) {
    PracticeMode.NOTES -> stringResource(R.string.notes)
    PracticeMode.INTERVALS -> stringResource(R.string.intervals)
    PracticeMode.CHORDS -> stringResource(R.string.chords)
}

private fun Modifier.panelWidth(): Modifier {
    val tuning = LayoutTuning.Scores
    return this
        .widthIn(max = tuning.tableMaxWidth)
        .fillMaxWidth()
}

private data class TableCell(
    val text: String,
    val header: Boolean = false,
    val recorded: Boolean = false,
)
