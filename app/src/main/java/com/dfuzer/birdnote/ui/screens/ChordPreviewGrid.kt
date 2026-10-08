package com.dfuzer.birdnote.ui.screens

import com.dfuzer.birdnote.domain.ChordQuality
import com.dfuzer.birdnote.domain.MIN_DIFFICULTY
import com.dfuzer.birdnote.domain.PracticeChord
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.staff.chordPreviewStaffScale
import com.dfuzer.birdnote.ui.staff.lineSpacingForHeight

internal data class ChordGridTarget(
    val chords: List<PracticeChord>,
    val difficulty: Int,
)

internal data class ChordGridSlot(
    val column: Int,
    val row: Int,
    val columns: Int,
    val rows: Int,
)

internal data class ChordTileMotion(
    val quality: ChordQuality,
    val fromChord: PracticeChord?,
    val toChord: PracticeChord?,
    val fromDifficulty: Int?,
    val toDifficulty: Int?,
    val fromSlot: ChordGridSlot?,
    val toSlot: ChordGridSlot?,
)

internal data class TileFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val alpha: Float,
    val scale: Float,
)

internal data class ChordTileStaffEase(
    val staffScale: Float,
    val verticalCenter: Float,
)

internal fun chordPreviewColumnCount(count: Int): Int {
    val maxRows = LayoutTuning.Setup.chordPreviewMaxRows
    if (count <= maxRows) return count.coerceAtLeast(1)
    return (count + maxRows - 1) / maxRows
}

internal fun chordGridSlot(index: Int, count: Int): ChordGridSlot {
    val columns = chordPreviewColumnCount(count)
    val rows = (count + columns - 1) / columns
    return ChordGridSlot(
        column = index % columns,
        row = index / columns,
        columns = columns,
        rows = rows.coerceAtLeast(1),
    )
}

internal fun chordTileMotions(
    fromChords: List<PracticeChord>,
    fromDifficulty: Int,
    toChords: List<PracticeChord>,
    toDifficulty: Int,
): List<ChordTileMotion> {
    val fromSlots = fromChords.mapIndexed { index, chord ->
        chord.quality to (chord to chordGridSlot(index, fromChords.size))
    }.toMap()
    val toSlots = toChords.mapIndexed { index, chord ->
        chord.quality to (chord to chordGridSlot(index, toChords.size))
    }.toMap()
    val leaving = fromChords.map { it.quality }.filter { it !in toSlots }
    val staying = toChords.map { chord ->
        val from = fromSlots[chord.quality]
        val to = toSlots.getValue(chord.quality)
        ChordTileMotion(
            quality = chord.quality,
            fromChord = from?.first,
            toChord = to.first,
            fromDifficulty = if (from == null) null else fromDifficulty,
            toDifficulty = toDifficulty,
            fromSlot = from?.second,
            toSlot = to.second,
        )
    }
    return leaving.map { quality ->
        val from = fromSlots.getValue(quality)
        ChordTileMotion(
            quality = quality,
            fromChord = from.first,
            toChord = null,
            fromDifficulty = fromDifficulty,
            toDifficulty = null,
            fromSlot = from.second,
            toSlot = null,
        )
    } + staying
}

internal fun settledStaffHeightPx(
    slotHeightPx: Float,
    tileHeightPx: Float,
    staffHeightPx: Float,
): Float = (slotHeightPx - (tileHeightPx - staffHeightPx).coerceAtLeast(0f)).coerceAtLeast(0f)

internal fun easedChordTileStaff(
    fromDifficulty: Int?,
    toDifficulty: Int?,
    fraction: Float,
    fromStaffHeightPx: Float,
    toStaffHeightPx: Float,
    staffHeightPx: Float,
    notesSingleStaffHeightPx: Float,
): ChordTileStaffEase {
    val startDifficulty = fromDifficulty ?: toDifficulty
    val endDifficulty = toDifficulty ?: fromDifficulty
    if (startDifficulty == null || endDifficulty == null) {
        return ChordTileStaffEase(staffScale = 1f, verticalCenter = 0f)
    }
    val move = when {
        fromDifficulty == null -> 1f
        toDifficulty == null -> 0f
        else -> fraction.coerceIn(0f, 1f)
    }
    val startScale = chordPreviewStaffScale(
        startDifficulty,
        fromStaffHeightPx,
        notesSingleStaffHeightPx,
    )
    val endScale = chordPreviewStaffScale(
        endDifficulty,
        toStaffHeightPx,
        notesSingleStaffHeightPx,
    )
    val startSpacing = lineSpacingForHeight(fromStaffHeightPx, compactVertical = true) * startScale
    val endSpacing = lineSpacingForHeight(toStaffHeightPx, compactVertical = true) * endScale
    val currentSpacing = lineSpacingForHeight(staffHeightPx, compactVertical = true)
    val staffScale = if (currentSpacing <= 0f) {
        lerp(startScale, endScale, move)
    } else {
        lerp(startSpacing, endSpacing, move) / currentSpacing
    }
    return ChordTileStaffEase(
        staffScale = staffScale,
        verticalCenter = lerp(
            if (startDifficulty == MIN_DIFFICULTY) 1f else 0f,
            if (endDifficulty == MIN_DIFFICULTY) 1f else 0f,
            move,
        ),
    )
}

internal fun easedTileFrame(
    motion: ChordTileMotion,
    fraction: Float,
    width: Float,
    height: Float,
    gap: Float,
): TileFrame {
    val t = fraction.coerceIn(0f, 1f)
    val from = motion.fromSlot
    val to = motion.toSlot
    val alpha = when {
        from == null && to != null -> t
        from != null && to == null -> 1f - t
        else -> 1f
    }
    val scale = when {
        from == null && to != null -> lerp(0.92f, 1f, t)
        from != null && to == null -> lerp(1f, 0.92f, t)
        else -> 1f
    }
    val start = chordGridRect(from ?: to!!, width, height, gap)
    val end = chordGridRect(to ?: from!!, width, height, gap)
    val move = if (from == null || to == null) 0f else t
    return TileFrame(
        left = lerp(start.left, end.left, move),
        top = lerp(start.top, end.top, move),
        width = lerp(start.width, end.width, move),
        height = lerp(start.height, end.height, move),
        alpha = alpha,
        scale = scale,
    )
}

internal fun chordGridRect(
    slot: ChordGridSlot,
    width: Float,
    height: Float,
    gap: Float,
): TileFrame {
    val cellWidth = (width - gap * (slot.columns - 1)) / slot.columns
    val cellHeight = (height - gap * (slot.rows - 1)) / slot.rows
    return TileFrame(
        left = slot.column * (cellWidth + gap),
        top = slot.row * (cellHeight + gap),
        width = cellWidth,
        height = cellHeight,
        alpha = 1f,
        scale = 1f,
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction
