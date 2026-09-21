package com.example.birdnote.ui.staff

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.example.birdnote.domain.Accidental
import com.example.birdnote.domain.Clef
import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.MAX_DIFFICULTY
import com.example.birdnote.domain.Pitch
import com.example.birdnote.domain.PracticeChord
import com.example.birdnote.domain.StaffInterval
import com.example.birdnote.domain.StaffNote
import com.example.birdnote.domain.staffStep
import com.example.birdnote.ui.LayoutTuning
import com.example.birdnote.ui.theme.LightBlue
import com.example.birdnote.ui.theme.NoteZoneHighlight

data class StaffChord(
    val notes: List<StaffNote>,
)

fun StaffNote.asChord(): StaffChord = StaffChord(listOf(this))

fun List<StaffNote>.asChords(): List<StaffChord> = map { it.asChord() }

fun StaffInterval.asChord(): StaffChord = StaffChord(notes)

fun PracticeChord.asChord(): StaffChord = StaffChord(notes)

data class StaffRenderModel(
    val clefMode: ClefMode,
    val chords: List<StaffChord>,
    val difficulty: Int? = null,
    val highlightIndex: Int? = null,
    val ranges: Map<Clef, IntRange> = emptyMap(),
)

@Composable
fun StaffCanvas(
    model: StaffRenderModel,
    modifier: Modifier = Modifier,
    slotShift: Float = 0f,
    visibleSlotCount: Int? = null,
    noteAreaExtraLeftPaddingInLineSpaces: Float = 0f,
    extendStaffLinesToEnd: Boolean = false,
    compactVertical: Boolean = false,
) {
    val staffColor = Color.Black
    val highlightColor = Color.Black
    val rangeColor = NoteZoneHighlight
    val trebleClef = rememberStaffSvgPainter("key-sol.svg", width = 38, height = 109)
    val bassClef = rememberStaffSvgPainter("key-fa.svg", width = 49, height = 57)
    val notePainter = rememberStaffSvgPainter("note.svg", width = 20, height = 64)
    val sharpPainter = rememberStaffSvgPainter(
        LayoutTuning.Staff.sharpAsset,
        width = LayoutTuning.Staff.accidentalRasterWidth,
        height = LayoutTuning.Staff.accidentalRasterHeight,
    )
    val flatPainter = rememberStaffSvgPainter(
        LayoutTuning.Staff.flatAsset,
        width = LayoutTuning.Staff.accidentalRasterWidth,
        height = LayoutTuning.Staff.accidentalRasterHeight,
    )
    val naturalPainter = rememberStaffSvgPainter(
        LayoutTuning.Staff.naturalAsset,
        width = LayoutTuning.Staff.accidentalRasterWidth,
        height = LayoutTuning.Staff.accidentalRasterHeight,
    )
    Canvas(modifier = modifier) {
        drawScore(
            model = model,
            slotShift = slotShift,
            staffColor = staffColor,
            staffLineColor = Color.Black,
            highlightColor = highlightColor,
            rangeColor = rangeColor,
            trebleClef = trebleClef,
            bassClef = bassClef,
            notePainter = notePainter,
            sharpPainter = sharpPainter,
            flatPainter = flatPainter,
            naturalPainter = naturalPainter,
            visibleSlotCount = visibleSlotCount,
            noteAreaExtraLeftPaddingInLineSpaces = noteAreaExtraLeftPaddingInLineSpaces,
            extendStaffLinesToEnd = extendStaffLinesToEnd,
            compactVertical = compactVertical,
        )
    }
}

@Composable
private fun rememberStaffSvgPainter(
    assetName: String,
    width: Int,
    height: Int,
): Painter {
    val context = LocalContext.current
    val scale = LayoutTuning.Staff.canvasSvgRasterScale
    val request = remember(context, assetName, width, height, scale) {
        ImageRequest.Builder(context)
            .data("file:///android_asset/$assetName")
            .size(width * scale, height * scale)
            .build()
    }
    return rememberAsyncImagePainter(request)
}

private const val STAFF_STEPS = 8
private const val MAX_LEDGER_STEPS = 4

private fun DrawScope.drawScore(
    model: StaffRenderModel,
    slotShift: Float,
    staffColor: Color,
    staffLineColor: Color,
    highlightColor: Color,
    rangeColor: Color,
    trebleClef: Painter,
    bassClef: Painter,
    notePainter: Painter,
    sharpPainter: Painter,
    flatPainter: Painter,
    naturalPainter: Painter,
    visibleSlotCount: Int?,
    noteAreaExtraLeftPaddingInLineSpaces: Float,
    extendStaffLinesToEnd: Boolean,
    compactVertical: Boolean,
) {
    val clefs = when (model.clefMode) {
        ClefMode.SOL -> listOf(Clef.SOL)
        ClefMode.FA -> listOf(Clef.FA)
        ClefMode.SOL_FA -> listOf(Clef.SOL, Clef.FA)
    }
    val tuning = LayoutTuning.Staff
    val paddingX = size.width * tuning.horizontalPadding
    val paddingY = size.height * if (compactVertical) {
        tuning.compactVerticalPadding
    } else {
        tuning.verticalPadding
    }
    val gap = if (clefs.size == 2) size.height * tuning.doubleStaffGap else 0f
    val staffLineWidth = if (clefs.size == 1) {
        tuning.singleStaffLineWidthPx
    } else {
        tuning.staffLineWidthPx
    }
    val available = size.height - paddingY * 2 - gap
    val blockHeight = available / clefs.size
    val ledgerAbove = if (compactVertical) tuning.compactLedgerStepsAbove else MAX_LEDGER_STEPS
    val ledgerBelow = if (compactVertical) tuning.compactLedgerStepsBelow else MAX_LEDGER_STEPS
    val totalSteps = STAFF_STEPS + ledgerAbove + ledgerBelow
    val singleStaffLineSpacing =
        ((size.height - paddingY * 2) / totalSteps) * 2f
    val staffScale = if (compactVertical) {
        1f
    } else if (clefs.size == 2) {
        if (model.difficulty == MAX_DIFFICULTY) {
            tuning.doubleStaffDifficulty4Scale
        } else {
            tuning.doubleStaffScale
        }
    } else {
        tuning.doubleStaffScale * tuning.singleStaffToDoubleRatio
    }
    val lineSpacing = singleStaffLineSpacing * staffScale
    val slotCount = (visibleSlotCount ?: model.chords.size)
        .coerceAtLeast(tuning.minimumNoteSlots)
    val clefWidth = lineSpacing * tuning.clefReservedWidthInLineSpaces
    val clefRight = paddingX + clefWidth
    val notesStartX = clefRight +
        lineSpacing * noteAreaExtraLeftPaddingInLineSpaces
    val usableWidth = size.width - notesStartX - paddingX
    val slotWidth = usableWidth / slotCount

    clefs.forEachIndexed { staffIndex, clef ->
        val blockTop = paddingY + staffIndex * (blockHeight + gap)
        val blockCenterY = blockTop + blockHeight / 2f
        val bottomLineY = if (compactVertical) {
            blockTop + blockHeight - ledgerBelow * (lineSpacing / 2f)
        } else {
            blockCenterY + 2f * lineSpacing
        }
        model.ranges[clef]?.let { range ->
            drawRange(
                range = range,
                clef = clef,
                left = notesStartX,
                width = usableWidth,
                bottomLineY = bottomLineY,
                lineSpacing = lineSpacing,
                color = rangeColor,
            )
        }
        model.highlightIndex?.let { highlightIndex ->
            val current = model.chords.getOrNull(highlightIndex)
            if (current != null && current.notes.any { it.clef == clef }) {
                val x = notesStartX + (highlightIndex - slotShift + 0.5f) * slotWidth
                clipRect(left = clefRight, top = 0f, right = size.width, bottom = size.height) {
                    drawCurrentChordHighlight(
                        chord = current,
                        x = x,
                        bottomLineY = bottomLineY,
                        lineSpacing = lineSpacing,
                        color = LightBlue,
                    )
                }
            }
        }
        drawStaffLines(
            left = paddingX,
            right = if (extendStaffLinesToEnd) size.width else size.width - paddingX,
            bottomLineY = bottomLineY,
            lineSpacing = lineSpacing,
            strokeWidth = staffLineWidth,
            color = staffLineColor,
        )
        drawClef(
            clef = clef,
            left = paddingX,
            bottomLineY = bottomLineY,
            lineSpacing = lineSpacing,
            painter = if (clef == Clef.SOL) trebleClef else bassClef,
        )
        clipRect(left = clefRight, top = 0f, right = size.width, bottom = size.height) {
            model.chords.forEachIndexed { index, chord ->
                val x = notesStartX + (index - slotShift + 0.5f) * slotWidth
                val highlighted = index == model.highlightIndex
                drawChord(
                    chord = chord,
                    x = x,
                    clef = clef,
                    bottomLineY = bottomLineY,
                    lineSpacing = lineSpacing,
                    color = if (highlighted) highlightColor else staffColor,
                    notePainter = notePainter,
                    sharpPainter = sharpPainter,
                    flatPainter = flatPainter,
                    naturalPainter = naturalPainter,
                )
            }
        }
    }
}

private fun DrawScope.drawStaffLines(
    left: Float,
    right: Float,
    bottomLineY: Float,
    lineSpacing: Float,
    strokeWidth: Float,
    color: Color,
) {
    val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Square)
    repeat(5) { line ->
        val y = bottomLineY - line * lineSpacing
        drawLine(color, Offset(left, y), Offset(right, y), strokeWidth = stroke.width)
    }
}

private fun DrawScope.drawRange(
    range: IntRange,
    clef: Clef,
    left: Float,
    width: Float,
    bottomLineY: Float,
    lineSpacing: Float,
    color: Color,
) {
    val minStep = rangeStaffStep(range.first, clef)
    val maxStep = rangeStaffStep(range.last, clef)
    val topY = yForStep(maxOf(minStep, maxStep).toFloat(), bottomLineY, lineSpacing)
    val bottomY = yForStep(minOf(minStep, maxStep).toFloat(), bottomLineY, lineSpacing)
    val radius = lineSpacing * LayoutTuning.Staff.rangeCornerRadiusInLineSpaces
    drawRoundRect(
        color = color,
        topLeft = Offset(left, topY),
        size = Size(width, (bottomY - topY).coerceAtLeast(0f)),
        cornerRadius = CornerRadius(radius, radius),
    )
}

private fun DrawScope.drawCurrentChordHighlight(
    chord: StaffChord,
    x: Float,
    bottomLineY: Float,
    lineSpacing: Float,
    color: Color,
) {
    val layout = layoutChord(chord.notes, x, lineSpacing, bottomLineY, staffLayoutMetrics())
    if (layout.heads.isEmpty()) return
    val tuning = LayoutTuning.Staff
    val padding = lineSpacing * tuning.currentNoteHighlightPaddingInLineSpaces
    val staffTop = bottomLineY - 4f * lineSpacing
    val left = layout.bounds.left
    val right = layout.bounds.right
    val top = minOf(layout.bounds.top, staffTop)
    val bottom = maxOf(layout.bounds.bottom, bottomLineY)
    val radius = lineSpacing * tuning.currentNoteHighlightCornerRadiusInLineSpaces
    drawRoundRect(
        color = color,
        topLeft = Offset(left - padding, top - padding),
        size = Size((right - left) + padding * 2f, (bottom - top) + padding * 2f),
        cornerRadius = CornerRadius(radius, radius),
    )
}

private fun rangeStaffStep(diatonicStep: Int, clef: Clef): Int =
    Pitch(diatonicStep).staffStep(clef)

private fun DrawScope.drawChord(
    chord: StaffChord,
    x: Float,
    clef: Clef,
    bottomLineY: Float,
    lineSpacing: Float,
    color: Color,
    notePainter: Painter,
    sharpPainter: Painter,
    flatPainter: Painter,
    naturalPainter: Painter,
) {
    val onStaff = chord.notes.filter { it.clef == clef }
    if (onStaff.isEmpty()) return
    val layout = layoutChord(onStaff, x, lineSpacing, bottomLineY, staffLayoutMetrics())
    val ledgerHalf = lineSpacing * LayoutTuning.Staff.ledgerHalfWidthInLineSpaces
    val ledgerLeft = layout.heads.minOf { it.x } - ledgerHalf
    val ledgerRight = layout.heads.maxOf { it.x } + ledgerHalf
    layout.ledgerSteps.forEach { ledgerStep ->
        val ly = yForStep(ledgerStep.toFloat(), bottomLineY, lineSpacing)
        drawLine(
            color,
            Offset(ledgerLeft, ly),
            Offset(ledgerRight, ly),
            strokeWidth = lineSpacing * LayoutTuning.Staff.ledgerLineWidthInLineSpaces,
        )
    }
    if (layout.useCompleteNote) {
        val head = layout.heads.first()
        drawCompleteNote(
            x = head.x,
            step = head.step,
            stemDown = head.stemDown,
            bottomLineY = bottomLineY,
            lineSpacing = lineSpacing,
            painter = notePainter,
        )
    } else {
        val stem = layout.stem
        if (stem != null) {
            val headWidth = lineSpacing * LayoutTuning.Staff.noteWidthInLineSpaces
            drawLine(
                color = color,
                start = Offset(stem.x, stem.startY),
                end = Offset(stem.x, stem.endY),
                strokeWidth = headWidth * LayoutTuning.Staff.stemThicknessInNoteWidths,
                cap = StrokeCap.Butt,
            )
        }
        val headWidth = lineSpacing * LayoutTuning.Staff.noteWidthInLineSpaces
        layout.heads.forEach { head ->
            drawNoteHeadOval(
                x = head.x,
                y = yForStep(head.step.toFloat(), bottomLineY, lineSpacing),
                width = headWidth,
                color = color,
            )
        }
    }
    layout.accidentals.forEach { accidental ->
        val (painter, glyph) = when (accidental.accidental) {
            Accidental.SHARP -> sharpPainter to AccidentalGlyph(
                widthInLineSpaces = LayoutTuning.Staff.sharpWidthInLineSpaces,
                heightInLineSpaces = LayoutTuning.Staff.sharpHeightInLineSpaces,
            )
            Accidental.FLAT -> flatPainter to AccidentalGlyph(
                widthInLineSpaces = LayoutTuning.Staff.flatWidthInLineSpaces,
                heightInLineSpaces = LayoutTuning.Staff.flatHeightInLineSpaces,
                centerYOffsetInLineSpaces = LayoutTuning.Staff.flatCenterYOffsetInLineSpaces,
            )
            Accidental.NATURAL -> naturalPainter to AccidentalGlyph(
                widthInLineSpaces = LayoutTuning.Staff.naturalWidthInLineSpaces,
                heightInLineSpaces = LayoutTuning.Staff.naturalHeightInLineSpaces,
            )
            Accidental.NONE -> return@forEach
        }
        drawNoteHead(
            painter = painter,
            x = accidental.x,
            y = yForStep(accidental.step.toFloat(), bottomLineY, lineSpacing) +
                lineSpacing * glyph.centerYOffsetInLineSpaces,
            width = lineSpacing * glyph.widthInLineSpaces,
            height = lineSpacing * glyph.heightInLineSpaces,
        )
    }
}

private data class AccidentalGlyph(
    val widthInLineSpaces: Float,
    val heightInLineSpaces: Float,
    val centerYOffsetInLineSpaces: Float = 0f,
)

private fun DrawScope.drawNoteHeadOval(
    x: Float,
    y: Float,
    width: Float,
    color: Color,
) {
    val tuning = LayoutTuning.Staff
    val rx = width * tuning.noteHeadEllipseRx / tuning.noteHeadSvgWidth
    val ry = width * tuning.noteHeadEllipseRy / tuning.noteHeadSvgWidth
    rotate(degrees = tuning.noteHeadRotationDegrees, pivot = Offset(x, y)) {
        drawOval(
            color = color,
            topLeft = Offset(x - rx, y - ry),
            size = Size(rx * 2f, ry * 2f),
        )
    }
}

private fun DrawScope.drawNoteHead(
    painter: Painter,
    x: Float,
    y: Float,
    width: Float,
    height: Float,
) {
    withTransform({
        translate(left = x - width / 2f, top = y - height / 2f)
    }) {
        with(painter) {
            draw(size = Size(width, height))
        }
    }
}

private fun DrawScope.drawCompleteNote(
    x: Float,
    step: Int,
    stemDown: Boolean,
    bottomLineY: Float,
    lineSpacing: Float,
    painter: Painter,
) {
    val y = yForStep(step.toFloat(), bottomLineY, lineSpacing)
    val noteWidth = lineSpacing * LayoutTuning.Staff.noteWidthInLineSpaces
    val noteHeight = lineSpacing * LayoutTuning.Staff.noteHeightInLineSpaces
    val noteLeft = x - noteWidth * LayoutTuning.Staff.noteHeadCenterXFraction
    val noteTop = y - noteHeight * LayoutTuning.Staff.noteHeadCenterYFraction
    withTransform({
        if (stemDown) rotate(degrees = 180f, pivot = Offset(x, y))
    }) {
        withTransform({
            translate(left = noteLeft, top = noteTop)
        }) {
            with(painter) {
                draw(size = Size(noteWidth, noteHeight))
            }
        }
    }
}

private fun staffLayoutMetrics(): ChordLayoutMetrics {
    val tuning = LayoutTuning.Staff
    return ChordLayoutMetrics(
        noteWidthInLineSpaces = tuning.noteWidthInLineSpaces,
        noteHeightInLineSpaces = tuning.noteHeightInLineSpaces,
        noteHeadCenterXFraction = tuning.noteHeadCenterXFraction,
        noteHeadCenterYFraction = tuning.noteHeadCenterYFraction,
        stemDownFromStaffStep = tuning.stemDownFromStaffStep,
        stemThicknessInNoteWidths = tuning.stemThicknessInNoteWidths,
        ledgerHalfWidthInLineSpaces = tuning.ledgerHalfWidthInLineSpaces,
        accidentalWidthInLineSpaces = tuning.accidentalMaxWidthInLineSpaces,
        accidentalHeightInLineSpaces = tuning.accidentalMaxHeightInLineSpaces,
        accidentalColumnGapInLineSpaces = tuning.accidentalColumnGapInLineSpaces,
        accidentalToHeadGapInLineSpaces = tuning.accidentalToHeadGapInLineSpaces,
        accidentalSameColumnMinSteps = tuning.accidentalSameColumnMinSteps,
        accidentalFlatExtraSteps = tuning.accidentalFlatExtraSteps,
    )
}

private fun DrawScope.drawClef(
    clef: Clef,
    left: Float,
    bottomLineY: Float,
    lineSpacing: Float,
    painter: Painter,
) {
    val tuning = LayoutTuning.Staff
    val x: Float
    val top: Float
    val width: Float
    val height: Float
    when (clef) {
        Clef.SOL -> {
            x = left + lineSpacing * tuning.trebleClefXInLineSpaces
            top = bottomLineY + lineSpacing * tuning.trebleClefTopInLineSpaces
            width = lineSpacing * tuning.trebleClefWidthInLineSpaces
            height = lineSpacing * tuning.trebleClefHeightInLineSpaces
        }
        Clef.FA -> {
            x = left + lineSpacing * tuning.bassClefXInLineSpaces
            top = bottomLineY + lineSpacing * tuning.bassClefTopInLineSpaces
            width = lineSpacing * tuning.bassClefWidthInLineSpaces
            height = lineSpacing * tuning.bassClefHeightInLineSpaces
        }
    }
    withTransform({
        translate(left = x, top = top)
    }) {
        with(painter) {
            draw(size = Size(width, height))
        }
    }
}
