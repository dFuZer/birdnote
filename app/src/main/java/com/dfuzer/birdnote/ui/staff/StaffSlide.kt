package com.dfuzer.birdnote.ui.staff

import com.dfuzer.birdnote.domain.isQueueAdvance
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sign

/** Slots the belt is shifted left of absolute index 0. */
internal fun slideOffsetSlots(shift: Float, origin: Int): Float = shift - origin

/** Shift where a fresh belt is drawn, one slot short of resting at 0 so it slides in. */
internal fun spawnShift(visibleCount: Int): Float =
    1f - visibleCount.coerceAtLeast(1).toFloat()

/**
 * Position after [elapsedSeconds] on the follow curve.
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

    fun offer(incoming: List<T>, visibleCount: Int, nowSeconds: Double, frameSeconds: Double) {
        if (incoming == previous) return
        when {
            incoming.isEmpty() -> {
                belt.clear()
                targetShift = 0f
                origin = 0
                shift = 0f
                moving = false
            }
            previous.isEmpty() || !isQueueAdvance(previous, incoming) -> {
                belt.clear()
                belt.addAll(incoming)
                targetShift = 0f
                origin = 0
                begin(spawnShift(visibleCount), nowSeconds, frameSeconds)
            }
            else -> {
                belt.add(incoming.last())
                targetShift += 1f
                origin = prune(visibleCount)
                begin(shift, nowSeconds, frameSeconds)
            }
        }
        notes = belt.toList()
        val local = (targetShift - origin).roundToInt()
        highlightIndex = local.coerceIn(0, (belt.size - 1).coerceAtLeast(0))
        previous = incoming
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
