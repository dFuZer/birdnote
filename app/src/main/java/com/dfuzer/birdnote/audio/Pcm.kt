package com.dfuzer.birdnote.audio

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PcmBuffer(
    val sampleRate: Int,
    val samples: ShortArray,
)

fun pitchShift(samples: ShortArray, semitones: Int): ShortArray {
    if (semitones == 0 || samples.isEmpty()) return samples
    val ratio = 2.0.pow(semitones / 12.0)
    val newLen = (samples.size / ratio).roundToInt().coerceAtLeast(1)
    return ShortArray(newLen) { index ->
        val source = index * ratio
        val left = source.toInt().coerceIn(0, samples.lastIndex)
        val right = (left + 1).coerceAtMost(samples.lastIndex)
        val frac = source - left
        val mixed = samples[left] * (1.0 - frac) + samples[right] * frac
        mixed.roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }
}

fun mixPcm(voices: List<ShortArray>): ShortArray {
    if (voices.isEmpty()) return ShortArray(0)
    val length = voices.maxOf { it.size }
    if (length == 0) return ShortArray(0)
    val mixed = ShortArray(length)
    val scale = 1.0 / sqrt(voices.size.toDouble())
    for (index in 0 until length) {
        var sum = 0.0
        for (voice in voices) {
            if (index < voice.size) {
                sum += voice[index]
            }
        }
        mixed[index] = (sum * scale).roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }
    return mixed
}
