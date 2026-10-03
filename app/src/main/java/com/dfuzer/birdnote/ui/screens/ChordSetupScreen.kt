package com.dfuzer.birdnote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.ChordConfig
import com.dfuzer.birdnote.domain.MAX_DIFFICULTY
import com.dfuzer.birdnote.domain.MIN_DIFFICULTY
import com.dfuzer.birdnote.domain.PracticeChord
import com.dfuzer.birdnote.domain.clefs
import com.dfuzer.birdnote.domain.openingClefMode
import com.dfuzer.birdnote.domain.previewChords
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.staff.ChordGridTarget
import com.dfuzer.birdnote.ui.staff.StaffCanvas
import com.dfuzer.birdnote.ui.staff.StaffRenderModel
import com.dfuzer.birdnote.ui.staff.asChord
import com.dfuzer.birdnote.ui.staff.chordPreviewStaffScale
import com.dfuzer.birdnote.ui.staff.chordTileMotions
import com.dfuzer.birdnote.ui.staff.easedTileFrame
import com.dfuzer.birdnote.ui.staff.rememberSetupEase
import com.dfuzer.birdnote.ui.theme.DarkBlue
import com.dfuzer.birdnote.ui.theme.Neutral

@Composable
fun ChordSetupScreen(
    initialDifficulty: Int,
    initialClefMode: ClefMode,
    onSaveSetup: (difficulty: Int, clefMode: ClefMode) -> Unit,
    onStartClick: (difficulty: Int, clefMode: ClefMode) -> Unit,
    onBackClick: () -> Unit,
    clefModes: List<ClefMode>,
    modifier: Modifier = Modifier,
) {
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.Setup
    var difficulty by rememberSaveable { mutableIntStateOf(initialDifficulty) }
    var clefModeName by rememberSaveable { mutableStateOf(initialClefMode.name) }
    var clefMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val clefMode = openingClefMode(
        preferred = clefModes.flatMap { it.clefs() }.toSet(),
        remembered = ClefMode.entries.firstOrNull { it.name == clefModeName },
    )
    val saveSetup by rememberUpdatedState(onSaveSetup)
    val difficultyToSave by rememberUpdatedState(difficulty)
    val clefToSave by rememberUpdatedState(clefMode)
    DisposableEffect(Unit) {
        onDispose { saveSetup(difficultyToSave, clefToSave) }
    }
    val config = ChordConfig(difficulty, clefMode)
    val preview = previewChords(config)

    DecoratedScreen(modifier = modifier) {
        BackButton(
            label = stringResource(R.string.back),
            onClick = {
                onSaveSetup(difficulty, clefMode)
                onBackClick()
            },
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
            val notesSingleStaffHeightPx = with(LocalDensity.current) {
                (maxHeight - tuning.previewInnerPadding * 2).toPx()
            }
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
                            clefModes.forEach { mode ->
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
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(tuning.previewInnerPadding),
                    ) {
                        val ease = rememberSetupEase(ChordGridTarget(preview, difficulty))
                        val motions = chordTileMotions(
                            fromChords = ease.from.chords,
                            fromDifficulty = ease.from.difficulty,
                            toChords = ease.to.chords,
                            toDifficulty = ease.to.difficulty,
                        )
                        val density = LocalDensity.current
                        val widthPx = constraints.maxWidth.toFloat()
                        val heightPx = constraints.maxHeight.toFloat()
                        val gapPx = with(density) { tuning.chordPreviewTileGap.toPx() }
                        val fraction = ease.fraction
                        Box(Modifier.fillMaxSize()) {
                            motions.forEach { motion ->
                                val frame = easedTileFrame(
                                    motion = motion,
                                    fraction = fraction,
                                    width = widthPx,
                                    height = heightPx,
                                    gap = gapPx,
                                )
                                if (frame.alpha <= 0f) return@forEach
                                val chord = motion.toChord ?: motion.fromChord ?: return@forEach
                                val tileDifficulty = motion.toDifficulty ?: motion.fromDifficulty ?: difficulty
                                key(motion.quality) {
                                    ChordPreviewTile(
                                        chord = chord,
                                        difficulty = tileDifficulty,
                                        notesSingleStaffHeightPx = notesSingleStaffHeightPx,
                                        modifier = Modifier
                                            .offset {
                                                IntOffset(
                                                    frame.left.roundToInt(),
                                                    frame.top.roundToInt(),
                                                )
                                            }
                                            .requiredSize(
                                                with(density) { frame.width.toDp() },
                                                with(density) { frame.height.toDp() },
                                            )
                                            .graphicsLayer {
                                                alpha = frame.alpha
                                                scaleX = frame.scale
                                                scaleY = frame.scale
                                            },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        AppButton(
            text = stringResource(R.string.lets_go),
            onClick = {
                onSaveSetup(difficulty, clefMode)
                onStartClick(difficulty, clefMode)
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = tuning.startButtonBottomMargin),
        )
    }
}

@Composable
private fun ChordPreviewTile(
    chord: PracticeChord,
    difficulty: Int,
    notesSingleStaffHeightPx: Float,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Setup
    val density = LocalDensity.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tuning.chordPreviewLabelGap),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val tileHeightPx = with(density) { maxHeight.toPx() }
            StaffCanvas(
                model = StaffRenderModel(
                    clefMode = chord.clef.toMode(),
                    chords = listOf(chord.asChord()),
                ),
                visibleSlotCount = 1,
                compactVertical = true,
                staffScaleOverride = chordPreviewStaffScale(
                    difficulty = difficulty,
                    tileHeightPx = tileHeightPx,
                    notesSingleStaffHeightPx = notesSingleStaffHeightPx,
                ),
                centerVertically = difficulty == MIN_DIFFICULTY,
                noteAreaExtraLeftPaddingInLineSpaces =
                    tuning.previewNoteAreaExtraLeftPaddingInLineSpaces,
                easeChanges = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = chord.quality.label(),
            style = MaterialTheme.typography.labelSmall,
            color = DarkBlue,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun Clef.toMode(): ClefMode = when (this) {
    Clef.SOL -> ClefMode.SOL
    Clef.FA -> ClefMode.FA
    Clef.ALTO -> ClefMode.ALTO
    Clef.TENOR -> ClefMode.TENOR
}
