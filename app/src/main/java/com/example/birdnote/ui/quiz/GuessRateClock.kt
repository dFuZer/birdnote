package com.example.birdnote.ui.quiz

import com.example.birdnote.domain.guessesPerMinute

internal class GuessRateClock {
    private val tapTimesMillis = ArrayList<Long>()
    private var elapsedMillis = 0L

    fun advance(deltaMillis: Long): Int {
        elapsedMillis += deltaMillis
        return rate()
    }

    fun recordGuess(): Int {
        tapTimesMillis += elapsedMillis
        return rate()
    }

    private fun rate(): Int = guessesPerMinute(tapTimesMillis, elapsedMillis)
}
