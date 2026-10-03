package com.example.birdnote.domain

enum class RoundMode {
    NORMAL,
    PRACTICE,
}

/** Taps inside this window, ending at now, are scaled up to a minute. */
const val GUESS_RATE_WINDOW_MILLIS = 2_000L

/**
 * Guesses per minute from taps that fall in the last [windowMillis] before [nowMillis].
 * The count is scaled as if that same pace lasted a full minute. Early in a round,
 * only the taps that already sit inside the window are counted.
 */
fun guessesPerMinute(
    tapTimesMillis: List<Long>,
    nowMillis: Long,
    windowMillis: Long = GUESS_RATE_WINDOW_MILLIS,
): Int {
    require(windowMillis > 0L) { "Window must be positive" }
    val earliest = nowMillis - windowMillis
    val guesses = tapTimesMillis.count { it in earliest..nowMillis }
    return (guesses * 60_000L / windowMillis).toInt()
}
