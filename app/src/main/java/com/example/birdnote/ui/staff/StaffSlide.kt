package com.example.birdnote.ui.staff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import com.example.birdnote.domain.isQueueAdvance
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.flow.first

@Stable
class StaffSlide<T> {
    var notes: List<T> by mutableStateOf(emptyList())
        private set
    var highlightIndex: Int by mutableIntStateOf(0)
        private set

    /** Changes every animation frame; read it only while drawing. */
    var shift: Float by mutableFloatStateOf(0f)
        internal set

    internal fun publish(belt: List<T>, targetShift: Float) {
        notes = belt.toList()
        highlightIndex = targetShift.roundToInt().coerceIn(0, (belt.size - 1).coerceAtLeast(0))
    }
}

private const val SETTLE_EPSILON = 0.001f

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

    fun spawnShift(): Float = 1f - visibleCount.coerceAtLeast(1).toFloat()

    LaunchedEffect(notes, visibleCount) {
        when {
            notes.isEmpty() -> {
                belt.clear()
                targetShift = 0f
                slide.shift = 0f
            }
            previousNotes.isEmpty() || !isQueueAdvance(previousNotes, notes) -> {
                belt.clear()
                belt.addAll(notes)
                targetShift = 0f
                slide.shift = spawnShift()
            }
            else -> {
                belt.add(notes.last())
                targetShift += 1f
            }
        }
        slide.publish(belt, targetShift)
        previousNotes = notes
    }

    LaunchedEffect(followTimeMillis, minSpeedSlotsPerSecond) {
        val tauSeconds = followTimeMillis.coerceAtLeast(1) / 1_000f
        while (true) {
            snapshotFlow { abs(targetShift - slide.shift) }
                .first { it > SETTLE_EPSILON }
            var lastTime = 0L
            while (abs(targetShift - slide.shift) > SETTLE_EPSILON) {
                withFrameNanos { time ->
                    if (lastTime == 0L) {
                        lastTime = time
                        return@withFrameNanos
                    }
                    val dt = ((time - lastTime) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    lastTime = time
                    val remaining = targetShift - slide.shift
                    val distance = abs(remaining)
                    val speed = max(minSpeedSlotsPerSecond, distance / tauSeconds)
                    val step = speed * dt
                    slide.shift = if (step >= distance) {
                        targetShift
                    } else {
                        slide.shift + remaining.sign * step
                    }
                }
            }
            slide.shift = targetShift
            val prune = targetShift.roundToInt()
            if (prune > 0) {
                repeat(prune.coerceAtMost((belt.size - visibleCount).coerceAtLeast(0))) {
                    belt.removeAt(0)
                }
                targetShift -= prune
                slide.shift -= prune
                slide.publish(belt, targetShift)
            }
        }
    }

    return slide
}
