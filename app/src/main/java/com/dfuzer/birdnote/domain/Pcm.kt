package com.dfuzer.birdnote.domain

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class PcmBuffer(
    val sampleRate: Int,
    val samples: ShortArray,
)

fun parseWavPcm(bytes: ByteArray): PcmBuffer {
    require(bytes.size >= 44) { "WAV file is too small" }
    val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    require(readFourCc(buf) == "RIFF") { "Not a RIFF file" }
    buf.int
    require(readFourCc(buf) == "WAVE") { "Not a WAVE file" }

    var sampleRate = 0
    var channels = 0
    var bitsPerSample = 0
    var audioFormat = 0
    var samples: ShortArray? = null

    while (buf.remaining() >= 8) {
        val chunkId = readFourCc(buf)
        val chunkSize = buf.int
        require(chunkSize >= 0 && buf.remaining() >= chunkSize) { "WAV chunk is truncated" }
        val chunkEnd = buf.position() + chunkSize
        when (chunkId) {
            "fmt " -> {
                require(chunkSize >= 16) { "fmt chunk is too small" }
                audioFormat = buf.short.toInt() and 0xFFFF
                channels = buf.short.toInt() and 0xFFFF
                sampleRate = buf.int
                buf.int
                buf.short
                bitsPerSample = buf.short.toInt() and 0xFFFF
            }
            "data" -> {
                require(chunkSize % 2 == 0) { "PCM data must be 16-bit aligned" }
                val count = chunkSize / 2
                samples = ShortArray(count)
                buf.asShortBuffer().get(samples)
            }
        }
        buf.position(chunkEnd)
        if (chunkSize % 2 == 1 && buf.hasRemaining()) {
            buf.get()
        }
    }

    require(audioFormat == 1) { "WAV must be PCM" }
    require(channels == 1) { "WAV must be mono" }
    require(bitsPerSample == 16) { "WAV must be 16-bit" }
    require(sampleRate > 0) { "WAV sample rate is missing" }
    requireNotNull(samples) { "WAV data chunk is missing" }
    return PcmBuffer(sampleRate, samples)
}

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

private fun readFourCc(buf: ByteBuffer): String {
    val bytes = ByteArray(4)
    buf.get(bytes)
    return String(bytes, Charsets.US_ASCII)
}
