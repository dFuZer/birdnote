package com.example.birdnote.ui.staff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
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

data class StaffSlide<T>(
    val notes: List<T>,
    val shift: Float,
    val highlightIndex: Int,
)

private const val SETTLE_EPSILON = 0.001f

@Composable
fun <T> rememberStaffSlide(
    notes: List<T>,
    visibleCount: Int,
    followTimeMillis: Int,
    minSpeedSlotsPerSecond: Float,
): StaffSlide<T> {
    val belt = remember { mutableStateListOf<T>() }
    var previousNotes by remember { mutableStateOf<List<T>>(emptyList()) }
    var targetShift by remember { mutableFloatStateOf(0f) }
    var displayShift by remember { mutableFloatStateOf(0f) }

    fun spawnShift(): Float = 1f - visibleCount.coerceAtLeast(1).toFloat()

    LaunchedEffect(notes, visibleCount) {
        when {
            notes.isEmpty() -> {
                belt.clear()
                targetShift = 0f
                displayShift = 0f
            }
            previousNotes.isEmpty() || !isQueueAdvance(previousNotes, notes) -> {
                belt.clear()
                belt.addAll(notes)
                targetShift = 0f
                displayShift = spawnShift()
            }
            else -> {
                belt.add(notes.last())
                targetShift += 1f
            }
        }
        previousNotes = notes
    }

    LaunchedEffect(followTimeMillis, minSpeedSlotsPerSecond) {
        val tauSeconds = followTimeMillis.coerceAtLeast(1) / 1_000f
        while (true) {
            snapshotFlow { abs(targetShift - displayShift) }
                .first { it > SETTLE_EPSILON }
            var lastTime = 0L
            while (abs(targetShift - displayShift) > SETTLE_EPSILON) {
                withFrameNanos { time ->
                    if (lastTime == 0L) {
                        lastTime = time
                        return@withFrameNanos
                    }
                    val dt = ((time - lastTime) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    lastTime = time
                    val remaining = targetShift - displayShift
                    val distance = abs(remaining)
                    val speed = max(minSpeedSlotsPerSecond, distance / tauSeconds)
                    val step = speed * dt
                    displayShift = if (step >= distance) {
                        targetShift
                    } else {
                        displayShift + remaining.sign * step
                    }
                }
            }
            displayShift = targetShift
            val prune = targetShift.roundToInt()
            if (prune > 0) {
                repeat(prune.coerceAtMost((belt.size - visibleCount).coerceAtLeast(0))) {
                    belt.removeAt(0)
                }
                targetShift -= prune
                displayShift -= prune
            }
        }
    }

    val highlightIndex = targetShift.roundToInt()
        .coerceIn(0, (belt.size - 1).coerceAtLeast(0))
    return StaffSlide(
        notes = belt.toList(),
        shift = displayShift,
        highlightIndex = highlightIndex,
    )
}
