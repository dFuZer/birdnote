package com.example.birdnote.ui.staff

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
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
import kotlin.math.ceil

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

private class StaffPainters(
    val trebleClef: Painter,
    val bassClef: Painter,
    val note: Painter,
    val sharp: Painter,
    val flat: Painter,
    val natural: Painter,
)

@Composable
fun StaffCanvas(
    model: StaffRenderModel,
    modifier: Modifier = Modifier,
    slide: StaffSlide<*>? = null,
    visibleSlotCount: Int? = null,
    noteAreaExtraLeftPaddingInLineSpaces: Float = 0f,
    extendStaffLinesToEnd: Boolean = false,
    compactVertical: Boolean = false,
) {
    val trebleClef = rememberStaffSvgPainter("key-sol.svg", width = 38, height = 109)
    val bassClef = rememberStaffSvgPainter("key-fa.svg", width = 49, height = 57)
    val note = rememberStaffSvgPainter("note.svg", width = 20, height = 64)
    val sharp = rememberStaffSvgPainter(
        LayoutTuning.Staff.sharpAsset,
        width = LayoutTuning.Staff.accidentalRasterWidth,
        height = LayoutTuning.Staff.accidentalRasterHeight,
    )
    val flat = rememberStaffSvgPainter(
        LayoutTuning.Staff.flatAsset,
        width = LayoutTuning.Staff.accidentalRasterWidth,
        height = LayoutTuning.Staff.accidentalRasterHeight,
    )
    val natural = rememberStaffSvgPainter(
        LayoutTuning.Staff.naturalAsset,
        width = LayoutTuning.Staff.accidentalRasterWidth,
        height = LayoutTuning.Staff.accidentalRasterHeight,
    )
    val painters = remember(trebleClef, bassClef, note, sharp, flat, natural) {
        StaffPainters(trebleClef, bassClef, note, sharp, flat, natural)
    }
    Spacer(
        modifier = modifier.drawWithCache {
            val geometry = staffGeometry(
                size = size,
                model = model,
                visibleSlotCount = visibleSlotCount,
                noteAreaExtraLeftPaddingInLineSpaces = noteAreaExtraLeftPaddingInLineSpaces,
                compactVertical = compactVertical,
            )
            val chordDraws = cacheChordDraws(model, geometry)
            val highlights = highlightRects(model.highlightIndex, chordDraws, geometry.lineSpacing)
            val rangeLayer = obtainGraphicsLayer().apply {
                record {
                    drawRanges(model, geometry)
                }
            }
            val staffLayer = obtainGraphicsLayer().apply {
                record {
                    drawStaff(geometry, painters, extendStaffLinesToEnd)
                }
            }
            val notesSize = IntSize(
                width = ceil(
                    maxOf(
                        size.width,
                        geometry.notesStartX + (model.chords.size + 1) * geometry.slotWidth,
                    ),
                ).toInt(),
                height = ceil(size.height).toInt(),
            )
            val notesLayer = obtainGraphicsLayer().apply {
                record(size = notesSize) {
                    chordDraws.forEach { chordDraw ->
                        drawChord(
                            layout = chordDraw.layout,
                            bottomLineY = chordDraw.bottomLineY,
                            lineSpacing = geometry.lineSpacing,
                            color = Color.Black,
                            painters = painters,
                        )
                    }
                }
            }
            onDrawBehind {
                val offsetX = -(slide?.shift ?: 0f) * geometry.slotWidth
                drawLayer(rangeLayer)
                clipRect(left = geometry.clefRight) {
                    translate(left = offsetX) {
                        highlights.forEach { highlight ->
                            drawRoundRect(
                                color = LightBlue,
                                topLeft = highlight.topLeft,
                                size = highlight.size,
                                cornerRadius = CornerRadius(highlight.radius, highlight.radius),
                            )
                        }
                    }
                }
                drawLayer(staffLayer)
                clipRect(left = geometry.clefRight) {
                    translate(left = offsetX) {
                        drawLayer(notesLayer)
                    }
                }
            }
        },
    )
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

private class StaffGeometry(
    val clefs: List<Clef>,
    val bottomLineYs: List<Float>,
    val paddingX: Float,
    val staffLineWidth: Float,
    val lineSpacing: Float,
    val clefRight: Float,
    val notesStartX: Float,
    val usableWidth: Float,
    val slotWidth: Float,
) {
    fun slotCenterX(index: Int): Float = notesStartX + (index + 0.5f) * slotWidth
}

private class HighlightRect(
    val topLeft: Offset,
    val size: Size,
    val radius: Float,
)

private fun staffGeometry(
    size: Size,
    model: StaffRenderModel,
    visibleSlotCount: Int?,
    noteAreaExtraLeftPaddingInLineSpaces: Float,
    compactVertical: Boolean,
): StaffGeometry {
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
    val bottomLineYs = clefs.indices.map { staffIndex ->
        val blockTop = paddingY + staffIndex * (blockHeight + gap)
        val blockCenterY = blockTop + blockHeight / 2f
        if (compactVertical) {
            blockTop + blockHeight - ledgerBelow * (lineSpacing / 2f)
        } else {
            blockCenterY + 2f * lineSpacing
        }
    }
    return StaffGeometry(
        clefs = clefs,
        bottomLineYs = bottomLineYs,
        paddingX = paddingX,
        staffLineWidth = staffLineWidth,
        lineSpacing = lineSpacing,
        clefRight = clefRight,
        notesStartX = notesStartX,
        usableWidth = usableWidth,
        slotWidth = usableWidth / slotCount,
    )
}

private class CachedChordDraw(
    val index: Int,
    val bottomLineY: Float,
    val layout: ChordLayout,
)

private fun cacheChordDraws(
    model: StaffRenderModel,
    geometry: StaffGeometry,
): List<CachedChordDraw> {
    val metrics = staffLayoutMetrics()
    return buildList {
        geometry.clefs.forEachIndexed { staffIndex, clef ->
            val bottomLineY = geometry.bottomLineYs[staffIndex]
            model.chords.forEachIndexed { index, chord ->
                val onStaff = chord.notes.filter { it.clef == clef }
                if (onStaff.isEmpty()) return@forEachIndexed
                add(
                    CachedChordDraw(
                        index = index,
                        bottomLineY = bottomLineY,
                        layout = layoutChord(
                            onStaff,
                            geometry.slotCenterX(index),
                            geometry.lineSpacing,
                            bottomLineY,
                            metrics,
                        ),
                    ),
                )
            }
        }
    }
}

private fun DrawScope.drawStaff(
    geometry: StaffGeometry,
    painters: StaffPainters,
    extendStaffLinesToEnd: Boolean,
) {
    geometry.clefs.forEachIndexed { staffIndex, clef ->
        val bottomLineY = geometry.bottomLineYs[staffIndex]
        drawStaffLines(
            left = geometry.paddingX,
            right = if (extendStaffLinesToEnd) size.width else size.width - geometry.paddingX,
            bottomLineY = bottomLineY,
            lineSpacing = geometry.lineSpacing,
            strokeWidth = geometry.staffLineWidth,
            color = Color.Black,
        )
        drawClef(
            clef = clef,
            left = geometry.paddingX,
            bottomLineY = bottomLineY,
            lineSpacing = geometry.lineSpacing,
            painter = if (clef == Clef.SOL) painters.trebleClef else painters.bassClef,
        )
    }
}

private fun DrawScope.drawRanges(
    model: StaffRenderModel,
    geometry: StaffGeometry,
) {
    geometry.clefs.forEachIndexed { staffIndex, clef ->
        val range = model.ranges[clef] ?: return@forEachIndexed
        drawRange(
            range = range,
            clef = clef,
            left = geometry.notesStartX,
            width = geometry.usableWidth,
            bottomLineY = geometry.bottomLineYs[staffIndex],
            lineSpacing = geometry.lineSpacing,
            color = NoteZoneHighlight,
        )
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

private fun highlightRects(
    highlightIndex: Int?,
    chordDraws: List<CachedChordDraw>,
    lineSpacing: Float,
): List<HighlightRect> {
    if (highlightIndex == null) return emptyList()
    val tuning = LayoutTuning.Staff
    val padding = lineSpacing * tuning.currentNoteHighlightPaddingInLineSpaces
    val radius = lineSpacing * tuning.currentNoteHighlightCornerRadiusInLineSpaces
    return chordDraws.mapNotNull { chordDraw ->
        if (chordDraw.index != highlightIndex) return@mapNotNull null
        val layout = chordDraw.layout
        if (layout.heads.isEmpty()) return@mapNotNull null
        val staffTop = chordDraw.bottomLineY - 4f * lineSpacing
        val left = layout.bounds.left
        val right = layout.bounds.right
        val top = minOf(layout.bounds.top, staffTop)
        val bottom = maxOf(layout.bounds.bottom, chordDraw.bottomLineY)
        HighlightRect(
            topLeft = Offset(left - padding, top - padding),
            size = Size((right - left) + padding * 2f, (bottom - top) + padding * 2f),
            radius = radius,
        )
    }
}

private fun rangeStaffStep(diatonicStep: Int, clef: Clef): Int =
    Pitch(diatonicStep).staffStep(clef)

private fun DrawScope.drawChord(
    layout: ChordLayout,
    bottomLineY: Float,
    lineSpacing: Float,
    color: Color,
    painters: StaffPainters,
) {
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
            painter = painters.note,
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
            Accidental.SHARP -> painters.sharp to AccidentalGlyph(
                widthInLineSpaces = LayoutTuning.Staff.sharpWidthInLineSpaces,
                heightInLineSpaces = LayoutTuning.Staff.sharpHeightInLineSpaces,
            )
            Accidental.FLAT -> painters.flat to AccidentalGlyph(
                widthInLineSpaces = LayoutTuning.Staff.flatWidthInLineSpaces,
                heightInLineSpaces = LayoutTuning.Staff.flatHeightInLineSpaces,
                centerYOffsetInLineSpaces = LayoutTuning.Staff.flatCenterYOffsetInLineSpaces,
            )
            Accidental.NATURAL -> painters.natural to AccidentalGlyph(
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
