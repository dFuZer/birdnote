package com.dfuzer.birdnote.audio

import com.dfuzer.birdnote.domain.Accidental

data class SoundTone(
    val diatonicStep: Int,
    val accidental: Accidental = Accidental.NONE,
)

data class AnswerFeedback(
    val id: Int,
    val correct: Boolean,
    val tones: List<SoundTone>,
)
