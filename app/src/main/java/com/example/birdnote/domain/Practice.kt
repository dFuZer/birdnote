package com.example.birdnote.domain

import kotlin.random.Random

fun pitchRange(difficulty: Int, clef: Clef): IntRange {
    require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY)
    val table = if (clef == Clef.SOL) TREBLE_RANGES else BASS_RANGES
    return table.getValue(difficulty)
}

fun previewNotes(config: PracticeConfig): List<StaffNote> {
    val byClef = config.clefMode.clefs().map { clef ->
        sampleRange(pitchRange(config.difficulty, clef)).map { StaffNote(Pitch(it), clef) }
    }
    if (byClef.size == 1) return byClef.first()
    val slotCount = byClef.maxOf { it.size }
    return (0 until slotCount).flatMap { index ->
        byClef.mapNotNull { notes -> notes.getOrNull(index) }
    }
}

fun generateQueue(
    config: PracticeConfig,
    size: Int = QUEUE_SIZE,
    random: Random = Random.Default,
): List<StaffNote> {
    val notes = ArrayList<StaffNote>(size)
    repeat(size) {
        notes += generateNote(config, notes.lastOrNull(), random)
    }
    return notes
}

fun advanceQueue(
    queue: List<StaffNote>,
    config: PracticeConfig,
    random: Random = Random.Default,
): List<StaffNote> {
    require(queue.isNotEmpty())
    val remaining = queue.drop(1)
    return remaining + generateNote(config, remaining.lastOrNull() ?: queue.last(), random)
}

fun <T> isQueueAdvance(previous: List<T>, next: List<T>): Boolean =
    previous.size == next.size &&
        previous.isNotEmpty() &&
        previous.drop(1) == next.dropLast(1)

fun intervalNamesFor(difficulty: Int): List<IntervalName> {
    require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY)
    val max = INTERVAL_MAX_BY_DIFFICULTY.getValue(difficulty)
    return IntervalName.entries.filter { it.ordinal <= max.ordinal }
}

fun intervalPitchRange(): IntRange = INTERVAL_PITCH_RANGE

fun previewIntervals(config: IntervalConfig): List<StaffInterval> {
    val names = intervalNamesFor(config.difficulty)
    return listOf(
        centeredInterval(IntervalName.SECONDE),
        centeredInterval(names.last()),
    )
}

fun generateIntervalQueue(
    config: IntervalConfig,
    size: Int = QUEUE_SIZE,
    random: Random = Random.Default,
): List<StaffInterval> {
    val intervals = ArrayList<StaffInterval>(size)
    repeat(size) {
        intervals += generateInterval(config, intervals.lastOrNull(), random)
    }
    return intervals
}

fun advanceIntervalQueue(
    queue: List<StaffInterval>,
    config: IntervalConfig,
    random: Random = Random.Default,
): List<StaffInterval> {
    require(queue.isNotEmpty())
    val remaining = queue.drop(1)
    return remaining + generateInterval(config, remaining.lastOrNull() ?: queue.last(), random)
}

fun generateInterval(
    config: IntervalConfig,
    previous: StaffInterval?,
    random: Random = Random.Default,
): StaffInterval {
    val names = intervalNamesFor(config.difficulty)
    val nameCandidates = names.filter { name ->
        previous == null || name != previous.name || names.size == 1
    }
    val name = nameCandidates.random(random)
    val distance = name.diatonicDistance
    val range = INTERVAL_PITCH_RANGE
    val lowerRange = range.first..(range.last - distance)
    val lowers = lowerRange.filter { lower ->
        previous == null ||
            lower != previous.lower.diatonicStep ||
            lower + distance != previous.upper.diatonicStep ||
            lowerRange.count() == 1
    }
    val lower = lowers.random(random)
    return StaffInterval(Pitch(lower), Pitch(lower + distance), Clef.SOL)
}

private fun centeredInterval(name: IntervalName): StaffInterval {
    val distance = name.diatonicDistance
    val mid = (INTERVAL_PITCH_RANGE.first + INTERVAL_PITCH_RANGE.last) / 2
    val lower = (mid - distance / 2).coerceIn(
        INTERVAL_PITCH_RANGE.first,
        INTERVAL_PITCH_RANGE.last - distance,
    )
    return StaffInterval(Pitch(lower), Pitch(lower + distance), Clef.SOL)
}

fun generateNote(
    config: PracticeConfig,
    previous: StaffNote?,
    random: Random = Random.Default,
): StaffNote {
    val clef = nextClef(config.clefMode, previous)
    val range = pitchRange(config.difficulty, clef)
    val candidates = range.filter { step ->
        previous == null || step != previous.pitch.diatonicStep || range.count() == 1
    }
    return StaffNote(Pitch(candidates.random(random)), clef)
}

fun nextClef(mode: ClefMode, previous: StaffNote?): Clef = nextClef(mode, previous?.clef)

fun nextClef(mode: ClefMode, previousClef: Clef?): Clef = when (mode) {
    ClefMode.SOL -> Clef.SOL
    ClefMode.FA -> Clef.FA
    ClefMode.SOL_FA -> if (previousClef == Clef.SOL) Clef.FA else Clef.SOL
}

fun chordQualitiesFor(difficulty: Int): List<ChordQuality> {
    require(difficulty in MIN_DIFFICULTY..MAX_DIFFICULTY)
    return CHORD_QUALITIES_BY_DIFFICULTY.getValue(difficulty)
}

fun chordPitchRange(clef: Clef): IntRange =
    if (clef == Clef.SOL) CHORD_TREBLE_RANGE else CHORD_BASS_RANGE

fun previewChords(config: ChordConfig): List<PracticeChord> {
    val qualities = chordQualitiesFor(config.difficulty)
    var previousClef: Clef? = null
    return qualities.map { quality ->
        val clef = nextClef(config.clefMode, previousClef)
        previousClef = clef
        val chord = spellChord(Pitch(previewRootStep(clef)), quality, clef)
        requireNotNull(chord) { "Preview root cannot spell $quality on $clef" }
    }
}

fun generateChordQueue(
    config: ChordConfig,
    size: Int = QUEUE_SIZE,
    random: Random = Random.Default,
): List<PracticeChord> {
    val chords = ArrayList<PracticeChord>(size)
    repeat(size) {
        chords += generateChord(config, chords.lastOrNull(), random)
    }
    return chords
}

fun advanceChordQueue(
    queue: List<PracticeChord>,
    config: ChordConfig,
    random: Random = Random.Default,
): List<PracticeChord> {
    require(queue.isNotEmpty())
    val remaining = queue.drop(1)
    return remaining + generateChord(config, remaining.lastOrNull() ?: queue.last(), random)
}

fun generateChord(
    config: ChordConfig,
    previous: PracticeChord?,
    random: Random = Random.Default,
): PracticeChord {
    val clef = nextClef(config.clefMode, previous?.clef)
    val qualities = chordQualitiesFor(config.difficulty)
    val qualityCandidates = qualities.filter { quality ->
        previous == null || quality != previous.quality || qualities.size == 1
    }
    val quality = qualityCandidates.random(random)
    val range = chordPitchRange(clef)
    val rootRange = range.first..(range.last - quality.diatonicSpan)
    val spelled = rootRange.mapNotNull { step -> spellChord(Pitch(step), quality, clef) }
    require(spelled.isNotEmpty()) { "No spellable $quality roots on $clef" }
    val candidates = spelled.filter { chord ->
        previous == null ||
            chord.root != previous.root ||
            chord.quality != previous.quality ||
            spelled.size == 1
    }
    return (candidates.ifEmpty { spelled }).random(random)
}

internal val TREBLE_RANGES = mapOf(
    1 to 30..34, // Centered on G4, the treble-clef reference line
    2 to 28..36,
    3 to 28..40, // Expand upward only to keep mixed staves separated
    4 to 26..42, // A3–C6, two ledgers either side
)

internal val INTERVAL_MAX_BY_DIFFICULTY = mapOf(
    1 to IntervalName.QUARTE,
    2 to IntervalName.SIXTE,
    3 to IntervalName.OCTAVE,
    4 to IntervalName.NEUVIEME,
)

/** Treble, 2nd ledger below through 2nd ledger above (A3–C6). */
internal val INTERVAL_PITCH_RANGE = 26..42

internal val CHORD_TREBLE_RANGE = 26..42
internal val CHORD_BASS_RANGE = 14..30

internal val CHORD_QUALITIES_BY_DIFFICULTY = mapOf(
    1 to listOf(ChordQuality.MAJOR, ChordQuality.MINOR),
    2 to listOf(
        ChordQuality.MAJOR,
        ChordQuality.MINOR,
        ChordQuality.AUGMENTED,
        ChordQuality.DIMINISHED,
    ),
    3 to listOf(
        ChordQuality.MAJOR,
        ChordQuality.MINOR,
        ChordQuality.AUGMENTED,
        ChordQuality.DIMINISHED,
        ChordQuality.DOMINANT_7,
        ChordQuality.MAJOR_7,
        ChordQuality.MINOR_7,
    ),
    4 to ChordQuality.entries,
)

internal fun previewRootStep(clef: Clef): Int = when (clef) {
    Clef.SOL -> 32 // G4, a 7th stays on the top line
    Clef.FA -> 20 // B2
}

internal val BASS_RANGES = mapOf(
    1 to 22..26, // Centered on F3, the bass-clef reference line
    2 to 20..28,
    3 to 16..28, // Expand downward only to keep mixed staves separated
    4 to 14..30, // C2–E4, two ledgers either side
)

private fun sampleRange(range: IntRange): List<Int> {
    val first = range.first
    val last = range.last
    if (first == last) return listOf(first)
    val span = last - first
    return listOf(
        first,
        first + span / 3,
        first + 2 * span / 3,
        last,
    ).distinct()
}
