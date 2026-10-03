package com.example.birdnote.ui.screens

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.example.birdnote.R
import com.example.birdnote.domain.Clef
import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.ChordConfig
import com.example.birdnote.domain.MAX_DIFFICULTY
import com.example.birdnote.domain.MIN_DIFFICULTY
import com.example.birdnote.domain.PracticeChord
import com.example.birdnote.domain.previewChords
import com.example.birdnote.ui.LayoutTuning
import com.example.birdnote.ui.staff.ChordGridTarget
import com.example.birdnote.ui.staff.StaffCanvas
import com.example.birdnote.ui.staff.StaffRenderModel
import com.example.birdnote.ui.staff.asChord
import com.example.birdnote.ui.staff.chordPreviewStaffScale
import com.example.birdnote.ui.staff.chordTileMotions
import com.example.birdnote.ui.staff.easedTileFrame
import com.example.birdnote.ui.staff.rememberSetupEase
import com.example.birdnote.ui.theme.DarkBlue
import com.example.birdnote.ui.theme.Neutral

@Composable
fun ChordSetupScreen(
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
    val config = ChordConfig(difficulty, clefMode)
    val preview = previewChords(config)

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
            onClick = { onStartClick(difficulty, clefMode) },
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
}
