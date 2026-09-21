package com.example.birdnote.ui.staff

import com.example.birdnote.domain.Accidental
import com.example.birdnote.domain.StaffNote
import com.example.birdnote.domain.staffStep
import com.example.birdnote.ui.LayoutTuning
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

data class ChordLayoutMetrics(
    val noteWidthInLineSpaces: Float = LayoutTuning.Staff.noteWidthInLineSpaces,
    val noteHeightInLineSpaces: Float = LayoutTuning.Staff.noteHeightInLineSpaces,
    val noteHeadCenterXFraction: Float = LayoutTuning.Staff.noteHeadCenterXFraction,
    val noteHeadCenterYFraction: Float = LayoutTuning.Staff.noteHeadCenterYFraction,
    val stemDownFromStaffStep: Int = LayoutTuning.Staff.stemDownFromStaffStep,
    val stemThicknessInNoteWidths: Float = LayoutTuning.Staff.stemThicknessInNoteWidths,
    val ledgerHalfWidthInLineSpaces: Float = LayoutTuning.Staff.ledgerHalfWidthInLineSpaces,
    val accidentalWidthInLineSpaces: Float = LayoutTuning.Staff.accidentalMaxWidthInLineSpaces,
    val accidentalHeightInLineSpaces: Float = LayoutTuning.Staff.accidentalMaxHeightInLineSpaces,
    val accidentalColumnGapInLineSpaces: Float =
        LayoutTuning.Staff.accidentalColumnGapInLineSpaces,
    val accidentalToHeadGapInLineSpaces: Float =
        LayoutTuning.Staff.accidentalToHeadGapInLineSpaces,
    val accidentalSameColumnMinSteps: Int =
        LayoutTuning.Staff.accidentalSameColumnMinSteps,
    val accidentalFlatExtraSteps: Int = LayoutTuning.Staff.accidentalFlatExtraSteps,
)

data class HeadPlacement(
    val note: StaffNote,
    val x: Float,
    val step: Int,
    val stemDown: Boolean,
)

data class AccidentalPlacement(
    val accidental: Accidental,
    val x: Float,
    val step: Int,
)

data class StemPlacement(
    val x: Float,
    val startY: Float,
    val endY: Float,
)

data class ChordBounds(
    val left: Float,
    val right: Float,
    val top: Float,
    val bottom: Float,
)

data class ChordLayout(
    val heads: List<HeadPlacement>,
    val stem: StemPlacement?,
    val accidentals: List<AccidentalPlacement>,
    val ledgerSteps: List<Int>,
    val bounds: ChordBounds,
    val useCompleteNote: Boolean,
)

fun layoutChord(
    notes: List<StaffNote>,
    x: Float,
    lineSpacing: Float,
    bottomLineY: Float,
    metrics: ChordLayoutMetrics = ChordLayoutMetrics(),
): ChordLayout {
    val sorted = notes.sortedBy { it.pitch.diatonicStep }
    if (sorted.isEmpty()) {
        return ChordLayout(
            heads = emptyList(),
            stem = null,
            accidentals = emptyList(),
            ledgerSteps = emptyList(),
            bounds = ChordBounds(x, x, bottomLineY, bottomLineY),
            useCompleteNote = true,
        )
    }
    val heads = placeHeads(sorted, x, lineSpacing, metrics)
    val useCompleteNote = heads.size == 1
    val stem = if (useCompleteNote) {
        null
    } else {
        stemPlacement(heads, x, hasAnySecond(sorted), bottomLineY, lineSpacing, metrics)
    }
    val accidentals = placeAccidentals(heads, lineSpacing, metrics)
    val ledgerSteps = heads.flatMap { ledgerSteps(it.step) }.distinct().sorted()
    val bounds = chordBounds(heads, stem, accidentals, lineSpacing, bottomLineY, metrics)
    return ChordLayout(
        heads = heads,
        stem = stem,
        accidentals = accidentals,
        ledgerSteps = ledgerSteps,
        bounds = bounds,
        useCompleteNote = useCompleteNote,
    )
}

fun yForStep(step: Float, bottomLineY: Float, lineSpacing: Float): Float =
    bottomLineY - step * (lineSpacing / 2f)

/**
 * Horizontal distance from a note-head center to the stem center.
 *
 * The stem sits on the tilted oval's edge at staff-line height, tucked inward
 * by half the stem thickness so the stroke overlaps the head instead of
 * leaving a gap beside the dest rect.
 */
fun stemToHeadOffset(lineSpacing: Float, metrics: ChordLayoutMetrics = ChordLayoutMetrics()): Float {
    val headWidth = lineSpacing * metrics.noteWidthInLineSpaces
    val stemHalf = headWidth * metrics.stemThicknessInNoteWidths / 2f
    return noteHeadVisualHalfWidth(headWidth) - stemHalf
}

fun noteHeadVisualHalfWidth(headWidth: Float): Float {
    val tuning = LayoutTuning.Staff
    val rx = headWidth * tuning.noteHeadEllipseRx / tuning.noteHeadSvgWidth
    val ry = headWidth * tuning.noteHeadEllipseRy / tuning.noteHeadSvgWidth
    val theta = Math.toRadians(tuning.noteHeadRotationDegrees.toDouble())
    val cos = cos(theta)
    val sin = sin(theta)
    return (1.0 / sqrt(cos * cos / (rx * rx) + sin * sin / (ry * ry))).toFloat()
}

fun ledgerSteps(step: Int): List<Int> = when {
    step <= -2 -> (step..-2).filter { it % 2 == 0 }
    step >= 10 -> (10..step).filter { it % 2 == 0 }
    else -> emptyList()
}

private fun placeHeads(
    notes: List<StaffNote>,
    stemX: Float,
    lineSpacing: Float,
    metrics: ChordLayoutMetrics,
): List<HeadPlacement> {
    val lowestStep = notes.minOf { it.pitch.staffStep(it.clef) }
    val stemDown = lowestStep >= metrics.stemDownFromStaffStep
    val secondOffset = stemToHeadOffset(lineSpacing, metrics)
    if (!hasAnySecond(notes)) {
        return notes.map { note ->
            HeadPlacement(
                note = note,
                x = stemX,
                step = note.pitch.staffStep(note.clef),
                stemDown = stemDown,
            )
        }
    }
    var previousStep: Int? = null
    var previousColumn = 0
    return notes.map { note ->
        val step = note.pitch.staffStep(note.clef)
        val column = if (previousStep != null && step - previousStep!! == 1) {
            1 - previousColumn
        } else {
            0
        }
        previousColumn = column
        previousStep = step
        val x = if (hasSecondNeighbor(notes, step)) {
            val mainOnLeft = !stemDown
            val onMain = column == 0
            if (onMain == mainOnLeft) stemX - secondOffset else stemX + secondOffset
        } else {
            stemX
        }
        HeadPlacement(note = note, x = x, step = step, stemDown = stemDown)
    }
}

private fun hasAnySecond(notes: List<StaffNote>): Boolean {
    val steps = notes.map { it.pitch.staffStep(it.clef) }.sorted()
    return steps.zipWithNext().any { (lower, upper) -> upper - lower == 1 }
}

private fun hasSecondNeighbor(notes: List<StaffNote>, step: Int): Boolean =
    notes.any { abs(it.pitch.staffStep(it.clef) - step) == 1 }

private fun stemPlacement(
    heads: List<HeadPlacement>,
    slotX: Float,
    hasSecond: Boolean,
    bottomLineY: Float,
    lineSpacing: Float,
    metrics: ChordLayoutMetrics,
): StemPlacement {
    val stemDown = heads.first().stemDown
    val lowerY = yForStep(heads.minOf { it.step }.toFloat(), bottomLineY, lineSpacing)
    val upperY = yForStep(heads.maxOf { it.step }.toFloat(), bottomLineY, lineSpacing)
    val stemLength = lineSpacing * metrics.noteHeightInLineSpaces * metrics.noteHeadCenterYFraction
    val side = stemToHeadOffset(lineSpacing, metrics)
    val stemX = when {
        hasSecond -> slotX
        stemDown -> slotX - side
        else -> slotX + side
    }
    return if (stemDown) {
        StemPlacement(x = stemX, startY = upperY, endY = lowerY + stemLength)
    } else {
        StemPlacement(x = stemX, startY = lowerY, endY = upperY - stemLength)
    }
}

private fun placeAccidentals(
    heads: List<HeadPlacement>,
    lineSpacing: Float,
    metrics: ChordLayoutMetrics,
): List<AccidentalPlacement> {
    val written = heads
        .filter { it.note.accidental != Accidental.NONE }
        .sortedByDescending { it.step }
    if (written.isEmpty()) return emptyList()
    val noteWidth = lineSpacing * metrics.noteWidthInLineSpaces
    val leftmostHeadLeft = heads.minOf { head ->
        head.x - noteWidth * metrics.noteHeadCenterXFraction
    }
    val columnWidth = lineSpacing *
        (metrics.accidentalWidthInLineSpaces + metrics.accidentalColumnGapInLineSpaces)
    val gap = lineSpacing * metrics.accidentalToHeadGapInLineSpaces
    val columns = mutableListOf<MutableList<HeadPlacement>>()
    return written.map { head ->
        var column = 0
        while (column < columns.size && columns[column].any { other ->
                accidentalsCollide(head, other, metrics)
            }
        ) {
            column++
        }
        if (column == columns.size) {
            columns.add(mutableListOf())
        }
        columns[column].add(head)
        val width = lineSpacing * metrics.accidentalWidthInLineSpaces
        val x = leftmostHeadLeft - gap - column * columnWidth - width / 2f
        AccidentalPlacement(accidental = head.note.accidental, x = x, step = head.step)
    }
}

internal fun accidentalsCollide(
    a: HeadPlacement,
    b: HeadPlacement,
    metrics: ChordLayoutMetrics = ChordLayoutMetrics(),
): Boolean = accidentalsCollide(a.step, a.note.accidental, b.step, b.note.accidental, metrics)

internal fun accidentalsCollide(
    stepA: Int,
    accidentalA: Accidental,
    stepB: Int,
    accidentalB: Accidental,
    metrics: ChordLayoutMetrics = ChordLayoutMetrics(),
): Boolean {
    val extra = if (accidentalA == Accidental.FLAT || accidentalB == Accidental.FLAT) {
        metrics.accidentalFlatExtraSteps
    } else {
        0
    }
    return abs(stepA - stepB) < metrics.accidentalSameColumnMinSteps + extra
}

private fun chordBounds(
    heads: List<HeadPlacement>,
    stem: StemPlacement?,
    accidentals: List<AccidentalPlacement>,
    lineSpacing: Float,
    bottomLineY: Float,
    metrics: ChordLayoutMetrics,
): ChordBounds {
    val noteWidth = lineSpacing * metrics.noteWidthInLineSpaces
    val noteHeight = lineSpacing * metrics.noteHeightInLineSpaces
    val ledgerHalf = lineSpacing * metrics.ledgerHalfWidthInLineSpaces
    val accidentalHalf = lineSpacing * metrics.accidentalWidthInLineSpaces / 2f
    val accidentalHalfHeight = lineSpacing * metrics.accidentalHeightInLineSpaces / 2f
    var left = Float.POSITIVE_INFINITY
    var right = Float.NEGATIVE_INFINITY
    var top = Float.POSITIVE_INFINITY
    var bottom = Float.NEGATIVE_INFINITY
    heads.forEach { head ->
        val y = yForStep(head.step.toFloat(), bottomLineY, lineSpacing)
        val halfWidth = max(
            noteWidth * metrics.noteHeadCenterXFraction,
            if (ledgerSteps(head.step).isNotEmpty()) ledgerHalf else 0f,
        )
        left = min(left, head.x - halfWidth)
        right = max(right, head.x + halfWidth)
        if (stem == null) {
            val above = if (head.stemDown) {
                noteHeight * (1f - metrics.noteHeadCenterYFraction)
            } else {
                noteHeight * metrics.noteHeadCenterYFraction
            }
            val below = if (head.stemDown) {
                noteHeight * metrics.noteHeadCenterYFraction
            } else {
                noteHeight * (1f - metrics.noteHeadCenterYFraction)
            }
            top = min(top, y - above)
            bottom = max(bottom, y + below)
        } else {
            val headHalfHeight = noteWidth * (7f / 8f) / 2f
            top = min(top, y - headHalfHeight)
            bottom = max(bottom, y + headHalfHeight)
        }
    }
    if (stem != null) {
        top = min(top, min(stem.startY, stem.endY))
        bottom = max(bottom, max(stem.startY, stem.endY))
        val stemHalf = noteWidth * 0.04f
        left = min(left, stem.x - stemHalf)
        right = max(right, stem.x + stemHalf)
    }
    accidentals.forEach { accidental ->
        val y = yForStep(accidental.step.toFloat(), bottomLineY, lineSpacing)
        left = min(left, accidental.x - accidentalHalf)
        right = max(right, accidental.x + accidentalHalf)
        top = min(top, y - accidentalHalfHeight)
        bottom = max(bottom, y + accidentalHalfHeight)
    }
    if (left == Float.POSITIVE_INFINITY) {
        left = 0f
        right = 0f
        top = bottomLineY
        bottom = bottomLineY
    }
    return ChordBounds(left = left, right = right, top = top, bottom = bottom)
}
