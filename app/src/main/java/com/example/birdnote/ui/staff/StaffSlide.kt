package com.example.birdnote.ui.staff

import com.example.birdnote.domain.isQueueAdvance
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign

/** What [StaffBelt.offer] did with an incoming queue. */
internal enum class BeltStep {
    Unchanged,
    Reset,
    Advanced,
}

/**
 * Strength of a correct-answer brighten, from 1 at the tap down to 0.
 * The slide keeps moving for longer than this fade.
 */
internal fun brightenStrength(elapsedSeconds: Float, durationSeconds: Float): Float {
    if (durationSeconds <= 0f) return 0f
    val t = elapsedSeconds / durationSeconds
    if (t <= 0f) return 1f
    if (t >= 1f) return 0f
    val remain = 1f - t
    return remain * remain
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
 * Position after [elapsedSeconds] of [advanceShift].
 *
 * Speed is the remaining distance divided by [followTimeSeconds], and never
 * slower than [minSpeedSlotsPerSecond]. A late frame samples the same curve.
 */
internal fun shiftAt(
    elapsedSeconds: Float,
    startShift: Float,
    targetShift: Float,
    followTimeSeconds: Float,
    minSpeedSlotsPerSecond: Float,
): Float {
    val elapsed = elapsedSeconds.toDouble().coerceAtLeast(0.0)
    val start = startShift.toDouble()
    val target = targetShift.toDouble()
    val distance = abs(target - start)
    if (distance == 0.0 || elapsed == 0.0) return startShift
    val direction = sign(targetShift - startShift).toDouble()
    val tau = followTimeSeconds.coerceAtLeast(0.001f).toDouble()
    val minSpeed = minSpeedSlotsPerSecond.toDouble().coerceAtLeast(0.0)
    val threshold = minSpeed * tau
    if (minSpeed == 0.0) {
        val remaining = distance * exp(-elapsed / tau)
        return (target - direction * remaining).toFloat()
    }
    if (distance <= threshold) {
        val travel = minSpeed * elapsed
        if (travel >= distance) return targetShift
        return (start + direction * travel).toFloat()
    }
    val switchAt = tau * ln(distance / threshold)
    if (elapsed <= switchAt) {
        val remaining = distance * exp(-elapsed / tau)
        return (target - direction * remaining).toFloat()
    }
    val coast = minSpeed * (elapsed - switchAt)
    if (coast >= threshold) return targetShift
    return (target - direction * (threshold - coast)).toFloat()
}

/**
 * Note belt for the quiz surface. The render thread is the only caller.
 * [offer] matches the old slide publish: reset, or append one note and drop
 * whatever has scrolled off. [advance] samples [shiftAt].
 */
internal class StaffBelt<T> {
    private val belt = mutableListOf<T>()
    private var previous: List<T> = emptyList()
    private var targetShift = 0f
    private var startShift = 0f
    private var motionStartSeconds = 0.0
    private var moving = false

    var notes: List<T> = emptyList()
        private set
    var origin: Int = 0
        private set
    var highlightIndex: Int = 0
        private set
    var shift: Float = 0f
        private set

    private var brightenAbsoluteIndex: Int? = null

    /**
     * Local index of the note a correct tap just answered, or null once that
     * note has scrolled off the belt. Survives later prunes.
     */
    fun brightenLocalIndex(): Int? {
        val absolute = brightenAbsoluteIndex ?: return null
        val local = absolute - origin
        if (local !in notes.indices) return null
        return local
    }

    /** Marks the note that was current before the latest advance. */
    fun armBrighten() {
        val local = highlightIndex - 1
        brightenAbsoluteIndex = if (local >= 0) origin + local else null
    }

    fun offer(incoming: List<T>, visibleCount: Int, nowSeconds: Double, frameSeconds: Double): BeltStep {
        if (incoming == previous) return BeltStep.Unchanged
        val step = when {
            incoming.isEmpty() -> {
                belt.clear()
                targetShift = 0f
                origin = 0
                shift = 0f
                moving = false
                brightenAbsoluteIndex = null
                BeltStep.Reset
            }
            previous.isEmpty() || !isQueueAdvance(previous, incoming) -> {
                belt.clear()
                belt.addAll(incoming)
                targetShift = 0f
                origin = 0
                begin(spawnShift(visibleCount), nowSeconds, frameSeconds)
                brightenAbsoluteIndex = null
                BeltStep.Reset
            }
            else -> {
                belt.add(incoming.last())
                targetShift += 1f
                origin = prune(visibleCount)
                begin(shift, nowSeconds, frameSeconds)
                BeltStep.Advanced
            }
        }
        notes = belt.toList()
        val local = (targetShift - origin).roundToInt()
        highlightIndex = local.coerceIn(0, (belt.size - 1).coerceAtLeast(0))
        previous = incoming
        return step
    }

    fun advance(nowSeconds: Double, followTimeSeconds: Float, minSpeedSlotsPerSecond: Float) {
        if (!moving) return
        val elapsed = (nowSeconds - motionStartSeconds).toFloat()
        val next = shiftAt(elapsed, startShift, targetShift, followTimeSeconds, minSpeedSlotsPerSecond)
        shift = next
        if (next == targetShift) moving = false
    }

    private fun begin(from: Float, nowSeconds: Double, frameSeconds: Double) {
        startShift = from
        shift = from
        motionStartSeconds = nowSeconds - frameSeconds
        moving = from != targetShift
        if (!moving) shift = targetShift
    }

    private fun prune(visibleCount: Int): Int {
        val dropped = dropCount(shift, origin, belt.size, visibleCount)
        repeat(dropped) { belt.removeAt(0) }
        return origin + dropped
    }
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
