package com.example.birdnote.ui.staff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.example.birdnote.domain.isQueueAdvance
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign

@Stable
class StaffSlide<T> {
    var notes: List<T> by mutableStateOf(emptyList())
        private set
    var highlightIndex: Int by mutableIntStateOf(0)
        private set

    /** Absolute index of [notes] first item. */
    var origin: Int by mutableIntStateOf(0)
        private set

    /**
     * Absolute slot position. Changes every animation frame; read it only while drawing.
     */
    var shift: Float by mutableFloatStateOf(0f)
        private set

    /**
     * Slots moved on the last animation frame. Zero while the belt is resting.
     * Read it only while drawing; the sign is the direction of travel.
     */
    var travelSlots: Float by mutableFloatStateOf(0f)
        private set

    internal fun publish(belt: List<T>, targetShift: Float, newOrigin: Int) {
        notes = belt.toList()
        origin = newOrigin
        val local = (targetShift - newOrigin).roundToInt()
        highlightIndex = local.coerceIn(0, (belt.size - 1).coerceAtLeast(0))
    }

    internal fun moveTo(shift: Float, travelSlots: Float = 0f) {
        if (this.shift != shift) this.shift = shift
        if (this.travelSlots != travelSlots) this.travelSlots = travelSlots
    }
}

/**
 * Horizontal blur sigma, in pixels, for one frame of travel.
 * Slow motion stays sharp. The result never exceeds [maxSigmaPx], which keeps
 * the smear inside the padded note layer.
 */
internal fun motionBlurSigmaPx(
    travelSlots: Float,
    slotWidth: Float,
    minTravelPx: Float,
    sigmaPerTravelPx: Float,
    maxSigmaPx: Float,
): Float {
    if (maxSigmaPx <= 0f) return 0f
    val travelPx = abs(travelSlots) * slotWidth
    if (travelPx < minTravelPx) return 0f
    return (travelPx * sigmaPerTravelPx).coerceAtMost(maxSigmaPx)
}

/** Slots the belt is shifted left of absolute index 0. */
internal fun slideOffsetSlots(shift: Float, origin: Int): Float = shift - origin

/** Shift where a fresh belt is drawn, one slot short of resting at 0 so it slides in. */
internal fun spawnShift(visibleCount: Int): Float =
    1f - visibleCount.coerceAtLeast(1).toFloat()

/**
 * Moves [shift] toward [target]. Speed is the remaining distance divided by
 * [followTimeSeconds], and never slower than [minSpeedSlotsPerSecond].
 */
internal fun advanceShift(
    shift: Float,
    target: Float,
    followTimeSeconds: Float,
    minSpeedSlotsPerSecond: Float,
    dtSeconds: Float,
): Float {
    val remaining = target - shift
    val distance = abs(remaining)
    if (distance == 0f) return shift
    val tau = followTimeSeconds.coerceAtLeast(0.001f)
    val speed = max(minSpeedSlotsPerSecond, distance / tau)
    val step = speed * dtSeconds
    if (step >= distance) return target
    return shift + sign(remaining) * step
}

/**
 * How many leading notes are fully past the left edge and safe to drop.
 * Keeps at least [visibleCount] notes on the belt.
 */
internal fun dropCount(shift: Float, origin: Int, beltSize: Int, visibleCount: Int): Int {
    val offscreen = floor(shift).toInt() - origin
    val room = (beltSize - visibleCount.coerceAtLeast(0)).coerceAtLeast(0)
    return offscreen.coerceIn(0, room)
}

@Composable
fun <T> rememberStaffSlide(
    notes: List<T>,
    visibleCount: Int,
    followTimeMillis: Int,
    minSpeedSlotsPerSecond: Float,
): StaffSlide<T> {
    val slide = remember { StaffSlide<T>() }
    val belt = remember { mutableListOf<T>() }
    var previousNotes by remember { mutableStateOf<List<T>>(emptyList()) }
    var targetShift by remember { mutableFloatStateOf(0f) }
    var origin by remember { mutableIntStateOf(0) }
    val notesState = rememberUpdatedState(notes)
    val visibleState = rememberUpdatedState(visibleCount)

    LaunchedEffect(Unit) {
        snapshotFlow { notesState.value to visibleState.value }.collect { (incoming, count) ->
            when {
                incoming.isEmpty() -> {
                    belt.clear()
                    targetShift = 0f
                    origin = 0
                    slide.moveTo(0f)
                }
                previousNotes.isEmpty() || !isQueueAdvance(previousNotes, incoming) -> {
                    belt.clear()
                    belt.addAll(incoming)
                    targetShift = 0f
                    origin = 0
                    slide.moveTo(spawnShift(count))
                }
                else -> {
                    belt.add(incoming.last())
                    targetShift += 1f
                    // Drop scrolled-off notes in this same publish as the appended one.
                    origin = pruneBelt(belt, slide.shift, origin, count)
                }
            }
            slide.publish(belt, targetShift, origin)
            previousNotes = incoming
        }
    }

    LaunchedEffect(followTimeMillis, minSpeedSlotsPerSecond) {
        val tauSeconds = followTimeMillis.coerceAtLeast(1) / 1_000f
        var lastTime = 0L
        while (true) {
            snapshotFlow { targetShift to slide.shift }
                .first { (target, shift) -> target != shift }
            lastTime = 0L
            while (slide.shift != targetShift) {
                withFrameNanos { time ->
                    if (lastTime == 0L) {
                        lastTime = time
                        return@withFrameNanos
                    }
                    val dt = ((time - lastTime) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    lastTime = time
                    val previous = slide.shift
                    val next = advanceShift(
                        previous,
                        targetShift,
                        tauSeconds,
                        minSpeedSlotsPerSecond,
                        dt,
                    )
                    if (next != previous) slide.moveTo(next, next - previous)
                }
            }
            if (slide.travelSlots != 0f) slide.moveTo(slide.shift)
        }
    }

    return slide
}

private fun <T> pruneBelt(
    belt: MutableList<T>,
    shift: Float,
    origin: Int,
    visibleCount: Int,
): Int {
    val dropped = dropCount(shift, origin, belt.size, visibleCount)
    repeat(dropped) { belt.removeAt(0) }
    return origin + dropped
}
