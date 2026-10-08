package com.dfuzer.birdnote.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.Accidental
import com.dfuzer.birdnote.domain.StaffNote
import com.dfuzer.birdnote.domain.staffStep
import com.dfuzer.birdnote.ui.components.label

/** Describe the notation position, without supplying the answer to the drill. */
@Composable
internal fun staffDescription(notes: List<StaffNote>): String = notes.map { describeNote(it) }.joinToString("; ")

@Composable
private fun describeNote(note: StaffNote): String {
    val step = note.pitch.staffStep(note.clef)
    val position = when {
        step < 0 -> stringResource(R.string.staff_below, -step)
        step > 8 -> stringResource(R.string.staff_above, step - 8)
        step % 2 == 0 -> stringResource(R.string.staff_line, step / 2 + 1)
        else -> stringResource(R.string.staff_space, (step + 1) / 2)
    }
    val accidental = when (note.accidental) {
        Accidental.NONE -> ""
        Accidental.SHARP -> stringResource(R.string.accidental_sharp)
        Accidental.FLAT -> stringResource(R.string.accidental_flat)
        Accidental.NATURAL -> stringResource(R.string.accidental_natural)
    }
    return listOf(note.clef.label(), position, accidental).filter { it.isNotEmpty() }.joinToString(", ")
}
