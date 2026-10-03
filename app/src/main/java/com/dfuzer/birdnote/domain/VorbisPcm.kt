package com.dfuzer.birdnote.domain

import com.jcraft.jogg.Packet
import com.jcraft.jogg.Page
import com.jcraft.jogg.StreamState
import com.jcraft.jogg.SyncState
import com.jcraft.jorbis.Block
import com.jcraft.jorbis.Comment
import com.jcraft.jorbis.DspState
import com.jcraft.jorbis.Info
import kotlin.math.roundToInt

/** Decodes an Ogg Vorbis file to 16-bit mono PCM. Stereo input is averaged. */
fun decodeVorbisPcm(bytes: ByteArray): PcmBuffer {
    require(bytes.isNotEmpty()) { "Vorbis file is empty" }
    val sync = SyncState()
    val stream = StreamState()
    val page = Page()
    val packet = Packet()
    val info = Info()
    val comment = Comment()
    val dsp = DspState()
    val block = Block(dsp)
    sync.init()
    try {
        val offset = sync.buffer(bytes.size)
        System.arraycopy(bytes, 0, sync.data, offset, bytes.size)
        sync.wrote(bytes.size)

        check(sync.pageout(page) == 1) { "Not an Ogg Vorbis file" }
        stream.init(page.serialno())
        info.init()
        comment.init()
        check(stream.pagein(page) >= 0) { "Bad first Ogg page" }
        check(stream.packetout(packet) == 1) { "Missing Vorbis identification header" }
        check(info.synthesis_headerin(comment, packet) >= 0) { "Not a Vorbis stream" }

        var headersLeft = 2
        while (headersLeft > 0) {
            check(sync.pageout(page) == 1) { "Truncated Vorbis headers" }
            stream.pagein(page)
            while (headersLeft > 0) {
                when (stream.packetout(packet)) {
                    0 -> break
                    -1 -> error("Corrupt Vorbis header")
                    else -> {
                        check(info.synthesis_headerin(comment, packet) >= 0) {
                            "Bad Vorbis header"
                        }
                        headersLeft--
                    }
                }
            }
        }

        val channels = info.channels
        val rate = info.rate
        check(channels == 1 || channels == 2) { "Unsupported channel count: $channels" }
        check(rate > 0) { "Vorbis sample rate is missing" }
        dsp.synthesis_init(info)
        block.init(dsp)

        val output = PcmBuilder()
        @Suppress("UNCHECKED_CAST")
        val pcmHolder = arrayOfNulls<Array<FloatArray>>(1) as Array<Array<FloatArray>>
        val pcmIndex = IntArray(channels)
        while (true) {
            val pageResult = sync.pageout(page)
            if (pageResult == 0) break
            if (pageResult < 0) continue
            stream.pagein(page)
            while (true) {
                val packetResult = stream.packetout(packet)
                if (packetResult == 0) break
                if (packetResult < 0) continue
                if (block.synthesis(packet) == 0) {
                    dsp.synthesis_blockin(block)
                }
                var count = dsp.synthesis_pcmout(pcmHolder, pcmIndex)
                while (count > 0) {
                    val pcm = pcmHolder[0]
                    for (frame in 0 until count) {
                        val sample = if (channels == 1) {
                            pcm[0][pcmIndex[0] + frame]
                        } else {
                            (pcm[0][pcmIndex[0] + frame] + pcm[1][pcmIndex[1] + frame]) * 0.5f
                        }
                        output.add(sample)
                    }
                    dsp.synthesis_read(count)
                    count = dsp.synthesis_pcmout(pcmHolder, pcmIndex)
                }
            }
        }
        val samples = output.toArray()
        check(samples.isNotEmpty()) { "Vorbis file has no samples" }
        return PcmBuffer(rate, samples)
    } finally {
        runCatching { stream.clear() }
        runCatching { block.clear() }
        runCatching { dsp.clear() }
        runCatching { info.clear() }
        runCatching { sync.clear() }
    }
}

private class PcmBuilder {
    private var data = ShortArray(4_096)
    private var size = 0

    fun add(sample: Float) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = (sample * 32_767f).roundToInt()
            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            .toShort()
    }

    fun toArray(): ShortArray = data.copyOf(size)
}
