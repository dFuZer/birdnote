package com.dfuzer.birdnote.ui.staff

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.dfuzer.birdnote.domain.Accidental
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.theme.NoteZoneHighlight
import kotlin.math.roundToInt

@Stable
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
    visibleSlotCount: Int? = null,
    noteAreaExtraLeftPaddingInLineSpaces: Float = 0f,
    compactVertical: Boolean = false,
    staffScaleOverride: Float? = null,
    centerVertically: Boolean = false,
) {
    val trebleClef = rememberStaffSvgPainter(Clef.SOL.assetName(), width = 38, height = 109)
    val bassClef = rememberStaffSvgPainter(Clef.FA.assetName(), width = 49, height = 57)
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
    EasingStaffCanvas(
        request = StaffDrawRequest(
            model = model,
            visibleSlotCount = visibleSlotCount,
            noteAreaExtraLeftPaddingInLineSpaces = noteAreaExtraLeftPaddingInLineSpaces,
            compactVertical = compactVertical,
            staffScaleOverride = staffScaleOverride,
            centerVertically = centerVertically,
        ),
        painters = painters,
        modifier = modifier,
    )
}

@Composable
private fun EasingStaffCanvas(
    request: StaffDrawRequest,
    painters: StaffPainters,
    modifier: Modifier,
) {
    val ease = rememberSetupEase(request)
    val fraction = ease.fraction
    val from = ease.from
    val to = ease.to
    Spacer(
        modifier = modifier.drawBehind {
            drawEasedPreview(
                easeSetupPreview(
                    from = setupPreviewPose(size, from),
                    to = setupPreviewPose(size, to),
                    fraction = fraction,
                ),
                painters,
            )
        },
    )
}

private fun DrawScope.drawEasedPreview(
    pose: EasedSetupPreview,
    painters: StaffPainters,
) {
    pose.ranges.forEach { range ->
        withAlpha(range.alpha) {
            drawRoundRect(
                color = NoteZoneHighlight,
                topLeft = Offset(range.left, range.top),
                size = Size(range.width, range.height),
                cornerRadius = CornerRadius(range.radius, range.radius),
            )
        }
    }
    pose.staves.forEach { staff ->
        withAlpha(staff.alpha) {
            drawStaffLines(
                left = staff.left,
                right = staff.right,
                bottomLineY = staff.bottomLineY,
                lineSpacing = staff.lineSpacing,
                strokeWidth = staff.strokeWidth,
                color = Color.Black,
            )
        }
        staff.clefs.forEach { clef ->
            withAlpha(minOf(staff.alpha, clef.alpha)) {
                drawClef(
                    clef = clef.clef,
                    left = staff.left,
                    bottomLineY = staff.bottomLineY,
                    lineSpacing = staff.lineSpacing,
                    painter = if (clef.clef == Clef.SOL) painters.trebleClef else painters.bassClef,
                )
            }
        }
    }
    pose.chords.forEach { chord ->
        drawPosedChord(chord, painters)
    }
}

private fun DrawScope.drawPosedChord(
    chord: ChordPose,
    painters: StaffPainters,
) {
    if (chord.heads.isEmpty() || chord.alpha <= 0f) return
    withAlpha(chord.alpha) {
        val ledgerHalf = chord.lineSpacing * LayoutTuning.Staff.ledgerHalfWidthInLineSpaces
        val ledgerLeft = chord.heads.minOf { it.x } - ledgerHalf
        val ledgerRight = chord.heads.maxOf { it.x } + ledgerHalf
        chord.heads
            .flatMap { ledgerSteps(it.step.roundToInt()) }
            .distinct()
            .forEach { ledgerStep ->
                val ly = yForStep(ledgerStep.toFloat(), chord.bottomLineY, chord.lineSpacing)
                drawLine(
                    Color.Black,
                    Offset(ledgerLeft, ly),
                    Offset(ledgerRight, ly),
                    strokeWidth = chord.lineSpacing * LayoutTuning.Staff.ledgerLineWidthInLineSpaces,
                )
            }
        if (chord.useCompleteNote) {
            val head = chord.heads.first()
            drawCompleteNote(
                x = head.x,
                step = head.step,
                stemDown = head.stemDown,
                bottomLineY = chord.bottomLineY,
                lineSpacing = chord.lineSpacing,
                painter = painters.note,
            )
        } else {
            val stem = chord.stem
            if (stem != null) {
                val headWidth = chord.lineSpacing * LayoutTuning.Staff.noteWidthInLineSpaces
                drawLine(
                    color = Color.Black,
                    start = Offset(stem.x, stem.startY),
                    end = Offset(stem.x, stem.endY),
                    strokeWidth = headWidth * LayoutTuning.Staff.stemThicknessInNoteWidths,
                    cap = StrokeCap.Butt,
                )
            }
            val headWidth = chord.lineSpacing * LayoutTuning.Staff.noteWidthInLineSpaces
            chord.heads.forEach { head ->
                drawNoteHeadOval(
                    x = head.x,
                    y = yForStep(head.step, chord.bottomLineY, chord.lineSpacing),
                    width = headWidth,
                    color = Color.Black,
                )
            }
        }
        chord.accidentals.forEach { accidental ->
            val glyph = accidental.accidental.glyph() ?: return@forEach
            val painter = when (accidental.accidental) {
                Accidental.SHARP -> painters.sharp
                Accidental.FLAT -> painters.flat
                Accidental.NATURAL -> painters.natural
                Accidental.NONE -> return@forEach
            }
            withAlpha(accidental.alpha) {
                drawNoteHead(
                    painter = painter,
                    x = accidental.x,
                    y = yForStep(accidental.step, chord.bottomLineY, chord.lineSpacing) +
                        chord.lineSpacing * glyph.centerYOffsetInLineSpaces,
                    width = chord.lineSpacing * glyph.widthInLineSpaces,
                    height = chord.lineSpacing * glyph.heightInLineSpaces,
                )
            }
        }
    }
}

private fun DrawScope.withAlpha(alpha: Float, block: DrawScope.() -> Unit) {
    if (alpha <= 0.001f) return
    if (alpha >= 0.999f) {
        this.block()
        return
    }
    drawContext.canvas.saveLayer(
        Rect(0f, 0f, size.width, size.height),
        Paint().apply { this.alpha = alpha },
    )
    this.block()
    drawContext.canvas.restore()
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
    step: Float,
    stemDown: Boolean,
    bottomLineY: Float,
    lineSpacing: Float,
    painter: Painter,
) {
    val y = yForStep(step, bottomLineY, lineSpacing)
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

private fun DrawScope.drawClef(
    clef: Clef,
    left: Float,
    bottomLineY: Float,
    lineSpacing: Float,
    painter: Painter,
) {
    val box = clef.glyphBox()
    val x = left + lineSpacing * box.xInLineSpaces
    val top = bottomLineY + lineSpacing * box.topInLineSpaces
    val width = lineSpacing * box.widthInLineSpaces
    val height = lineSpacing * box.heightInLineSpaces
    withTransform({
        translate(left = x, top = top)
    }) {
        with(painter) {
            draw(size = Size(width, height))
        }
    }
}
