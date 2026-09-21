package com.example.birdnote.domain

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt
import kotlin.math.sqrt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmTest {
    @Test
    fun parseWavPcmReadsMono16BitPcm() {
        val samples = shortArrayOf(-3, 0, 12, Short.MAX_VALUE, Short.MIN_VALUE)
        val parsed = parseWavPcm(pcmWav(samples, sampleRate = 22_050))
        assertEquals(22_050, parsed.sampleRate)
        assertArrayEquals(samples, parsed.samples)
    }

    @Test
    fun parseWavPcmSkipsUnknownChunks() {
        val samples = shortArrayOf(7, 8, 9)
        val parsed = parseWavPcm(pcmWav(samples, extraListChunk = true))
        assertArrayEquals(samples, parsed.samples)
    }

    @Test
    fun convertedPianoAssetIsMonoPcm() {
        val file = listOf(
            java.io.File("assets/notes/4C.wav"),
            java.io.File("../assets/notes/4C.wav"),
        ).first { it.exists() }
        val parsed = parseWavPcm(file.readBytes())
        assertEquals(44_100, parsed.sampleRate)
        assertTrue(parsed.samples.isNotEmpty())
    }

    @Test
    fun mixPcmReturnsEmptyForNoVoices() {
        assertEquals(0, mixPcm(emptyList()).size)
    }

    @Test
    fun mixPcmKeepsASingleVoiceUnchanged() {
        val voice = shortArrayOf(100, -20, 0)
        assertArrayEquals(voice, mixPcm(listOf(voice)))
    }

    @Test
    fun mixPcmStartsEveryVoiceAtFrameZero() {
        val lower = shortArrayOf(1_000, 500, 0)
        val upper = shortArrayOf(2_000, 0, 250)
        val mixed = mixPcm(listOf(lower, upper))
        val scale = 1.0 / sqrt(2.0)
        assertEquals((3_000 * scale).roundToInt().toShort(), mixed[0])
        assertEquals((500 * scale).roundToInt().toShort(), mixed[1])
        assertEquals((250 * scale).roundToInt().toShort(), mixed[2])
    }

    @Test
    fun mixPcmPadsShorterVoicesAndMixesAChord() {
        val root = shortArrayOf(1_200)
        val third = shortArrayOf(600, 60)
        val fifth = shortArrayOf(300, 30, 3)
        val mixed = mixPcm(listOf(root, third, fifth))
        val scale = 1.0 / sqrt(3.0)
        assertEquals(3, mixed.size)
        assertEquals((2_100 * scale).roundToInt().toShort(), mixed[0])
        assertEquals((90 * scale).roundToInt().toShort(), mixed[1])
        assertEquals((3 * scale).roundToInt().toShort(), mixed[2])
    }

    @Test
    fun mixPcmClampsOverflow() {
        val hot = ShortArray(8) { Short.MAX_VALUE }
        val mixed = mixPcm(List(8) { hot })
        assertTrue(mixed.all { it == Short.MAX_VALUE })
    }

    @Test
    fun pitchShiftOfZeroReturnsTheSameSamples() {
        val samples = shortArrayOf(1, 2, 3, 4)
        assertArrayEquals(samples, pitchShift(samples, 0))
    }

    @Test
    fun pitchShiftUpShortensTheBuffer() {
        val samples = ShortArray(120) { index -> (index * 20).toShort() }
        val sharp = pitchShift(samples, 1)
        val flat = pitchShift(samples, -1)
        assertTrue(sharp.size < samples.size)
        assertTrue(flat.size > samples.size)
    }
}

private fun pcmWav(
    samples: ShortArray,
    sampleRate: Int = 44_100,
    extraListChunk: Boolean = false,
): ByteArray {
    val dataSize = samples.size * 2
    val listSize = if (extraListChunk) 12 else 0
    val riffSize = 4 + 8 + 16 + listSize + 8 + dataSize
    val buf = ByteBuffer.allocate(8 + riffSize).order(ByteOrder.LITTLE_ENDIAN)
    buf.put("RIFF".toByteArray(Charsets.US_ASCII))
    buf.putInt(riffSize)
    buf.put("WAVE".toByteArray(Charsets.US_ASCII))
    buf.put("fmt ".toByteArray(Charsets.US_ASCII))
    buf.putInt(16)
    buf.putShort(1)
    buf.putShort(1)
    buf.putInt(sampleRate)
    buf.putInt(sampleRate * 2)
    buf.putShort(2)
    buf.putShort(16)
    if (extraListChunk) {
        buf.put("LIST".toByteArray(Charsets.US_ASCII))
        buf.putInt(4)
        buf.put("INFO".toByteArray(Charsets.US_ASCII))
    }
    buf.put("data".toByteArray(Charsets.US_ASCII))
    buf.putInt(dataSize)
    samples.forEach { sample -> buf.putShort(sample) }
    return buf.array()
}
