package com.dfuzer.birdnote.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.PracticeConfig
import com.dfuzer.birdnote.domain.clefs
import com.dfuzer.birdnote.domain.openingClefMode
import com.dfuzer.birdnote.domain.pitchRange
import com.dfuzer.birdnote.domain.previewNotes
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.components.ClefSelector
import com.dfuzer.birdnote.ui.components.DifficultyControls
import com.dfuzer.birdnote.ui.components.SetupScreen
import com.dfuzer.birdnote.ui.staff.StaffCanvas
import com.dfuzer.birdnote.ui.staff.StaffRenderModel
import com.dfuzer.birdnote.ui.staff.asChords

@Composable
fun NoteSetupScreen(
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
    val config = PracticeConfig(difficulty, clefMode)
    val preview = StaffRenderModel(
        clefMode = clefMode,
        chords = previewNotes(config).asChords(),
        difficulty = difficulty,
        ranges = config.clefMode.clefs().associateWith { pitchRange(difficulty, it) },
    )

    SetupScreen(
        onSave = { onSaveSetup(difficulty, clefMode) },
        onBack = onBackClick,
        onStart = { onStartClick(difficulty, clefMode) },
        modifier = modifier,
        controls = {
            DifficultyControls(difficulty) { difficulty = it }
            ClefSelector(clefMode, clefModes) { clefModeName = it.name }
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
