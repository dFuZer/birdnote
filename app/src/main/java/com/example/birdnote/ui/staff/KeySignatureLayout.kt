package com.example.birdnote.ui.staff

import com.example.birdnote.domain.Accidental
import com.example.birdnote.domain.Clef
import com.example.birdnote.domain.KeySignature
import com.example.birdnote.domain.MajorKey
import com.example.birdnote.domain.NOTE_COUNT
import com.example.birdnote.domain.Pitch
import com.example.birdnote.domain.signatureNotes
import com.example.birdnote.domain.staffStep
import com.example.birdnote.ui.LayoutTuning

/**
 * Key-signature column just after the clef.
 *
 * Accidentals use the usual octaves: treble sharps from F5 down through B4,
 * treble flats from B4 through F4, and the same shape two staff positions
 * lower on bass (through F2 for seven flats).
 */
fun layoutKeySignature(
    signature: KeySignature,
    left: Float,
    lineSpacing: Float,
    bottomLineY: Float,
): ChordLayout {
    val steps = keySignatureStaffSteps(signature.key, signature.clef)
    if (steps.isEmpty()) {
        val width = lineSpacing * EMPTY_SIGNATURE_WIDTH_IN_LINE_SPACES
        return ChordLayout(
            heads = emptyList(),
            stem = null,
            accidentals = emptyList(),
            ledgerSteps = emptyList(),
            bounds = ChordBounds(
                left = left,
                right = left + width,
                top = bottomLineY - 4f * lineSpacing,
                bottom = bottomLineY,
            ),
            useCompleteNote = false,
        )
    }
    val accidental = signature.key.signatureAccidental
    val tuning = LayoutTuning.Staff
    val glyphWidth = lineSpacing * when (accidental) {
        Accidental.FLAT -> tuning.flatWidthInLineSpaces
        else -> tuning.sharpWidthInLineSpaces
    }
    val glyphHeight = lineSpacing * when (accidental) {
        Accidental.FLAT -> tuning.flatHeightInLineSpaces
        else -> tuning.sharpHeightInLineSpaces
    }
    val centerYOffset = if (accidental == Accidental.FLAT) {
        lineSpacing * tuning.flatCenterYOffsetInLineSpaces
    } else {
        0f
    }
    val columnPitch = glyphWidth + lineSpacing * tuning.accidentalColumnGapInLineSpaces
    val placements = steps.mapIndexed { index, step ->
        AccidentalPlacement(
            accidental = accidental,
            x = left + glyphWidth / 2f + index * columnPitch,
            step = step,
        )
    }
    val halfWidth = glyphWidth / 2f
    val halfHeight = glyphHeight / 2f
    var top = Float.POSITIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    placements.forEach { placement ->
        val y = yForStep(placement.step.toFloat(), bottomLineY, lineSpacing) + centerYOffset
        top = minOf(top, y - halfHeight)
        bottom = maxOf(bottom, y + halfHeight)
    }
    return ChordLayout(
        heads = emptyList(),
        stem = null,
        accidentals = placements,
        ledgerSteps = emptyList(),
        bounds = ChordBounds(
            left = placements.first().x - halfWidth,
            right = placements.last().x + halfWidth,
            top = top,
            bottom = bottom,
        ),
        useCompleteNote = false,
    )
}

internal fun keySignatureStaffSteps(key: MajorKey, clef: Clef): List<Int> {
    val notes = key.signatureNotes()
    if (notes.isEmpty()) return emptyList()
    val octaves = when (clef) {
        Clef.SOL -> if (key.signatureAccidental == Accidental.FLAT) {
            TREBLE_FLAT_OCTAVES
        } else {
            TREBLE_SHARP_OCTAVES
        }
        Clef.FA -> if (key.signatureAccidental == Accidental.FLAT) {
            BASS_FLAT_OCTAVES
        } else {
            BASS_SHARP_OCTAVES
        }
    }
    return notes.mapIndexed { index, note ->
        Pitch(octaves[index] * NOTE_COUNT + note.ordinal).staffStep(clef)
    }
}

internal fun signatureLeftX(notesStartX: Float, slotWidth: Float, index: Int, lineSpacing: Float): Float =
    notesStartX + index * slotWidth + lineSpacing * SIGNATURE_LEADING_GAP_IN_LINE_SPACES

private const val SIGNATURE_LEADING_GAP_IN_LINE_SPACES = 0.45f
private const val EMPTY_SIGNATURE_WIDTH_IN_LINE_SPACES = 1.5f

// F C G D A E B
private val TREBLE_SHARP_OCTAVES = intArrayOf(5, 5, 5, 5, 4, 5, 4)
private val BASS_SHARP_OCTAVES = intArrayOf(3, 3, 3, 3, 2, 3, 2)

// B E A D G C F
private val TREBLE_FLAT_OCTAVES = intArrayOf(4, 5, 4, 5, 4, 5, 4)
private val BASS_FLAT_OCTAVES = intArrayOf(2, 3, 2, 3, 2, 3, 2)
