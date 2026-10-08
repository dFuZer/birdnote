package com.dfuzer.birdnote.audio

import android.content.res.AssetFileDescriptor
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Decode packaged samples with Android's codec; no bundled third-party decoder. */
fun decodeVorbisPcm(descriptor: AssetFileDescriptor): PcmBuffer {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    var started = false
    try {
        extractor.setDataSource(descriptor.fileDescriptor, descriptor.startOffset, descriptor.length)
        val track = (0 until extractor.trackCount).first { index ->
            extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        }
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val decoder = MediaCodec.createDecoderByType(checkNotNull(format.getString(MediaFormat.KEY_MIME)))
        codec = decoder
        decoder.configure(format, null, null, 0)
        decoder.start()
        started = true
        var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        var inputFinished = false
        var outputFinished = false
        val info = MediaCodec.BufferInfo()
        val bytes = ByteArrayOutputStream()
        // A broken decoder must not occupy the audio worker indefinitely.
        val deadline = System.nanoTime() + 10_000_000_000L
        while (!outputFinished) {
            check(System.nanoTime() < deadline) { "Audio decoder timed out" }
            if (!inputFinished) {
                val index = decoder.dequeueInputBuffer(10_000)
                if (index >= 0) {
                    val input = checkNotNull(decoder.getInputBuffer(index))
                    val size = extractor.readSampleData(input, 0)
                    if (size < 0) {
                        decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputFinished = true
                    } else {
                        decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }
            val index = decoder.dequeueOutputBuffer(info, 10_000)
            if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val output = decoder.outputFormat
                rate = output.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                channels = output.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                if (output.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                    check(output.getInteger(MediaFormat.KEY_PCM_ENCODING) == android.media.AudioFormat.ENCODING_PCM_16BIT)
                }
            } else if (index >= 0) {
                try {
                    if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                        val output = checkNotNull(decoder.getOutputBuffer(index))
                        output.position(info.offset)
                        output.limit(info.offset + info.size)
                        val chunk = ByteArray(info.size)
                        output.get(chunk)
                        bytes.write(chunk)
                    }
                    outputFinished = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                } finally {
                    decoder.releaseOutputBuffer(index, false)
                }
            }
        }
        check(channels > 0)
        val pcm = ByteBuffer.wrap(bytes.toByteArray()).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val samples = ShortArray(pcm.remaining() / channels) {
            var sum = 0
            repeat(channels) { sum += pcm.get().toInt() }
            (sum / channels).toShort()
        }
        check(samples.isNotEmpty()) { "Audio sample is empty" }
        return PcmBuffer(rate, samples)
    } finally {
        if (started) runCatching { codec?.stop() }
        codec?.release()
        extractor.release()
    }
}
