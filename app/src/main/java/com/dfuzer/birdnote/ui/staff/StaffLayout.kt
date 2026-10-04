package com.dfuzer.birdnote.ui.staff

import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.dfuzer.birdnote.domain.Accidental
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.MAX_DIFFICULTY
import com.dfuzer.birdnote.domain.MIN_DIFFICULTY
import com.dfuzer.birdnote.domain.PracticeChord
import com.dfuzer.birdnote.domain.StaffInterval
import com.dfuzer.birdnote.domain.StaffNote
import com.dfuzer.birdnote.domain.clefs
import com.dfuzer.birdnote.ui.LayoutTuning
import kotlin.math.round

@Immutable
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
    val ranges: Map<Clef, IntRange> = emptyMap(),
)

/** Clip edge on a device pixel, so a note leaving the clef is cut on a pixel boundary. */
internal fun staffClipLeft(clefRight: Float): Float = round(clefRight)

private const val STAFF_STEPS = 8
private const val MAX_LEDGER_STEPS = 4

internal class StaffGeometry(
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

internal class HighlightRect(
    val topLeft: Offset,
    val size: Size,
    val radius: Float,
)

internal fun chordPreviewStaffScale(
    difficulty: Int,
    tileHeightPx: Float,
    notesSingleStaffHeightPx: Float,
): Float {
    if (difficulty >= 3) return LayoutTuning.Setup.chordPreviewReducedElementScale
    if (difficulty != MIN_DIFFICULTY || tileHeightPx <= 0f) return 1f
    val unscaled = lineSpacingForHeight(tileHeightPx, compactVertical = true)
    if (unscaled <= 0f) return 1f
    return notesSingleStaffLineSpacing(notesSingleStaffHeightPx) / unscaled
}

internal fun notesSingleStaffLineSpacing(canvasHeightPx: Float): Float {
    val tuning = LayoutTuning.Staff
    return lineSpacingForHeight(canvasHeightPx, compactVertical = false) *
        tuning.doubleStaffScale *
        tuning.singleStaffToDoubleRatio
}

internal fun lineSpacingForHeight(canvasHeightPx: Float, compactVertical: Boolean): Float {
    val tuning = LayoutTuning.Staff
    val paddingY = canvasHeightPx * if (compactVertical) {
        tuning.compactVerticalPadding
    } else {
        tuning.verticalPadding
    }
    val ledgerAbove = if (compactVertical) tuning.compactLedgerStepsAbove else MAX_LEDGER_STEPS
    val ledgerBelow = if (compactVertical) tuning.compactLedgerStepsBelow else MAX_LEDGER_STEPS
    val totalSteps = STAFF_STEPS + ledgerAbove + ledgerBelow
    return ((canvasHeightPx - paddingY * 2) / totalSteps) * 2f
}

internal fun staffGeometry(
    size: Size,
    model: StaffRenderModel,
    visibleSlotCount: Int?,
    noteAreaExtraLeftPaddingInLineSpaces: Float,
    compactVertical: Boolean,
    staffScaleOverride: Float? = null,
    centerVertically: Boolean = false,
): StaffGeometry {
    val clefs = model.clefMode.clefs()
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
    val ledgerBelow = if (compactVertical) tuning.compactLedgerStepsBelow else MAX_LEDGER_STEPS
    val singleStaffLineSpacing = lineSpacingForHeight(size.height, compactVertical)
    val staffScale = staffScaleOverride ?: if (compactVertical) {
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
        if (compactVertical && !centerVertically) {
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

internal class CachedChordDraw(
    val index: Int,
    val bottomLineY: Float,
    val layout: ChordLayout,
)

internal fun cacheChordDraws(
    model: StaffRenderModel,
    geometry: StaffGeometry,
): List<CachedChordDraw> {
    val metrics = ChordLayoutMetrics()
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

internal fun highlightRects(
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

internal data class ClefGlyphBox(
    val xInLineSpaces: Float,
    val topInLineSpaces: Float,
    val widthInLineSpaces: Float,
    val heightInLineSpaces: Float,
)

internal fun Clef.glyphBox(): ClefGlyphBox {
    val tuning = LayoutTuning.Staff
    return when (this) {
        Clef.SOL -> ClefGlyphBox(
            tuning.trebleClefXInLineSpaces,
            tuning.trebleClefTopInLineSpaces,
            tuning.trebleClefWidthInLineSpaces,
            tuning.trebleClefHeightInLineSpaces,
        )
        Clef.FA -> ClefGlyphBox(
            tuning.bassClefXInLineSpaces,
            tuning.bassClefTopInLineSpaces,
            tuning.bassClefWidthInLineSpaces,
            tuning.bassClefHeightInLineSpaces,
        )
        Clef.ALTO -> ClefGlyphBox(
            tuning.cClefXInLineSpaces,
            tuning.altoClefTopInLineSpaces,
            tuning.cClefWidthInLineSpaces,
            tuning.cClefHeightInLineSpaces,
        )
        Clef.TENOR -> ClefGlyphBox(
            tuning.cClefXInLineSpaces,
            tuning.tenorClefTopInLineSpaces,
            tuning.cClefWidthInLineSpaces,
            tuning.cClefHeightInLineSpaces,
        )
    }
}


internal fun Clef.assetName(): String = when (this) {
    Clef.SOL -> "key-sol.svg"
    Clef.FA -> "key-fa.svg"
    Clef.ALTO, Clef.TENOR -> "key-ut.svg"
}

internal data class AccidentalGlyph(
    val asset: String,
    val widthInLineSpaces: Float,
    val heightInLineSpaces: Float,
    val centerYOffsetInLineSpaces: Float = 0f,
)

internal fun Accidental.glyph(): AccidentalGlyph? {
    val tuning = LayoutTuning.Staff
    return when (this) {
        Accidental.SHARP -> AccidentalGlyph(tuning.sharpAsset, tuning.sharpWidthInLineSpaces, tuning.sharpHeightInLineSpaces)
        Accidental.FLAT -> AccidentalGlyph(tuning.flatAsset, tuning.flatWidthInLineSpaces, tuning.flatHeightInLineSpaces, tuning.flatCenterYOffsetInLineSpaces)
        Accidental.NATURAL -> AccidentalGlyph(tuning.naturalAsset, tuning.naturalWidthInLineSpaces, tuning.naturalHeightInLineSpaces)
        Accidental.NONE -> null
    }
}
