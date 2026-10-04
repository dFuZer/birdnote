package com.dfuzer.birdnote.ui.staff

import com.dfuzer.birdnote.domain.ClefMode
import java.util.concurrent.ConcurrentLinkedQueue

internal data class StaffRenderConfig(
    val clefMode: ClefMode,
    val difficulty: Int,
    val visibleCount: Int,
    val followTimeMillis: Int,
    val minSpeedSlotsPerSecond: Float,
)

internal data class StaffUpdate<T>(val notes: List<T>, val config: StaffRenderConfig)

/** One Compose publisher; one render-thread consumer. Every distinct update is ordered. */
internal class StaffMailbox<T> {
    private val pending = ConcurrentLinkedQueue<StaffUpdate<T>>()
    @Volatile var latest: StaffUpdate<T>? = null
        private set

    fun submit(update: StaffUpdate<T>) {
        if (update == latest) return
        latest = update
        pending.add(update)
    }

    fun drainInto(out: MutableList<StaffUpdate<T>>) {
        out.clear()
        while (true) out.add(pending.poll() ?: return)
    }
}

internal data class StaffGeometryKey(
    val width: Int,
    val height: Int,
    val clefMode: ClefMode,
    val difficulty: Int,
    val visibleCount: Int,
)

internal data class StaffContentKey<T>(val notes: List<T>, val highlightIndex: Int)
