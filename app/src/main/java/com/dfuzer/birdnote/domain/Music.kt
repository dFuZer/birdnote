package com.dfuzer.birdnote.domain

@JvmInline
value class Pitch(val diatonicStep: Int) {
    val noteName: NoteName
        get() = NoteName.entries[Math.floorMod(diatonicStep, NOTE_COUNT)]
}

enum class NoteName {
    DO, RE, MI, FA, SOL, LA, SI
}

enum class NoteNaming {
    SOLFEGE,
    ENGLISH,
    GERMAN,
    SOLFEGE_TI,
    PORTUGUESE,
    JAPANESE,
    TURKISH,
    RUSSIAN,
    ARABIC,
}

enum class Clef {
    SOL, FA, ALTO, TENOR
}

/** Menu and score-row order: Sol, Fa, Sol + Fa, then Alto and Tenor. */
enum class ClefMode {
    SOL, FA, SOL_FA, ALTO, TENOR
}

enum class Accidental {
    NONE, SHARP, FLAT, NATURAL
}

data class StaffNote(
    val pitch: Pitch,
    val clef: Clef,
    val accidental: Accidental = Accidental.NONE,
)

enum class ChordQuality {
    MAJOR,
    MINOR,
    AUGMENTED,
    DIMINISHED,
    DOMINANT_7,
    MAJOR_7,
    MINOR_7,
    HALF_DIMINISHED_7,
    DIMINISHED_7,
}

data class PracticeChord(
    val root: Pitch,
    val quality: ChordQuality,
    val clef: Clef,
    val notes: List<StaffNote>,
)

data class ChordConfig(
    val difficulty: Int,
    val clefMode: ClefMode,
) {
    init {
        require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY) {
            "Difficulty must be between $MIN_DIFFICULTY and $MAX_DIFFICULTY"
        }
    }
}

data class PracticeConfig(
    val difficulty: Int,
    val clefMode: ClefMode,
) {
    init {
        require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY) {
            "Difficulty must be between $MIN_DIFFICULTY and $MAX_DIFFICULTY"
        }
    }
}

data class IntervalConfig(
    val difficulty: Int,
) {
    init {
        require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY) {
            "Difficulty must be between $MIN_DIFFICULTY and $MAX_DIFFICULTY"
        }
    }
}

enum class IntervalName {
    SECONDE, TIERCE, QUARTE, QUINTE, SIXTE, SEPTIEME, OCTAVE, NEUVIEME
}

data class StaffInterval(
    val lower: Pitch,
    val upper: Pitch,
    val clef: Clef = Clef.SOL,
) {
    init {
        require(upper.diatonicStep > lower.diatonicStep) {
            "Upper pitch must be above lower pitch"
        }
        val distance = upper.diatonicStep - lower.diatonicStep
        require(distance in 1..IntervalName.entries.size) {
            "Unsupported interval distance: $distance"
        }
    }

    val name: IntervalName
        get() = IntervalName.entries[upper.diatonicStep - lower.diatonicStep - 1]

    val notes: List<StaffNote>
        get() = listOf(StaffNote(lower, clef), StaffNote(upper, clef))
}

const val MIN_DIFFICULTY = 1
const val MAX_DIFFICULTY = 4
const val QUEUE_SIZE = 10
const val DOUBLE_CLEF_QUEUE_SIZE = QUEUE_SIZE
const val CHORD_VISIBLE_SLOTS = 2
const val QUIZ_DURATION_SECONDS = 45
const val MISTAKE_TIME_PENALTY_SECONDS = 3
const val NOTE_COUNT = 7

fun ClefMode.queueSize(): Int = QUEUE_SIZE

fun ClefMode.clefs(): List<Clef> = when (this) {
    ClefMode.SOL -> listOf(Clef.SOL)
    ClefMode.FA -> listOf(Clef.FA)
    ClefMode.ALTO -> listOf(Clef.ALTO)
    ClefMode.TENOR -> listOf(Clef.TENOR)
    ClefMode.SOL_FA -> listOf(Clef.SOL, Clef.FA)
}

/** Setup and score rows for [preferred]. Sol + Fa is included only when both of those clefs are on. */
fun clefModesFor(preferred: Set<Clef>): List<ClefMode> {
    require(preferred.isNotEmpty()) { "At least one clef stays selected" }
    return ClefMode.entries.filter { mode -> mode.clefs().all { it in preferred } }
}

/** Turns [clef] on or off. The last selected clef stays on. */
fun withClefToggled(preferred: Set<Clef>, clef: Clef): Set<Clef> {
    require(preferred.isNotEmpty()) { "At least one clef stays selected" }
    return if (clef in preferred) {
        if (preferred.size == 1) preferred else preferred - clef
    } else {
        preferred + clef
    }
}

/** A remembered mode is kept only when it is still preferred; otherwise the first preferred mode. */
fun openingClefMode(preferred: Set<Clef>, remembered: ClefMode? = null): ClefMode {
    val modes = clefModesFor(preferred)
    return if (remembered != null && remembered in modes) remembered else modes.first()
}

fun storedPreferredClefs(value: String?): Set<Clef> {
    if (value == null) return Clef.entries.toSet()
    val parsed = value.split(',')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { token -> Clef.entries.firstOrNull { it.name == token } }
        .toSet()
    return parsed.ifEmpty { Clef.entries.toSet() }
}

fun preferredClefsStorage(clefs: Set<Clef>): String {
    require(clefs.isNotEmpty()) { "At least one clef stays selected" }
    return Clef.entries.filter { it in clefs }.joinToString(",") { it.name }
}

fun Pitch.staffStep(clef: Clef): Int = diatonicStep - clef.bottomLineStep

val Clef.bottomLineStep: Int
    get() = when (this) {
        Clef.SOL -> TREBLE_BOTTOM_LINE_STEP
        Clef.FA -> BASS_BOTTOM_LINE_STEP
        Clef.ALTO -> ALTO_BOTTOM_LINE_STEP
        Clef.TENOR -> TENOR_BOTTOM_LINE_STEP
    }

fun isCorrect(note: StaffNote, answer: NoteName): Boolean = note.pitch.noteName == answer

fun isCorrect(interval: StaffInterval, answer: IntervalName): Boolean = interval.name == answer

fun isCorrect(chord: PracticeChord, answer: ChordQuality): Boolean = chord.quality == answer

val Accidental.semitones: Int
    get() = when (this) {
        Accidental.NONE, Accidental.NATURAL -> 0
        Accidental.SHARP -> 1
        Accidental.FLAT -> -1
    }

fun chromaticSemitone(pitch: Pitch, accidental: Accidental = Accidental.NONE): Int {
    val octave = pitch.diatonicStep / NOTE_COUNT
    val pitchClass = when (pitch.noteName) {
        NoteName.DO -> 0
        NoteName.RE -> 2
        NoteName.MI -> 4
        NoteName.FA -> 5
        NoteName.SOL -> 7
        NoteName.LA -> 9
        NoteName.SI -> 11
    }
    return octave * 12 + pitchClass + accidental.semitones
}

fun StaffNote.chromaticSemitone(): Int = chromaticSemitone(pitch, accidental)

val ChordQuality.diatonicOffsets: List<Int>
    get() = if (isSeventh) listOf(0, 2, 4, 6) else listOf(0, 2, 4)

val ChordQuality.semitoneIntervals: List<Int>
    get() = when (this) {
        ChordQuality.MAJOR -> listOf(0, 4, 7)
        ChordQuality.MINOR -> listOf(0, 3, 7)
        ChordQuality.AUGMENTED -> listOf(0, 4, 8)
        ChordQuality.DIMINISHED -> listOf(0, 3, 6)
        ChordQuality.DOMINANT_7 -> listOf(0, 4, 7, 10)
        ChordQuality.MAJOR_7 -> listOf(0, 4, 7, 11)
        ChordQuality.MINOR_7 -> listOf(0, 3, 7, 10)
        ChordQuality.HALF_DIMINISHED_7 -> listOf(0, 3, 6, 10)
        ChordQuality.DIMINISHED_7 -> listOf(0, 3, 6, 9)
    }

val ChordQuality.isSeventh: Boolean
    get() = this == ChordQuality.DOMINANT_7 ||
        this == ChordQuality.MAJOR_7 ||
        this == ChordQuality.MINOR_7 ||
        this == ChordQuality.HALF_DIMINISHED_7 ||
        this == ChordQuality.DIMINISHED_7

val ChordQuality.diatonicSpan: Int
    get() = diatonicOffsets.last()

fun spellChord(root: Pitch, quality: ChordQuality, clef: Clef): PracticeChord? {
    val rootChromatic = chromaticSemitone(root)
    val notes = ArrayList<StaffNote>(quality.diatonicOffsets.size)
    quality.diatonicOffsets.zip(quality.semitoneIntervals).forEach { (offset, semitones) ->
        val pitch = Pitch(root.diatonicStep + offset)
        val natural = chromaticSemitone(pitch)
        val accidental = when (val delta = rootChromatic + semitones - natural) {
            0 -> Accidental.NONE
            1 -> Accidental.SHARP
            -1 -> Accidental.FLAT
            else -> return null
        }
        notes += StaffNote(pitch, clef, accidental)
    }
    return PracticeChord(root, quality, clef, notes)
}

val IntervalName.diatonicDistance: Int
    get() = ordinal + 1

/** A saved scheme wins. With nothing saved, names follow [language]. */
fun storedNoteNaming(value: String?, language: AppLanguage): NoteNaming =
    NoteNaming.entries.firstOrNull { it.name == value } ?: language.defaultNoteNaming()

fun AppLanguage.defaultNoteNaming(): NoteNaming = when (this) {
    AppLanguage.FRENCH,
    AppLanguage.ITALIAN,
    AppLanguage.SPANISH_LATIN_AMERICA,
    AppLanguage.INDONESIAN -> NoteNaming.SOLFEGE
    AppLanguage.PORTUGUESE_BRAZIL -> NoteNaming.PORTUGUESE
    AppLanguage.ENGLISH,
    AppLanguage.HINDI -> NoteNaming.ENGLISH
    AppLanguage.GERMAN -> NoteNaming.GERMAN
    AppLanguage.JAPANESE -> NoteNaming.JAPANESE
    AppLanguage.TURKISH -> NoteNaming.TURKISH
    AppLanguage.RUSSIAN -> NoteNaming.RUSSIAN
    AppLanguage.ARABIC -> NoteNaming.ARABIC
}

fun NoteName.label(naming: NoteNaming): String = naming.labels()[ordinal]

private fun NoteNaming.labels(): List<String> = when (this) {
    NoteNaming.SOLFEGE -> listOf("Do", "Ré", "Mi", "Fa", "Sol", "La", "Si")
    NoteNaming.ENGLISH -> listOf("C", "D", "E", "F", "G", "A", "B")
    NoteNaming.GERMAN -> listOf("C", "D", "E", "F", "G", "A", "H")
    NoteNaming.SOLFEGE_TI -> listOf("Do", "Re", "Mi", "Fa", "Sol", "La", "Ti")
    NoteNaming.PORTUGUESE -> listOf("Dó", "Ré", "Mi", "Fá", "Sol", "Lá", "Si")
    NoteNaming.JAPANESE -> listOf("ド", "レ", "ミ", "ファ", "ソ", "ラ", "シ")
    NoteNaming.TURKISH -> listOf("Do", "Re", "Mi", "Fa", "Sol", "La", "Si")
    NoteNaming.RUSSIAN -> listOf("до", "ре", "ми", "фа", "соль", "ля", "си")
    NoteNaming.ARABIC -> listOf("دو", "ري", "مي", "فا", "صول", "لا", "سي")
}

fun NoteNaming.scalePreview(): String =
    NoteName.entries.joinToString(" ") { it.label(this) }

fun Pitch.pianoAssetPath(): String {
    val octave = diatonicStep / NOTE_COUNT
    val letter = when (noteName) {
        NoteName.DO -> "C"
        NoteName.RE -> "D"
        NoteName.MI -> "E"
        NoteName.FA -> "F"
        NoteName.SOL -> "G"
        NoteName.LA -> "A"
        NoteName.SI -> "B"
    }
    return "notes/${octave}$letter.ogg"
}

private const val TREBLE_BOTTOM_LINE_STEP = 30 // E4, with C0 = 0
private const val BASS_BOTTOM_LINE_STEP = 18 // G2
private const val ALTO_BOTTOM_LINE_STEP = 24 // F3; C4 sits on the middle line
private const val TENOR_BOTTOM_LINE_STEP = 22 // D3; C4 sits on the fourth line
