package com.dfuzer.birdnote.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.audio.AnswerFeedback
import com.dfuzer.birdnote.audio.QuizSounds
import com.dfuzer.birdnote.domain.CHORD_VISIBLE_SLOTS
import com.dfuzer.birdnote.domain.ChordConfig
import com.dfuzer.birdnote.domain.ChordQuality
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.IntervalConfig
import com.dfuzer.birdnote.domain.IntervalName
import com.dfuzer.birdnote.domain.NoteName
import com.dfuzer.birdnote.domain.NoteNaming
import com.dfuzer.birdnote.domain.PracticeChord
import com.dfuzer.birdnote.domain.PracticeConfig
import com.dfuzer.birdnote.domain.QUEUE_SIZE
import com.dfuzer.birdnote.domain.StaffInterval
import com.dfuzer.birdnote.domain.StaffNote
import com.dfuzer.birdnote.domain.chordQualitiesFor
import com.dfuzer.birdnote.domain.intervalNamesFor
import com.dfuzer.birdnote.domain.label
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.quiz.QuizViewModel
import com.dfuzer.birdnote.ui.staff.StaffChord
import com.dfuzer.birdnote.ui.staff.StaffSurface
import com.dfuzer.birdnote.ui.staff.asChord
import com.dfuzer.birdnote.ui.theme.DarkBlue
import com.dfuzer.birdnote.ui.theme.LightBlue
import com.dfuzer.birdnote.ui.theme.Neutral
import kotlinx.coroutines.flow.StateFlow

@Composable
fun QuizRoute(
    config: PracticeConfig,
    noteNaming: NoteNaming,
    onFinished: (score: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel<StaffNote, NoteName> = viewModel(factory = QuizViewModel.factory { QuizViewModel.notes(config) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    QuizEffects(state.finished, state.score, state.answerFeedback, onFinished)
    QuizScaffold(
        questions = state.questions,
        toChord = { it.asChord() },
        score = state.score,
        remainingMillis = viewModel.remainingMillis,
        durationMillis = state.durationMillis,
        clefMode = config.clefMode,
        difficulty = config.difficulty,
        visibleCount = QUEUE_SIZE,
        onStop = viewModel::stop,
        modifier = modifier,
    ) {
        AnswerRow(state.finished, noteNaming, state.answerFeedback, viewModel::onAnswer)
    }
}

@Composable
fun IntervalQuizRoute(
    config: IntervalConfig,
    onFinished: (score: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel<StaffInterval, IntervalName> = viewModel(factory = QuizViewModel.factory { QuizViewModel.intervals(config) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    QuizEffects(state.finished, state.score, state.answerFeedback, onFinished)
    QuizScaffold(
        questions = state.questions,
        toChord = { it.asChord() },
        score = state.score,
        remainingMillis = viewModel.remainingMillis,
        durationMillis = state.durationMillis,
        clefMode = ClefMode.SOL,
        difficulty = config.difficulty,
        visibleCount = QUEUE_SIZE,
        onStop = viewModel::stop,
        modifier = modifier,
    ) {
        IntervalAnswerGrid(config.difficulty, state.finished, state.answerFeedback, viewModel::onAnswer)
    }
}

@Composable
fun ChordQuizRoute(
    config: ChordConfig,
    onFinished: (score: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: QuizViewModel<PracticeChord, ChordQuality> = viewModel(factory = QuizViewModel.factory { QuizViewModel.chords(config) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    QuizEffects(state.finished, state.score, state.answerFeedback, onFinished)
    QuizScaffold(
        questions = state.questions,
        toChord = { it.asChord() },
        score = state.score,
        remainingMillis = viewModel.remainingMillis,
        durationMillis = state.durationMillis,
        clefMode = config.clefMode,
        difficulty = config.difficulty,
        visibleCount = CHORD_VISIBLE_SLOTS,
        onStop = viewModel::stop,
        modifier = modifier,
    ) {
        ChordAnswerGrid(config.difficulty, state.finished, state.answerFeedback, viewModel::onAnswer)
    }
}

@Composable
private fun QuizEffects(
    finished: Boolean,
    score: Int,
    feedback: AnswerFeedback?,
    onFinished: (Int) -> Unit,
) {
    val sounds = rememberQuizSounds()
    var didNavigate by remember { mutableStateOf(false) }
    LaunchedEffect(finished) {
        if (finished && !didNavigate) {
            didNavigate = true
            onFinished(score)
        }
    }
    LaunchedEffect(feedback?.id) {
        feedback?.let(sounds::play)
    }
}

@Composable
private fun <Question> QuizScaffold(
    questions: List<Question>,
    toChord: (Question) -> StaffChord,
    score: Int,
    remainingMillis: StateFlow<Long>,
    durationMillis: Long,
    clefMode: ClefMode,
    difficulty: Int,
    visibleCount: Int,
    onStop: () -> Unit,
    modifier: Modifier,
    answers: @Composable () -> Unit,
) {
    val tuning = LayoutTuning.Quiz

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
                notes = questions,
                toChords = { queued -> queued.map(toChord) },
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
        answers()
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
    // Do stays on the left when the rest of the screen is right-to-left.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
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
}

@Composable
private fun IntervalAnswerGrid(
    difficulty: Int,
    answersLocked: Boolean,
    answerFeedback: AnswerFeedback?,
    onAnswer: (IntervalName) -> Unit,
) {
    AnswerGrid(
        answers = intervalNamesFor(difficulty),
        columns = LayoutTuning.Quiz.intervalAnswerColumns,
        label = { it.label() },
        textStyle = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        answersLocked = answersLocked,
        answerFeedback = answerFeedback,
        onAnswer = onAnswer,
    )
}

@Composable
private fun ChordAnswerGrid(
    difficulty: Int,
    answersLocked: Boolean,
    answerFeedback: AnswerFeedback?,
    onAnswer: (ChordQuality) -> Unit,
) {
    val answers = chordQualitiesFor(difficulty)
    AnswerGrid(
        answers = answers,
        columns = chordAnswerColumns(answers.size),
        label = { it.label() },
        textStyle = MaterialTheme.typography.labelSmall,
        maxLines = 2,
        answersLocked = answersLocked,
        answerFeedback = answerFeedback,
        onAnswer = onAnswer,
    )
}

@Composable
private fun <Answer> AnswerGrid(
    answers: List<Answer>,
    columns: Int,
    label: @Composable (Answer) -> String,
    textStyle: TextStyle,
    maxLines: Int,
    answersLocked: Boolean,
    answerFeedback: AnswerFeedback?,
    onAnswer: (Answer) -> Unit,
) {
    val tuning = LayoutTuning.Quiz
    var tapped by remember { mutableStateOf<Answer?>(null) }
    val shaken = rememberShakenButton(answerFeedback, tapped)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tuning.answerRowGap),
    ) {
        answers.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { name ->
                    QuizAnswerButton(
                        text = label(name),
                        textStyle = textStyle,
                        maxLines = maxLines,
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
private fun rememberQuizSounds(): QuizSounds {
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
