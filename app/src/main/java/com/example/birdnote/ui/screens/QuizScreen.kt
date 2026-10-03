package com.example.birdnote.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import com.example.birdnote.R
import com.example.birdnote.domain.CHORD_VISIBLE_SLOTS
import com.example.birdnote.domain.ClefMode
import com.example.birdnote.domain.ChordConfig
import com.example.birdnote.domain.ChordQuality
import com.example.birdnote.domain.IntervalConfig
import com.example.birdnote.domain.IntervalName
import com.example.birdnote.domain.NoteName
import com.example.birdnote.domain.NoteNaming
import com.example.birdnote.domain.PracticeChord
import com.example.birdnote.domain.PracticeConfig
import com.example.birdnote.domain.QUEUE_SIZE
import com.example.birdnote.domain.StaffInterval
import com.example.birdnote.domain.StaffNote
import com.example.birdnote.domain.chordQualitiesFor
import com.example.birdnote.domain.intervalNamesFor
import com.example.birdnote.domain.label
import com.example.birdnote.domain.queueSize
import com.example.birdnote.ui.LayoutTuning
import com.example.birdnote.ui.quiz.AnswerFeedback
import com.example.birdnote.ui.quiz.ChordQuizViewModel
import com.example.birdnote.ui.quiz.IntervalQuizViewModel
import com.example.birdnote.ui.quiz.QuizSounds
import com.example.birdnote.ui.quiz.QuizViewModel
import com.example.birdnote.ui.quiz.SOUNDS_ENABLED
import com.example.birdnote.ui.staff.StaffSurface
import com.example.birdnote.ui.staff.asChord
import com.example.birdnote.ui.theme.DarkBlue
import com.example.birdnote.ui.theme.LightBlue
import com.example.birdnote.ui.theme.Neutral

@Composable
fun QuizRoute(
    config: PracticeConfig,
    noteNaming: NoteNaming,
    onFinished: (score: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel = viewModel(factory = QuizViewModel.factory(config)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sounds = rememberQuizSounds()
    var didNavigate by remember { mutableStateOf(false) }
    LaunchedEffect(state.finished) {
        if (state.finished && !didNavigate) {
            didNavigate = true
            onFinished(state.score)
        }
    }
    if (sounds != null) {
        LaunchedEffect(state.answerFeedback?.id) {
            state.answerFeedback?.let(sounds::play)
        }
    }
    QuizScreen(
        notes = state.notes,
        score = state.score,
        answerFeedback = state.answerFeedback,
        remainingMillis = viewModel.remainingMillis,
        durationMillis = state.durationMillis,
        answersLocked = state.answersLocked,
        clefMode = config.clefMode,
        difficulty = config.difficulty,
        noteNaming = noteNaming,
        onAnswer = viewModel::onAnswer,
        onStop = viewModel::stop,
        modifier = modifier,
    )
}

@Composable
fun QuizScreen(
    notes: List<StaffNote>,
    score: Int,
    answerFeedback: AnswerFeedback?,
    remainingMillis: StateFlow<Long>,
    durationMillis: Long,
    answersLocked: Boolean,
    clefMode: ClefMode,
    difficulty: Int,
    noteNaming: NoteNaming,
    onAnswer: (NoteName) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Quiz
    val visibleCount = clefMode.queueSize()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightBlue)
            .padding(
                horizontal = tuning.horizontalPadding,
                vertical = tuning.verticalPadding,
            ),
    ) {
        QuizTopBar(
            score = score,
            remainingMillis = remainingMillis,
            durationMillis = durationMillis,
            onStop = onStop,
        )
        Spacer(Modifier.height(tuning.staffVerticalGap))
        Card(
            shape = RoundedCornerShape(tuning.staffCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Neutral),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            StaffSurface(
                notes = notes,
                toChords = { queued -> queued.map { it.asChord() } },
                clefMode = clefMode,
                difficulty = difficulty,
                visibleCount = visibleCount,
                followTimeMillis = tuning.noteFollowTimeMillis,
                minSpeedSlotsPerSecond = tuning.noteFollowMinSpeedSlotsPerSecond,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(tuning.staffInnerPadding),
            )
        }
        Spacer(Modifier.height(tuning.staffVerticalGap))
        AnswerRow(
            answersLocked = answersLocked,
            noteNaming = noteNaming,
            answerFeedback = answerFeedback,
            onAnswer = onAnswer,
        )
    }
}

@Composable
fun IntervalQuizRoute(
    config: IntervalConfig,
    onFinished: (score: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IntervalQuizViewModel = viewModel(factory = IntervalQuizViewModel.factory(config)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sounds = rememberQuizSounds()
    var didNavigate by remember { mutableStateOf(false) }
    LaunchedEffect(state.finished) {
        if (state.finished && !didNavigate) {
            didNavigate = true
            onFinished(state.score)
        }
    }
    if (sounds != null) {
        LaunchedEffect(state.answerFeedback?.id) {
            state.answerFeedback?.let(sounds::play)
        }
    }
    IntervalQuizScreen(
        intervals = state.intervals,
        score = state.score,
        answerFeedback = state.answerFeedback,
        remainingMillis = viewModel.remainingMillis,
        durationMillis = state.durationMillis,
        answersLocked = state.answersLocked,
        difficulty = config.difficulty,
        onAnswer = viewModel::onAnswer,
        onStop = viewModel::stop,
        modifier = modifier,
    )
}

@Composable
fun IntervalQuizScreen(
    intervals: List<StaffInterval>,
    score: Int,
    answerFeedback: AnswerFeedback?,
    remainingMillis: StateFlow<Long>,
    durationMillis: Long,
    answersLocked: Boolean,
    difficulty: Int,
    onAnswer: (IntervalName) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Quiz
    val visibleCount = QUEUE_SIZE

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightBlue)
            .padding(
                horizontal = tuning.horizontalPadding,
                vertical = tuning.verticalPadding,
            ),
    ) {
        QuizTopBar(
            score = score,
            remainingMillis = remainingMillis,
            durationMillis = durationMillis,
            onStop = onStop,
        )
        Spacer(Modifier.height(tuning.staffVerticalGap))
        Card(
            shape = RoundedCornerShape(tuning.staffCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Neutral),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            StaffSurface(
                notes = intervals,
                toChords = { queued -> queued.map { it.asChord() } },
                clefMode = ClefMode.SOL,
                difficulty = difficulty,
                visibleCount = visibleCount,
                followTimeMillis = tuning.noteFollowTimeMillis,
                minSpeedSlotsPerSecond = tuning.noteFollowMinSpeedSlotsPerSecond,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(tuning.staffInnerPadding),
            )
        }
        Spacer(Modifier.height(tuning.staffVerticalGap))
        IntervalAnswerGrid(
            difficulty = difficulty,
            answersLocked = answersLocked,
            answerFeedback = answerFeedback,
            onAnswer = onAnswer,
        )
    }
}

@Composable
fun ChordQuizRoute(
    config: ChordConfig,
    onFinished: (score: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChordQuizViewModel = viewModel(factory = ChordQuizViewModel.factory(config)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sounds = rememberQuizSounds()
    var didNavigate by remember { mutableStateOf(false) }
    LaunchedEffect(state.finished) {
        if (state.finished && !didNavigate) {
            didNavigate = true
            onFinished(state.score)
        }
    }
    if (sounds != null) {
        LaunchedEffect(state.answerFeedback?.id) {
            state.answerFeedback?.let(sounds::play)
        }
    }
    ChordQuizScreen(
        chords = state.chords,
        score = state.score,
        answerFeedback = state.answerFeedback,
        remainingMillis = viewModel.remainingMillis,
        durationMillis = state.durationMillis,
        answersLocked = state.answersLocked,
        difficulty = config.difficulty,
        clefMode = config.clefMode,
        onAnswer = viewModel::onAnswer,
        onStop = viewModel::stop,
        modifier = modifier,
    )
}

@Composable
fun ChordQuizScreen(
    chords: List<PracticeChord>,
    score: Int,
    answerFeedback: AnswerFeedback?,
    remainingMillis: StateFlow<Long>,
    durationMillis: Long,
    answersLocked: Boolean,
    difficulty: Int,
    clefMode: ClefMode,
    onAnswer: (ChordQuality) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Quiz
    val visibleCount = CHORD_VISIBLE_SLOTS

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LightBlue)
            .padding(
                horizontal = tuning.horizontalPadding,
                vertical = tuning.verticalPadding,
            ),
    ) {
        QuizTopBar(
            score = score,
            remainingMillis = remainingMillis,
            durationMillis = durationMillis,
            onStop = onStop,
        )
        Spacer(Modifier.height(tuning.staffVerticalGap))
        Card(
            shape = RoundedCornerShape(tuning.staffCornerRadius),
            colors = CardDefaults.cardColors(containerColor = Neutral),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            StaffSurface(
                notes = chords,
                toChords = { queued -> queued.map { it.asChord() } },
                clefMode = clefMode,
                difficulty = difficulty,
                visibleCount = visibleCount,
                followTimeMillis = tuning.noteFollowTimeMillis,
                minSpeedSlotsPerSecond = tuning.noteFollowMinSpeedSlotsPerSecond,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(tuning.staffInnerPadding),
            )
        }
        Spacer(Modifier.height(tuning.staffVerticalGap))
        ChordAnswerGrid(
            difficulty = difficulty,
            answersLocked = answersLocked,
            answerFeedback = answerFeedback,
            onAnswer = onAnswer,
        )
    }
}

@Composable
private fun QuizTopBar(
    score: Int,
    remainingMillis: StateFlow<Long>,
    durationMillis: Long,
    onStop: () -> Unit,
) {
    val remaining by remainingMillis.collectAsStateWithLifecycle()
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.Quiz
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onStop,
            shape = RoundedCornerShape(commonTuning.buttonCornerRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkBlue,
                contentColor = Neutral,
            ),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = tuning.stopButtonHorizontalPadding,
            ),
            modifier = Modifier.height(tuning.stopButtonHeight),
        ) {
            Text(
                text = stringResource(R.string.stop),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        val timeRemainingLabel = stringResource(R.string.time_remaining)
        val remainingFraction = if (durationMillis <= 0L) {
            0f
        } else {
            (remaining.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
        }
        Spacer(Modifier.width(tuning.progressBarGap))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(tuning.progressBarHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(Neutral)
                .semantics { contentDescription = timeRemainingLabel },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(remainingFraction)
                    .background(DarkBlue),
            )
        }
        Spacer(Modifier.width(tuning.progressBarGap))
        QuizScore(score)
    }
}

@Composable
private fun QuizScore(score: Int) {
    val scale = remember { Animatable(1f) }
    var skipInitial by remember { mutableStateOf(true) }
    LaunchedEffect(score) {
        if (skipInitial) {
            skipInitial = false
            return@LaunchedEffect
        }
        scale.snapTo(1f)
        scale.animateTo(
            LayoutTuning.Quiz.scorePopScale,
            tween(durationMillis = SCORE_POP_UP_MILLIS, easing = FastOutSlowInEasing),
        )
        scale.animateTo(
            1f,
            tween(durationMillis = SCORE_POP_DOWN_MILLIS, easing = FastOutSlowInEasing),
        )
    }
    val label = stringResource(R.string.quiz_score, score)
    val pop = scale.value
    Text(
        text = score.toString(),
        style = MaterialTheme.typography.titleMedium,
        color = DarkBlue,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .widthIn(min = LayoutTuning.Quiz.scoreMinWidth)
            .graphicsLayer {
                scaleX = pop
                scaleY = pop
            }
            .semantics { contentDescription = label },
    )
}

@Composable
private fun AnswerRow(
    answersLocked: Boolean,
    noteNaming: NoteNaming,
    answerFeedback: AnswerFeedback?,
    onAnswer: (NoteName) -> Unit,
) {
    val tuning = LayoutTuning.Quiz
    var tapped by remember { mutableStateOf<NoteName?>(null) }
    val shaken = rememberShakenButton(answerFeedback, tapped)
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        NoteName.entries.forEach { name ->
            QuizAnswerButton(
                text = name.label(noteNaming),
                textStyle = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                answersLocked = answersLocked,
                shakeToken = shakeToken(shaken, name),
                onClick = {
                    tapped = name
                    onAnswer(name)
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = tuning.answerHorizontalMargin)
                    .height(tuning.answerButtonHeight),
            )
        }
    }
}

@Composable
private fun IntervalAnswerGrid(
    difficulty: Int,
    answersLocked: Boolean,
    answerFeedback: AnswerFeedback?,
    onAnswer: (IntervalName) -> Unit,
) {
    val tuning = LayoutTuning.Quiz
    val answers = intervalNamesFor(difficulty)
    val columns = tuning.intervalAnswerColumns
    var tapped by remember { mutableStateOf<IntervalName?>(null) }
    val shaken = rememberShakenButton(answerFeedback, tapped)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tuning.answerRowGap),
    ) {
        answers.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { name ->
                    QuizAnswerButton(
                        text = name.label(),
                        textStyle = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        answersLocked = answersLocked,
                        shakeToken = shakeToken(shaken, name),
                        onClick = {
                            tapped = name
                            onAnswer(name)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = tuning.answerHorizontalMargin)
                            .height(tuning.answerButtonHeight),
                    )
                }
                repeat(columns - rowItems.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = tuning.answerHorizontalMargin),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChordAnswerGrid(
    difficulty: Int,
    answersLocked: Boolean,
    answerFeedback: AnswerFeedback?,
    onAnswer: (ChordQuality) -> Unit,
) {
    val tuning = LayoutTuning.Quiz
    val answers = chordQualitiesFor(difficulty)
    val columns = chordAnswerColumns(answers.size)
    var tapped by remember { mutableStateOf<ChordQuality?>(null) }
    val shaken = rememberShakenButton(answerFeedback, tapped)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tuning.answerRowGap),
    ) {
        answers.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { quality ->
                    QuizAnswerButton(
                        text = quality.label(),
                        textStyle = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        answersLocked = answersLocked,
                        shakeToken = shakeToken(shaken, quality),
                        onClick = {
                            tapped = quality
                            onAnswer(quality)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = tuning.answerHorizontalMargin)
                            .height(tuning.answerButtonHeight),
                    )
                }
                repeat(columns - rowItems.size) {
                    Spacer(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = tuning.answerHorizontalMargin),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberQuizSounds(): QuizSounds? {
    if (!SOUNDS_ENABLED) return null
    val context = LocalContext.current
    val sounds = remember(context) { QuizSounds(context.assets) }
    DisposableEffect(sounds) {
        onDispose { sounds.release() }
    }
    return sounds
}

@Composable
private fun QuizAnswerButton(
    text: String,
    textStyle: TextStyle,
    maxLines: Int,
    answersLocked: Boolean,
    shakeToken: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shakeX = rememberShakeTranslation(shakeToken)
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.Quiz
    Button(
        onClick = onClick,
        enabled = !answersLocked,
        shape = RoundedCornerShape(commonTuning.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkBlue,
            contentColor = Neutral,
            disabledContainerColor = DarkBlue.copy(alpha = tuning.answerDisabledAlpha),
            disabledContentColor = Neutral,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = tuning.answerContentHorizontalPadding,
        ),
        modifier = modifier.graphicsLayer { translationX = shakeX },
    ) {
        Text(
            text = text,
            style = textStyle,
            textAlign = TextAlign.Center,
            maxLines = maxLines,
        )
    }
}

@Composable
private fun rememberShakeTranslation(shakeToken: Int): Float {
    val distancePx = with(LocalDensity.current) { LayoutTuning.Quiz.wrongShakeDistance.toPx() }
    val fraction = remember { Animatable(0f) }
    LaunchedEffect(shakeToken) {
        if (shakeToken == 0) {
            fraction.snapTo(0f)
            return@LaunchedEffect
        }
        val peaks = floatArrayOf(1f, -0.75f, 0.4f, -0.15f, 0f)
        for (peak in peaks) {
            fraction.animateTo(peak, tween(durationMillis = SHAKE_STEP_MILLIS, easing = LinearEasing))
        }
    }
    return fraction.value * distancePx
}

private fun <T> shakeToken(shaken: Pair<T, Int>?, button: T): Int =
    if (shaken != null && shaken.first == button) shaken.second else 0

@Composable
private fun <T> rememberShakenButton(feedback: AnswerFeedback?, tapped: T?): Pair<T, Int>? {
    var shaken by remember { mutableStateOf<Pair<T, Int>?>(null) }
    LaunchedEffect(feedback?.id) {
        val answer = feedback ?: return@LaunchedEffect
        val button = tapped ?: return@LaunchedEffect
        if (!answer.correct) shaken = button to answer.id
    }
    return shaken
}

private fun chordAnswerColumns(count: Int): Int {
    val maxRows = LayoutTuning.Quiz.chordAnswerMaxRows
    if (count <= maxRows) return count.coerceAtLeast(1)
    return (count + maxRows - 1) / maxRows
}

private const val SCORE_POP_UP_MILLIS = 90
private const val SCORE_POP_DOWN_MILLIS = 160
private const val SHAKE_STEP_MILLIS = 42
