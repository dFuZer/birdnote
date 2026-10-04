package com.dfuzer.birdnote.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.IntervalConfig
import com.dfuzer.birdnote.domain.intervalPitchRange
import com.dfuzer.birdnote.domain.previewIntervals
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.components.DifficultyControls
import com.dfuzer.birdnote.ui.components.SetupScreen
import com.dfuzer.birdnote.ui.staff.StaffCanvas
import com.dfuzer.birdnote.ui.staff.StaffRenderModel
import com.dfuzer.birdnote.ui.staff.asChord

@Composable
fun IntervalSetupScreen(
    initialDifficulty: Int,
    onSaveSetup: (difficulty: Int) -> Unit,
    onStartClick: (difficulty: Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Setup
    var difficulty by rememberSaveable { mutableIntStateOf(initialDifficulty) }
    val config = IntervalConfig(difficulty)
    val preview = StaffRenderModel(
        clefMode = ClefMode.SOL,
        chords = previewIntervals(config).map { it.asChord() },
        difficulty = difficulty,
        ranges = mapOf(Clef.SOL to intervalPitchRange()),
    )

    SetupScreen(
        onSave = { onSaveSetup(difficulty) },
        onBack = onBackClick,
        onStart = { onStartClick(difficulty) },
        modifier = modifier,
        controls = {
            DifficultyControls(difficulty) { difficulty = it }
        },
    ) {
        StaffCanvas(
            model = preview,
            noteAreaExtraLeftPaddingInLineSpaces =
                tuning.previewNoteAreaExtraLeftPaddingInLineSpaces,
            modifier = Modifier
                .fillMaxSize()
                .padding(tuning.previewInnerPadding),
        )
    }
}
