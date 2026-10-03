package com.example.birdnote.domain

import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random

/**
 * Major keys in circle-of-fifths order: sharps outward from C, then flats.
 *
 * Difficulty is cumulative, so each level keeps every simpler signature:
 * 1 keeps 0–1 accidentals (C, G, F), 2 keeps 0–3, 3 keeps 0–5, and 4 keeps
 * 0–7, which is all 15 major keys. The answer is the major tonic's note name.
 */
enum class MajorKey(
    val tonic: NoteName,
    val tonicAccidental: Accidental,
    val signatureAccidental: Accidental,
    val accidentalCount: Int,
) {
    C(NoteName.DO, Accidental.NONE, Accidental.NONE, 0),
    G(NoteName.SOL, Accidental.NONE, Accidental.SHARP, 1),
    D(NoteName.RE, Accidental.NONE, Accidental.SHARP, 2),
    A(NoteName.LA, Accidental.NONE, Accidental.SHARP, 3),
    E(NoteName.MI, Accidental.NONE, Accidental.SHARP, 4),
    B(NoteName.SI, Accidental.NONE, Accidental.SHARP, 5),
    F_SHARP(NoteName.FA, Accidental.SHARP, Accidental.SHARP, 6),
    C_SHARP(NoteName.DO, Accidental.SHARP, Accidental.SHARP, 7),
    F(NoteName.FA, Accidental.NONE, Accidental.FLAT, 1),
    B_FLAT(NoteName.SI, Accidental.FLAT, Accidental.FLAT, 2),
    E_FLAT(NoteName.MI, Accidental.FLAT, Accidental.FLAT, 3),
    A_FLAT(NoteName.LA, Accidental.FLAT, Accidental.FLAT, 4),
    D_FLAT(NoteName.RE, Accidental.FLAT, Accidental.FLAT, 5),
    G_FLAT(NoteName.SOL, Accidental.FLAT, Accidental.FLAT, 6),
    C_FLAT(NoteName.DO, Accidental.FLAT, Accidental.FLAT, 7),
}

data class KeySignature(
    val key: MajorKey,
    val clef: Clef,
    val id: Int = 0,
)

data class KeySignatureConfig(
    val difficulty: Int,
    val clefMode: ClefMode,
    val key: MajorKey? = null,
) {
    init {
        require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY) {
            "Difficulty must be between $MIN_DIFFICULTY and $MAX_DIFFICULTY"
        }
        if (key != null) {
            require(key in majorKeysFor(difficulty)) {
                "$key is not available at difficulty $difficulty"
            }
        }
    }
}

/** Route token for "every key allowed at this difficulty". */
const val ALL_MAJOR_KEYS = "ALL"

/** Sharps, left to right: F C G D A E B. */
val SHARP_ORDER = listOf(
    NoteName.FA,
    NoteName.DO,
    NoteName.SOL,
    NoteName.RE,
    NoteName.LA,
    NoteName.MI,
    NoteName.SI,
)

/** Flats, left to right: B E A D G C F. */
val FLAT_ORDER = SHARP_ORDER.asReversed()

fun majorKeysFor(difficulty: Int): List<MajorKey> {
    require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY)
    val maxAccidentals = KEY_SIGNATURE_MAX_ACCIDENTALS.getValue(difficulty)
    return MajorKey.entries.filter { it.accidentalCount <= maxAccidentals }
}

fun allowedMajorKeys(config: KeySignatureConfig): List<MajorKey> =
    if (config.key == null) majorKeysFor(config.difficulty) else listOf(config.key)

fun MajorKey.signatureNotes(): List<NoteName> = when (signatureAccidental) {
    Accidental.SHARP -> SHARP_ORDER.take(accidentalCount)
    Accidental.FLAT -> FLAT_ORDER.take(accidentalCount)
    Accidental.NONE, Accidental.NATURAL -> emptyList()
}

fun MajorKey.label(naming: NoteNaming): String {
    val mark = when (tonicAccidental) {
        Accidental.SHARP -> "♯"
        Accidental.FLAT -> "♭"
        Accidental.NONE, Accidental.NATURAL -> ""
    }
    return tonic.label(naming) + mark
}

fun majorKeyFromRoute(value: String): MajorKey? =
    if (value == ALL_MAJOR_KEYS) null else MajorKey.valueOf(value)

fun routeValue(key: MajorKey?): String = key?.name ?: ALL_MAJOR_KEYS

fun isCorrect(signature: KeySignature, answer: NoteName): Boolean = signature.key.tonic == answer

fun previewKeySignatures(config: KeySignatureConfig): List<KeySignature> {
    val key = allowedMajorKeys(config).maxBy { it.accidentalCount }
    return config.clefMode.clefs().map { clef -> KeySignature(key, clef) }
}

fun generateKeySignatureQueue(
    config: KeySignatureConfig,
    size: Int = QUEUE_SIZE,
    random: Random = Random.Default,
): List<KeySignature> {
    val signatures = ArrayList<KeySignature>(size)
    repeat(size) {
        signatures += generateKeySignature(config, signatures.lastOrNull(), random)
    }
    return signatures
}

fun advanceKeySignatureQueue(
    queue: List<KeySignature>,
    config: KeySignatureConfig,
    random: Random = Random.Default,
): List<KeySignature> {
    require(queue.isNotEmpty())
    val remaining = queue.drop(1)
    return remaining + generateKeySignature(
        config,
        remaining.lastOrNull() ?: queue.last(),
        random,
    )
}

fun generateKeySignature(
    config: KeySignatureConfig,
    previous: KeySignature?,
    random: Random = Random.Default,
): KeySignature {
    val clef = nextClef(config.clefMode, previous?.clef)
    val keys = allowedMajorKeys(config)
    val candidates = keys.filter { key ->
        previous == null || key != previous.key || keys.size == 1
    }
    return KeySignature(candidates.random(random), clef, signatureIds.incrementAndGet())
}

/** Root, major third, and fifth, written in an octave that matches the clef. */
fun majorTriad(key: MajorKey, clef: Clef): List<StaffNote> {
    val root = tonicPitch(key, clef)
    val rootChromatic = chromaticSemitone(root) + key.tonicAccidental.semitones
    return MAJOR_TRIAD_SPELLING.map { (offset, semitones) ->
        val pitch = Pitch(root.diatonicStep + offset)
        val accidental = when (val delta = rootChromatic + semitones - chromaticSemitone(pitch)) {
            0 -> Accidental.NONE
            1 -> Accidental.SHARP
            -1 -> Accidental.FLAT
            else -> error("Cannot spell $key major triad ($delta)")
        }
        StaffNote(pitch, clef, accidental)
    }
}

private fun tonicPitch(key: MajorKey, clef: Clef): Pitch {
    val octave = if (clef == Clef.SOL) TREBLE_TRIAD_OCTAVE else BASS_TRIAD_OCTAVE
    return Pitch(octave * NOTE_COUNT + key.tonic.ordinal)
}

private val MAJOR_TRIAD_SPELLING = listOf(0 to 0, 2 to 4, 4 to 7)

private const val TREBLE_TRIAD_OCTAVE = 4
private const val BASS_TRIAD_OCTAVE = 3

private val signatureIds = AtomicInteger()

internal val KEY_SIGNATURE_MAX_ACCIDENTALS = mapOf(
    1 to 1,
    2 to 3,
    3 to 5,
    4 to 7,
)
