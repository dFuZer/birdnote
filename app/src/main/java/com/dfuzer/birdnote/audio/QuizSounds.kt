package com.dfuzer.birdnote.audio

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import com.dfuzer.birdnote.domain.Pitch
import com.dfuzer.birdnote.domain.chromaticSemitone
import com.dfuzer.birdnote.domain.pianoAssetPath
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class QuizSounds(private val assets: AssetManager) {
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val player = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, PLAY_THREAD)
    }
    private val playGeneration = AtomicInteger()
    private val released = AtomicBoolean(false)

    private val descriptors = mutableListOf<AssetFileDescriptor>()
    private val notes = ConcurrentHashMap<Int, ShortArray>()
    private val naturalReady = ConcurrentHashMap<Int, CountDownLatch>()
    private val urgentSteps = LinkedBlockingDeque<Int>()
    private val sampleRate = AtomicInteger(DEFAULT_SAMPLE_RATE)
    @Volatile private var wrongId: Int? = null
    @Volatile private var pool: SoundPool? = null

    // Created, written, played, and released only on the play thread.
    private var track: AudioTrack? = null

    private val decoder = Thread(::decodeLoop, DECODE_THREAD).apply {
        isDaemon = true
        start()
    }

    init {
        player.execute { ensureTrack() }
    }

    fun play(feedback: AnswerFeedback) {
        if (player.isShutdown || released.get()) return
        val generation = playGeneration.incrementAndGet()
        player.execute {
            if (playGeneration.get() != generation || released.get()) return@execute
            if (feedback.correct) {
                val mixed = mixTones(feedback.tones) ?: return@execute
                if (playGeneration.get() != generation || released.get()) return@execute
                playPcm(mixed, generation)
            } else {
                if (playGeneration.get() != generation || released.get()) return@execute
                silenceTrack()
                wrongId?.let { id -> pool?.play(id, 1f, 1f, 1, 0, 1f) }
            }
        }
    }

    fun release() {
        if (!released.compareAndSet(false, true)) return
        playGeneration.incrementAndGet()
        naturalReady.values.forEach { it.countDown() }
        decoder.interrupt()
        if (player.isShutdown) return
        player.execute {
            releaseTrack()
            descriptors.forEach { descriptor -> runCatching { descriptor.close() } }
            descriptors.clear()
            notes.clear()
            wrongId = null
            pool?.release()
            pool = null
        }
        player.shutdown()
    }

    private fun decodeLoop() {
        try {
            if (released.get()) return
            pool = SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(audioAttributes)
                .build()
            wrongId = loadWrong()
            val remaining = ArrayDeque((MIN_QUIZ_STEP..MAX_QUIZ_STEP).toList())
            while (!released.get() && remaining.isNotEmpty()) {
                val step = urgentSteps.poll()
                if (step != null) {
                    remaining.remove(step)
                    decodeNatural(step)
                } else {
                    decodeNatural(remaining.removeFirst())
                }
            }
            if (!released.get()) fillMissingChromatics(notes)
            while (!released.get()) {
                val step = urgentSteps.poll(100, TimeUnit.MILLISECONDS) ?: continue
                decodeNatural(step)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            naturalReady.values.forEach { it.countDown() }
        }
    }

    private fun decodeNatural(step: Int) {
        val chromatic = chromaticSemitone(Pitch(step))
        if (notes[chromatic] == null && !released.get()) {
            val pcm = runCatching {
                assets.openFd(Pitch(step).pianoAssetPath()).use(::decodeVorbisPcm)
            }.getOrNull()
            if (pcm != null) {
                sampleRate.set(pcm.sampleRate)
                notes.putIfAbsent(chromatic, pcm.samples)
            }
        }
        latchFor(step).countDown()
    }

    private fun mixTones(tones: List<SoundTone>): ShortArray? {
        val voices = tones.mapNotNull { tone ->
            sample(
                chromaticSemitone(Pitch(tone.diatonicStep), tone.accidental),
            )
        }
        if (voices.isEmpty()) return null
        return mixPcm(voices)
    }

    private fun sample(chromatic: Int): ShortArray? {
        notes[chromatic]?.let { return it }
        val step = nearestNaturalStep(chromatic)
        val natural = chromaticSemitone(Pitch(step))
        if (notes[natural] == null) {
            urgentSteps.offerFirst(step)
            runCatching { latchFor(step).await(2, TimeUnit.SECONDS) }
        }
        val source = notes[natural] ?: return notes[chromatic]
        if (chromatic == natural) return source
        notes.putIfAbsent(chromatic, pitchShift(source, chromatic - natural))
        return notes[chromatic]
    }

    private fun latchFor(step: Int) =
        naturalReady.computeIfAbsent(step) { CountDownLatch(1) }

    private fun playPcm(mixed: ShortArray, generation: Int) {
        val output = ensureTrack() ?: return
        silenceTrack()
        if (playGeneration.get() != generation || released.get()) return
        output.play()
        val chunk = output.bufferSizeInFrames.coerceAtLeast(1)
        var offset = 0
        while (offset < mixed.size) {
            if (playGeneration.get() != generation || released.get()) return
            val count = minOf(chunk, mixed.size - offset)
            val written = output.write(mixed, offset, count, AudioTrack.WRITE_BLOCKING)
            if (written <= 0) return
            offset += written
        }
    }

    private fun ensureTrack(): AudioTrack? {
        if (released.get()) return null
        val rate = sampleRate.get()
        val current = track
        if (current != null &&
            current.sampleRate == rate &&
            current.state == AudioTrack.STATE_INITIALIZED
        ) {
            return current
        }
        current?.let { runCatching { it.release() } }
        track = null
        val minBytes = AudioTrack.getMinBufferSize(
            rate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBytes <= 0) return null
        val next = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBytes)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .build()
        track = next
        return next
    }

    private fun silenceTrack() {
        val current = track ?: return
        runCatching { current.pause() }
        runCatching { current.flush() }
    }

    private fun releaseTrack() {
        val current = track ?: return
        track = null
        runCatching { current.pause() }
        runCatching { current.flush() }
        runCatching { current.stop() }
        current.release()
    }

    private fun loadWrong(): Int? {
        val descriptor = runCatching { assets.openFd(WRONG_ASSET) }.getOrNull() ?: return null
        descriptors += descriptor
        return pool?.load(descriptor, 1)
    }

    private companion object {
        const val DEFAULT_SAMPLE_RATE = 44_100
        const val WRONG_ASSET = "notes/wrong.mp3"
        const val DECODE_THREAD = "QuizSoundsDecode"
        const val PLAY_THREAD = "QuizSoundsPlay"
    }
}
