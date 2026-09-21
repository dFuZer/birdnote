package com.example.birdnote.ui.quiz

import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import com.example.birdnote.domain.Pitch
import com.example.birdnote.domain.mixPcm
import com.example.birdnote.domain.parseWavPcm
import com.example.birdnote.domain.pianoAssetPath
import com.example.birdnote.domain.pitchShift
import com.example.birdnote.domain.semitones

class QuizSounds(assets: AssetManager) {
    private val audioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val pool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(audioAttributes)
        .build()

    private val descriptors = mutableListOf<AssetFileDescriptor>()
    private val notes = mutableMapOf<Int, ShortArray>()
    private var sampleRate = 44_100
    private var track: AudioTrack? = null
    private var wrongId: Int? = null

    init {
        (MIN_QUIZ_STEP..MAX_QUIZ_STEP).forEach { step ->
            loadNote(assets, step)
        }
        wrongId = loadWrong(assets)
    }

    fun play(feedback: AnswerFeedback) {
        if (feedback.correct) {
            playNotes(feedback.tones)
        } else {
            stopNotes()
            val soundId = wrongId ?: return
            pool.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }

    fun release() {
        stopNotes()
        pool.release()
        descriptors.forEach { descriptor -> descriptor.close() }
        descriptors.clear()
        notes.clear()
        wrongId = null
    }

    private fun playNotes(tones: List<SoundTone>) {
        val voices = tones.mapNotNull { tone ->
            val samples = notes[tone.diatonicStep] ?: return@mapNotNull null
            pitchShift(samples, tone.accidental.semitones)
        }
        if (voices.isEmpty()) return
        val mixed = mixPcm(voices)
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

    private fun loadNote(assets: AssetManager, step: Int) {
        val bytes = runCatching {
            assets.open(Pitch(step).pianoAssetPath()).use { it.readBytes() }
        }.getOrNull() ?: return
        val pcm = runCatching { parseWavPcm(bytes) }.getOrNull() ?: return
        sampleRate = pcm.sampleRate
        notes[step] = pcm.samples
    }

    private fun loadWrong(assets: AssetManager): Int? {
        val descriptor = runCatching { assets.openFd(WRONG_ASSET) }.getOrNull() ?: return null
        descriptors += descriptor
        return pool.load(descriptor, 1)
    }

    private companion object {
        const val MIN_QUIZ_STEP = 14
        const val MAX_QUIZ_STEP = 42
        const val WRONG_ASSET = "notes/wrong.mp3"
    }
}
