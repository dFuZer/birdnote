package com.dfuzer.birdnote.audio

import com.dfuzer.birdnote.domain.Pitch
import com.dfuzer.birdnote.domain.chromaticSemitone
import kotlin.math.abs
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

const val MIN_QUIZ_STEP = 14
const val MAX_QUIZ_STEP = 42

fun nearestNaturalStep(chromatic: Int): Int =
    (MIN_QUIZ_STEP..MAX_QUIZ_STEP).minBy { step ->
        abs(chromaticSemitone(Pitch(step)) - chromatic)
    }

/** Black keys get one resample; enharmonics reuse a natural or that single buffer. */
fun fillMissingChromatics(notes: MutableMap<Int, ShortArray>) {
    if (notes.isEmpty()) return
    for (chromatic in notes.keys.min()..notes.keys.max()) {
        if (chromatic in notes) continue
        val step = nearestNaturalStep(chromatic)
        val natural = chromaticSemitone(Pitch(step))
        val source = notes[natural] ?: continue
        notes[chromatic] = pitchShift(source, chromatic - natural)
    }
}
