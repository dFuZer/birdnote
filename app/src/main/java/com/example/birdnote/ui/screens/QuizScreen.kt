package com.example.birdnote.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.birdnote.domain.streakActive
import com.example.birdnote.ui.LayoutTuning
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
import kotlin.math.cos
import kotlin.math.sin

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
        remainingMillis = viewModel.remainingMillis,
        durationMillis = state.durationMillis,
        answersLocked = state.answersLocked,
        clefMode = config.clefMode,
        difficulty = config.difficulty,
        noteNaming = noteNaming,
        score = state.score,
        correctInARow = state.correctInARow,
        sparklePopMillis = state.sparklePopMillis,
        onAnswer = viewModel::onAnswer,
        onStop = viewModel::stop,
        modifier = modifier,
    )
}

@Composable
fun QuizScreen(
    notes: List<StaffNote>,
    remainingMillis: StateFlow<Long>,
    durationMillis: Long,
    answersLocked: Boolean,
    clefMode: ClefMode,
    difficulty: Int,
    noteNaming: NoteNaming,
    score: Int,
    correctInARow: Int,
    sparklePopMillis: Long?,
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
            remainingMillis = remainingMillis,
            durationMillis = durationMillis,
            score = score,
            correctInARow = correctInARow,
            sparklePopMillis = sparklePopMillis,
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
        remainingMillis = viewModel.remainingMillis,
        durationMillis = state.durationMillis,
        answersLocked = state.answersLocked,
        difficulty = config.difficulty,
        score = state.score,
        correctInARow = state.correctInARow,
        sparklePopMillis = state.sparklePopMillis,
        onAnswer = viewModel::onAnswer,
        onStop = viewModel::stop,
        modifier = modifier,
    )
}

@Composable
fun IntervalQuizScreen(
    intervals: List<StaffInterval>,
    remainingMillis: StateFlow<Long>,
    durationMillis: Long,
    answersLocked: Boolean,
    difficulty: Int,
    score: Int,
    correctInARow: Int,
    sparklePopMillis: Long?,
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
            remainingMillis = remainingMillis,
            durationMillis = durationMillis,
            score = score,
            correctInARow = correctInARow,
            sparklePopMillis = sparklePopMillis,
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
        remainingMillis = viewModel.remainingMillis,
        durationMillis = state.durationMillis,
        answersLocked = state.answersLocked,
        difficulty = config.difficulty,
        clefMode = config.clefMode,
        score = state.score,
        correctInARow = state.correctInARow,
        sparklePopMillis = state.sparklePopMillis,
        onAnswer = viewModel::onAnswer,
        onStop = viewModel::stop,
        modifier = modifier,
    )
}

@Composable
fun ChordQuizScreen(
    chords: List<PracticeChord>,
    remainingMillis: StateFlow<Long>,
    durationMillis: Long,
    answersLocked: Boolean,
    difficulty: Int,
    clefMode: ClefMode,
    score: Int,
    correctInARow: Int,
    sparklePopMillis: Long?,
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
            remainingMillis = remainingMillis,
            durationMillis = durationMillis,
            score = score,
            correctInARow = correctInARow,
            sparklePopMillis = sparklePopMillis,
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
            onAnswer = onAnswer,
        )
    }
}

@Composable
private fun QuizTopBar(
    remainingMillis: StateFlow<Long>,
    durationMillis: Long,
    score: Int,
    correctInARow: Int,
    sparklePopMillis: Long?,
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
        Spacer(Modifier.width(tuning.rewardGap))
        StreakReward(
            score = score,
            correctInARow = correctInARow,
            sparklePopMillis = sparklePopMillis,
        )
    }
}

@Composable
private fun StreakReward(
    score: Int,
    correctInARow: Int,
    sparklePopMillis: Long?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StreakIndicator(correctInARow = correctInARow)
        ScoreSparkle(
            score = score,
            sparklePopMillis = sparklePopMillis,
        )
    }
}

@Composable
private fun StreakIndicator(correctInARow: Int) {
    val active = streakActive(correctInARow)
    val scale = remember { Animatable(1f) }
    val pulse = scale.value
    LaunchedEffect(correctInARow, active) {
        if (!active) {
            scale.snapTo(1f)
            return@LaunchedEffect
        }
        scale.snapTo(0.84f)
        scale.animateTo(1f, tween(durationMillis = 280, easing = FastOutSlowInEasing))
    }
    AnimatedVisibility(
        visible = active,
        enter = fadeIn(tween(200)) + expandHorizontally(
            animationSpec = tween(200),
            expandFrom = Alignment.End,
        ),
        exit = fadeOut(tween(140)) + shrinkHorizontally(
            animationSpec = tween(140),
            shrinkTowards = Alignment.End,
        ),
    ) {
        val streakLabel = stringResource(R.string.streak_indicator, correctInARow)
        Text(
            text = "×$correctInARow",
            style = MaterialTheme.typography.bodyLarge,
            color = DarkBlue,
            maxLines = 1,
            modifier = Modifier
                .padding(end = LayoutTuning.Quiz.streakLabelSpacing)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .semantics { contentDescription = streakLabel },
        )
    }
}

@Composable
private fun ScoreSparkle(
    score: Int,
    sparklePopMillis: Long?,
) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(sparklePopMillis) {
        if (sparklePopMillis == null) {
            progress.snapTo(1f)
            return@LaunchedEffect
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 480, easing = FastOutSlowInEasing))
    }
    val scoreLabel = stringResource(R.string.quiz_score, score)
    val bump = 1f + (1f - progress.value) * 0.06f
    Box(
        modifier = Modifier.size(LayoutTuning.Quiz.scoreSparkleSize),
        contentAlignment = Alignment.Center,
    ) {
        if (progress.value < 1f) {
            Canvas(Modifier.fillMaxSize()) {
                drawScoreSparkle(progress.value)
            }
        }
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = DarkBlue,
            maxLines = 1,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = bump
                    scaleY = bump
                }
                .semantics { contentDescription = scoreLabel },
        )
    }
}

private fun DrawScope.drawScoreSparkle(progress: Float) {
    val center = this.center
    val reach = size.minDimension * 0.46f
    val alpha = ((1f - progress) * 0.85f).coerceIn(0f, 0.85f)
    val color = Neutral.copy(alpha = alpha)
    drawCircle(
        color = color,
        radius = reach * (0.35f + 0.65f * progress),
        style = Stroke(width = size.minDimension * 0.04f),
    )
    repeat(4) { index ->
        val angle = Math.toRadians((index * 90.0) - 45.0)
        val distance = reach * (0.22f + 0.78f * progress)
        drawCircle(
            color = color,
            radius = size.minDimension * 0.055f,
            center = Offset(
                x = center.x + (cos(angle) * distance).toFloat(),
                y = center.y + (sin(angle) * distance).toFloat(),
            ),
        )
    }
}

@Composable
private fun AnswerRow(
    answersLocked: Boolean,
    noteNaming: NoteNaming,
    onAnswer: (NoteName) -> Unit,
) {
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.Quiz
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        NoteName.entries.forEach { name ->
            Button(
                onClick = { onAnswer(name) },
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
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = tuning.answerHorizontalMargin)
                    .height(tuning.answerButtonHeight),
            ) {
                Text(
                    text = name.label(noteNaming),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun IntervalAnswerGrid(
    difficulty: Int,
    answersLocked: Boolean,
    onAnswer: (IntervalName) -> Unit,
) {
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.Quiz
    val answers = intervalNamesFor(difficulty)
    val columns = tuning.intervalAnswerColumns
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tuning.answerRowGap),
    ) {
        answers.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { name ->
                    Button(
                        onClick = { onAnswer(name) },
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
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = tuning.answerHorizontalMargin)
                            .height(tuning.answerButtonHeight),
                    ) {
                        Text(
                            text = name.label(),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
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
    onAnswer: (ChordQuality) -> Unit,
) {
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.Quiz
    val answers = chordQualitiesFor(difficulty)
    val columns = chordAnswerColumns(answers.size)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tuning.answerRowGap),
    ) {
        answers.chunked(columns).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowItems.forEach { quality ->
                    Button(
                        onClick = { onAnswer(quality) },
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
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = tuning.answerHorizontalMargin)
                            .height(tuning.answerButtonHeight),
                    ) {
                        Text(
                            text = quality.label(),
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
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

private fun chordAnswerColumns(count: Int): Int {
    val maxRows = LayoutTuning.Quiz.chordAnswerMaxRows
    if (count <= maxRows) return count.coerceAtLeast(1)
    return (count + maxRows - 1) / maxRows
}
