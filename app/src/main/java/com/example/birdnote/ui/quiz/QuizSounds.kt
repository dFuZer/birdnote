package com.example.birdnote.ui.quiz

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import android.os.Handler
import android.os.Looper
import com.example.birdnote.domain.Pitch
import com.example.birdnote.domain.decodeVorbisPcm
import com.example.birdnote.domain.mixPcm
import com.example.birdnote.domain.pianoAssetPath
import com.example.birdnote.domain.pitchShift
import com.example.birdnote.domain.semitones
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Master switch for quiz audio. Flip this while comparing lag.
 *
 * When false, quiz routes never construct [QuizSounds], so there is no worker
 * thread, [SoundPool], Vorbis decode, PCM cache, or [AudioTrack].
 */
const val SOUNDS_ENABLED = false

class QuizSounds(assets: AssetManager) {
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "QuizSounds")
    }
    private val playGeneration = AtomicInteger()
    private val released = AtomicBoolean(false)

    private val pool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(audioAttributes)
        .build()
    private val descriptors = mutableListOf<AssetFileDescriptor>()
    private val notes = ConcurrentHashMap<Int, Map<Int, ShortArray>>()
    @Volatile private var sampleRate = DEFAULT_SAMPLE_RATE
    @Volatile private var wrongId: Int? = null

    // Created, written, played, and released only on the main thread.
    private var track: AudioTrack? = null

    init {
        worker.execute { load(assets) }
    }

    fun play(feedback: AnswerFeedback) {
        if (worker.isShutdown || released.get()) return
        val generation = playGeneration.incrementAndGet()
        worker.execute {
            if (playGeneration.get() != generation || released.get()) return@execute
            if (feedback.correct) {
                val mixed = mixTones(feedback.tones) ?: return@execute
                mainHandler.post {
                    if (playGeneration.get() != generation || released.get()) return@post
                    playPcm(mixed)
                }
            } else {
                mainHandler.post {
                    if (playGeneration.get() != generation || released.get()) return@post
                    stopNotes()
                    wrongId?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
                }
            }
        }
    }

    fun release() {
        released.set(true)
        playGeneration.incrementAndGet()
        worker.execute {
            descriptors.forEach { descriptor -> descriptor.close() }
            descriptors.clear()
            notes.clear()
            wrongId = null
        }
        worker.shutdown()
        mainHandler.post { cleanupTrackAndPool() }
        runCatching { worker.awaitTermination(2, TimeUnit.SECONDS) }
    }

    private fun load(assets: AssetManager) {
        if (released.get()) return
        // SoundPool loads asynchronously, so start the error sound before the notes.
        wrongId = loadWrong(assets)
        for (step in MIN_QUIZ_STEP..MAX_QUIZ_STEP) {
            if (released.get()) return
            val pcm = runCatching {
                assets.open(Pitch(step).pianoAssetPath()).use { decodeVorbisPcm(it.readBytes()) }
            }.getOrNull() ?: continue
            sampleRate = pcm.sampleRate
            notes[step] = SHIFTS.associateWith { shift -> pitchShift(pcm.samples, shift) }
        }
    }

    private fun mixTones(tones: List<SoundTone>): ShortArray? {
        val voices = tones.mapNotNull { tone ->
            notes[tone.diatonicStep]?.get(tone.accidental.semitones)
        }
        if (voices.isEmpty()) return null
        return mixPcm(voices)
    }

    private fun playPcm(mixed: ShortArray) {
        stopNotes()
        val minBytes = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        val minFrames = if (minBytes > 0) minBytes / Short.SIZE_BYTES else 0
        val pcm = if (mixed.size >= minFrames) mixed else mixed.copyOf(minFrames)
        val nextTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * Short.SIZE_BYTES)
            .build()
        nextTrack.write(pcm, 0, pcm.size)
        nextTrack.play()
        track = nextTrack
    }

    private fun stopNotes() {
        val current = track ?: return
        track = null
        runCatching { current.stop() }
        current.release()
    }

    private fun cleanupTrackAndPool() {
        stopNotes()
        pool.release()
    }

    private fun loadWrong(assets: AssetManager): Int? {
        val descriptor = runCatching { assets.openFd(WRONG_ASSET) }.getOrNull() ?: return null
        descriptors += descriptor
        return pool.load(descriptor, 1)
    }

    private companion object {
        const val MIN_QUIZ_STEP = 14
        const val MAX_QUIZ_STEP = 42
        const val DEFAULT_SAMPLE_RATE = 44_100
        const val WRONG_ASSET = "notes/wrong.mp3"
        val SHIFTS = listOf(-1, 0, 1)
    }
}
