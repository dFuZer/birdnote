package com.dfuzer.birdnote.ui.staff

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import com.dfuzer.birdnote.domain.Accidental
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.Pitch
import com.dfuzer.birdnote.domain.staffStep
import com.dfuzer.birdnote.ui.LayoutTuning

internal data class StaffDrawRequest(
    val model: StaffRenderModel,
    val visibleSlotCount: Int?,
    val noteAreaExtraLeftPaddingInLineSpaces: Float,
    val compactVertical: Boolean,
    val staffScaleOverride: Float?,
    val centerVertically: Boolean,
)

internal class SetupEase<T>(initial: T) {
    var from by mutableStateOf(initial)
    var to by mutableStateOf(initial)
    var fraction by mutableFloatStateOf(1f)
}

/**
 * Keeps the previous setup pose when [target] changes and eases [SetupEase.fraction]
 * from 0 to 1. The first value is shown immediately.
 */
@Composable
internal fun <T> rememberSetupEase(target: T): SetupEase<T> {
    val ease = remember { SetupEase(target) }
    if (ease.to != target) {
        ease.from = ease.to
        ease.to = target
        ease.fraction = 0f
    }
    LaunchedEffect(ease.to) {
        if (ease.fraction >= 1f) return@LaunchedEffect
        val animation = Animatable(ease.fraction)
        animation.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = LayoutTuning.Setup.previewEaseMillis,
                easing = FastOutSlowInEasing,
            ),
        ) {
            ease.fraction = value
        }
        ease.from = ease.to
    }
    return ease
}

internal data class RangePose(
    val clef: Clef,
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val radius: Float,
    val alpha: Float = 1f,
)

internal data class ClefMark(
    val clef: Clef,
    val alpha: Float,
)

internal data class StaffPose(
    val clef: Clef,
    val left: Float,
    val right: Float,
    val bottomLineY: Float,
    val lineSpacing: Float,
    val strokeWidth: Float,
    val alpha: Float = 1f,
)

internal data class EasedStaff(
    val left: Float,
    val right: Float,
    val bottomLineY: Float,
    val lineSpacing: Float,
    val strokeWidth: Float,
    val alpha: Float,
    val clefs: List<ClefMark>,
)

internal data class HeadPose(
    val x: Float,
    val step: Float,
    val stemDown: Boolean,
)

internal data class StemPose(
    val x: Float,
    val startY: Float,
    val endY: Float,
)

internal data class AccidentalPose(
    val accidental: Accidental,
    val x: Float,
    val step: Float,
    val alpha: Float = 1f,
)

internal data class ChordPose(
    val chordIndex: Int,
    val clef: Clef,
    val useCompleteNote: Boolean,
    val heads: List<HeadPose>,
    val stem: StemPose?,
    val accidentals: List<AccidentalPose>,
    val bottomLineY: Float,
    val lineSpacing: Float,
    val alpha: Float = 1f,
)

internal data class SetupPreviewPose(
    val ranges: List<RangePose>,
    val staves: List<StaffPose>,
    val chords: List<ChordPose>,
)

internal data class EasedSetupPreview(
    val ranges: List<RangePose>,
    val staves: List<EasedStaff>,
    val chords: List<ChordPose>,
)

internal fun setupPreviewPose(
    size: Size,
    request: StaffDrawRequest,
): SetupPreviewPose {
    val model = request.model
    val geometry = staffGeometry(
        size = size,
        model = model,
        visibleSlotCount = request.visibleSlotCount,
        noteAreaExtraLeftPaddingInLineSpaces = request.noteAreaExtraLeftPaddingInLineSpaces,
        compactVertical = request.compactVertical,
        staffScaleOverride = request.staffScaleOverride,
        centerVertically = request.centerVertically,
    )
    val right = size.width - geometry.paddingX
    val staves = geometry.clefs.mapIndexed { staffIndex, clef ->
        StaffPose(
            clef = clef,
            left = geometry.paddingX,
            right = right,
            bottomLineY = geometry.bottomLineYs[staffIndex],
            lineSpacing = geometry.lineSpacing,
            strokeWidth = geometry.staffLineWidth,
        )
    }
    val ranges = geometry.clefs.mapIndexedNotNull { staffIndex, clef ->
        val range = model.ranges[clef] ?: return@mapIndexedNotNull null
        val bottomLineY = geometry.bottomLineYs[staffIndex]
        val minStep = Pitch(range.first).staffStep(clef)
        val maxStep = Pitch(range.last).staffStep(clef)
        val top = yForStep(maxOf(minStep, maxStep).toFloat(), bottomLineY, geometry.lineSpacing)
        val bottom = yForStep(minOf(minStep, maxStep).toFloat(), bottomLineY, geometry.lineSpacing)
        RangePose(
            clef = clef,
            left = geometry.notesStartX,
            top = top,
            width = geometry.usableWidth,
            height = (bottom - top).coerceAtLeast(0f),
            radius = geometry.lineSpacing * LayoutTuning.Staff.rangeCornerRadiusInLineSpaces,
        )
    }
    val staffIndexByBottom = geometry.bottomLineYs.withIndex().associate { it.value to it.index }
    val chords = cacheChordDraws(model, geometry).map { draw ->
        val staffIndex = staffIndexByBottom.getValue(draw.bottomLineY)
        val clef = geometry.clefs[staffIndex]
        ChordPose(
            chordIndex = draw.index,
            clef = clef,
            useCompleteNote = draw.layout.useCompleteNote,
            heads = draw.layout.heads.map { head ->
                HeadPose(x = head.x, step = head.step.toFloat(), stemDown = head.stemDown)
            },
            stem = draw.layout.stem?.let { stem ->
                StemPose(x = stem.x, startY = stem.startY, endY = stem.endY)
            },
            accidentals = draw.layout.accidentals.map { accidental ->
                AccidentalPose(
                    accidental = accidental.accidental,
                    x = accidental.x,
                    step = accidental.step.toFloat(),
                )
            },
            bottomLineY = draw.bottomLineY,
            lineSpacing = geometry.lineSpacing,
        )
    }
    return SetupPreviewPose(ranges = ranges, staves = staves, chords = chords)
}

internal fun easeSetupPreview(
    from: SetupPreviewPose,
    to: SetupPreviewPose,
    fraction: Float,
): EasedSetupPreview {
    val t = fraction.coerceIn(0f, 1f)
    if (t <= 0f) return from.toEased()
    if (t >= 1f) return to.toEased()
    return EasedSetupPreview(
        ranges = easeRanges(from.ranges, to.ranges, from.staves.size, to.staves.size, t),
        staves = easeStaves(from.staves, to.staves, t),
        chords = easeChords(from.chords, to.chords, t),
    )
}

private fun SetupPreviewPose.toEased(): EasedSetupPreview = EasedSetupPreview(
    ranges = ranges,
    staves = staves.map { it.toEased() },
    chords = chords,
)

private fun StaffPose.toEased(): EasedStaff = EasedStaff(
    left = left,
    right = right,
    bottomLineY = bottomLineY,
    lineSpacing = lineSpacing,
    strokeWidth = strokeWidth,
    alpha = alpha,
    clefs = listOf(ClefMark(clef, alpha)),
)

private fun easeRanges(
    from: List<RangePose>,
    to: List<RangePose>,
    fromStaffCount: Int,
    toStaffCount: Int,
    t: Float,
): List<RangePose> {
    val sameCount = fromStaffCount == toStaffCount && from.size == to.size
    if (sameCount) {
        return from.zip(to).map { (start, end) ->
            RangePose(
                clef = end.clef,
                left = lerp(start.left, end.left, t),
                top = lerp(start.top, end.top, t),
                width = lerp(start.width, end.width, t),
                height = lerp(start.height, end.height, t),
                radius = lerp(start.radius, end.radius, t),
            )
        }
    }
    val fromByClef = from.associateBy { it.clef }
    val toByClef = to.associateBy { it.clef }
    return buildList {
        to.forEach { end ->
            val start = fromByClef[end.clef]
            if (start == null) {
                add(end.copy(alpha = t))
            } else {
                add(
                    RangePose(
                        clef = end.clef,
                        left = lerp(start.left, end.left, t),
                        top = lerp(start.top, end.top, t),
                        width = lerp(start.width, end.width, t),
                        height = lerp(start.height, end.height, t),
                        radius = lerp(start.radius, end.radius, t),
                    ),
                )
            }
        }
        from.forEach { start ->
            if (start.clef !in toByClef) add(start.copy(alpha = 1f - t))
        }
    }
}

private fun easeStaves(
    from: List<StaffPose>,
    to: List<StaffPose>,
    t: Float,
): List<EasedStaff> {
    if (from.size == to.size) {
        return from.zip(to).map { (start, end) ->
            val lines = EasedStaff(
                left = lerp(start.left, end.left, t),
                right = lerp(start.right, end.right, t),
                bottomLineY = lerp(start.bottomLineY, end.bottomLineY, t),
                lineSpacing = lerp(start.lineSpacing, end.lineSpacing, t),
                strokeWidth = lerp(start.strokeWidth, end.strokeWidth, t),
                alpha = 1f,
                clefs = if (start.clef == end.clef) {
                    listOf(ClefMark(end.clef, 1f))
                } else {
                    listOf(ClefMark(start.clef, 1f - t), ClefMark(end.clef, t))
                },
            )
            lines
        }
    }
    val fromByClef = from.associateBy { it.clef }
    val toByClef = to.associateBy { it.clef }
    return buildList {
        to.forEach { end ->
            val start = fromByClef[end.clef]
            if (start == null) {
                add(end.toEased().copy(alpha = t, clefs = listOf(ClefMark(end.clef, t))))
            } else {
                add(
                    EasedStaff(
                        left = lerp(start.left, end.left, t),
                        right = lerp(start.right, end.right, t),
                        bottomLineY = lerp(start.bottomLineY, end.bottomLineY, t),
                        lineSpacing = lerp(start.lineSpacing, end.lineSpacing, t),
                        strokeWidth = lerp(start.strokeWidth, end.strokeWidth, t),
                        alpha = 1f,
                        clefs = listOf(ClefMark(end.clef, 1f)),
                    ),
                )
            }
        }
        from.forEach { start ->
            if (start.clef !in toByClef) {
                add(start.toEased().copy(alpha = 1f - t, clefs = listOf(ClefMark(start.clef, 1f - t))))
            }
        }
    }
}

private fun easeChords(
    from: List<ChordPose>,
    to: List<ChordPose>,
    t: Float,
): List<ChordPose> {
    val sameCount = from.size == to.size
    val fromKeys = chordKeys(from, sameCount)
    val toKeys = chordKeys(to, sameCount)
    val used = mutableSetOf<Any>()
    return buildList {
        to.forEachIndexed { index, end ->
            val key = toKeys[index]
            val startIndex = fromKeys.indexOf(key)
            val start = from.getOrNull(startIndex)
            if (start != null && key !in used && compatible(start, end)) {
                used += key
                add(blendChord(start, end, t))
            } else {
                add(end.copy(alpha = t))
            }
        }
        from.forEachIndexed { index, start ->
            val key = fromKeys[index]
            if (key !in used) add(start.copy(alpha = 1f - t))
        }
    }
}

private fun chordKeys(chords: List<ChordPose>, sameCount: Boolean): List<Any> {
    if (sameCount) return chords.map { it.chordIndex }
    val ordinal = mutableMapOf<Clef, Int>()
    return chords.map { chord ->
        val index = ordinal.getOrDefault(chord.clef, 0)
        ordinal[chord.clef] = index + 1
        chord.clef to index
    }
}

private fun compatible(start: ChordPose, end: ChordPose): Boolean =
    start.useCompleteNote == end.useCompleteNote && start.heads.size == end.heads.size

private fun blendChord(start: ChordPose, end: ChordPose, t: Float): ChordPose {
    val heads = start.heads.zip(end.heads).map { (fromHead, toHead) ->
        HeadPose(
            x = lerp(fromHead.x, toHead.x, t),
            step = lerp(fromHead.step, toHead.step, t),
            stemDown = if (t < 0.5f) fromHead.stemDown else toHead.stemDown,
        )
    }
    val stem = if (start.stem != null && end.stem != null) {
        StemPose(
            x = lerp(start.stem.x, end.stem.x, t),
            startY = lerp(start.stem.startY, end.stem.startY, t),
            endY = lerp(start.stem.endY, end.stem.endY, t),
        )
    } else {
        null
    }
    val accidentals = if (
        start.accidentals.size == end.accidentals.size &&
        start.accidentals.zip(end.accidentals).all { (a, b) -> a.accidental == b.accidental }
    ) {
        start.accidentals.zip(end.accidentals).map { (fromAccidental, toAccidental) ->
            AccidentalPose(
                accidental = toAccidental.accidental,
                x = lerp(fromAccidental.x, toAccidental.x, t),
                step = lerp(fromAccidental.step, toAccidental.step, t),
            )
        }
    } else {
        start.accidentals.map { it.copy(alpha = 1f - t) } +
            end.accidentals.map { it.copy(alpha = t) }
    }
    return end.copy(
        heads = heads,
        stem = stem,
        accidentals = accidentals,
        bottomLineY = lerp(start.bottomLineY, end.bottomLineY, t),
        lineSpacing = lerp(start.lineSpacing, end.lineSpacing, t),
        alpha = 1f,
    )
}

private fun lerp(start: Float, end: Float, fraction: Float): Float =
    start + (end - start) * fraction
