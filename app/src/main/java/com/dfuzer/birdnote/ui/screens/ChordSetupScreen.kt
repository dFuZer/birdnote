package com.dfuzer.birdnote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import com.dfuzer.birdnote.domain.ChordConfig
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.PracticeChord
import com.dfuzer.birdnote.domain.clefs
import com.dfuzer.birdnote.domain.openingClefMode
import com.dfuzer.birdnote.domain.previewChords
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.components.ClefSelector
import com.dfuzer.birdnote.ui.components.DifficultyControls
import com.dfuzer.birdnote.ui.components.SetupScreen
import com.dfuzer.birdnote.ui.components.label
import com.dfuzer.birdnote.ui.staff.StaffCanvas
import com.dfuzer.birdnote.ui.staff.StaffRenderModel
import com.dfuzer.birdnote.ui.staff.asChord
import com.dfuzer.birdnote.ui.staff.rememberSetupEase
import com.dfuzer.birdnote.ui.theme.DarkBlue
import kotlin.math.roundToInt

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
    val tuning = LayoutTuning.Setup
    var difficulty by rememberSaveable { mutableIntStateOf(initialDifficulty) }
    var clefModeName by rememberSaveable { mutableStateOf(initialClefMode.name) }
    val clefMode = openingClefMode(
        preferred = clefModes.flatMap { it.clefs() }.toSet(),
        remembered = ClefMode.entries.firstOrNull { it.name == clefModeName },
    )
    val config = ChordConfig(difficulty, clefMode)
    val preview = previewChords(config)

    SetupScreen(
        onSave = { onSaveSetup(difficulty, clefMode) },
        onBack = onBackClick,
        onStart = { onStartClick(difficulty, clefMode) },
        modifier = modifier,
        controls = {
            DifficultyControls(difficulty) { difficulty = it }
            ClefSelector(clefMode, clefModes) { clefModeName = it.name }
        },
    ) { notesSingleStaffHeightPx ->
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
                    val fromHeight = motion.fromSlot?.let {
                        chordGridRect(it, widthPx, heightPx, gapPx).height
                    } ?: frame.height
                    val toHeight = motion.toSlot?.let {
                        chordGridRect(it, widthPx, heightPx, gapPx).height
                    } ?: frame.height
                    key(motion.quality) {
                        ChordPreviewTile(
                            chord = chord,
                            fromDifficulty = motion.fromDifficulty,
                            toDifficulty = motion.toDifficulty,
                            fraction = fraction,
                            fromTileHeightPx = fromHeight,
                            toTileHeightPx = toHeight,
                            tileHeightPx = frame.height,
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

@Composable
private fun ChordPreviewTile(
    chord: PracticeChord,
    fromDifficulty: Int?,
    toDifficulty: Int?,
    fraction: Float,
    fromTileHeightPx: Float,
    toTileHeightPx: Float,
    tileHeightPx: Float,
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
            val staffHeightPx = with(density) { maxHeight.toPx() }
            val staff = easedChordTileStaff(
                fromDifficulty = fromDifficulty,
                toDifficulty = toDifficulty,
                fraction = fraction,
                fromStaffHeightPx = settledStaffHeightPx(
                    fromTileHeightPx,
                    tileHeightPx,
                    staffHeightPx,
                ),
                toStaffHeightPx = settledStaffHeightPx(
                    toTileHeightPx,
                    tileHeightPx,
                    staffHeightPx,
                ),
                staffHeightPx = staffHeightPx,
                notesSingleStaffHeightPx = notesSingleStaffHeightPx,
            )
            StaffCanvas(
                model = StaffRenderModel(
                    clefMode = chord.clef.toMode(),
                    chords = listOf(chord.asChord()),
                ),
                visibleSlotCount = 1,
                compactVertical = true,
                staffScaleOverride = staff.staffScale,
                verticalCenter = staff.verticalCenter,
                animateSetupChanges = false,
                noteAreaExtraLeftPaddingInLineSpaces =
                    tuning.previewNoteAreaExtraLeftPaddingInLineSpaces,
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
