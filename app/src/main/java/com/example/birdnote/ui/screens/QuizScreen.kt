package com.example.birdnote.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.birdnote.ui.quiz.ChordQuizViewModel
import com.example.birdnote.ui.quiz.IntervalQuizViewModel
import com.example.birdnote.ui.quiz.QuizSounds
import com.example.birdnote.ui.quiz.QuizViewModel
import com.example.birdnote.ui.staff.StaffCanvas
import com.example.birdnote.ui.staff.StaffRenderModel
import com.example.birdnote.ui.staff.asChord
import com.example.birdnote.ui.staff.asChords
import com.example.birdnote.ui.staff.rememberStaffSlide
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
    val context = LocalContext.current
    val sounds = remember(context) { QuizSounds(context.assets) }
    DisposableEffect(sounds) {
        onDispose { sounds.release() }
    }
    var didNavigate by remember { mutableStateOf(false) }
    LaunchedEffect(state.finished) {
        if (state.finished && !didNavigate) {
            didNavigate = true
            onFinished(state.score)
        }
    }
    LaunchedEffect(state.answerFeedback?.id) {
        state.answerFeedback?.let(sounds::play)
    }
    QuizScreen(
        notes = state.notes,
        remainingMillis = state.remainingMillis,
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
    remainingMillis: Long,
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
    val slide = rememberStaffSlide(
        notes = notes,
        visibleCount = visibleCount,
        followTimeMillis = tuning.noteFollowTimeMillis,
        minSpeedSlotsPerSecond = tuning.noteFollowMinSpeedSlotsPerSecond,
    )
    val staffModel = remember(slide.notes, slide.highlightIndex, clefMode, difficulty) {
        StaffRenderModel(
            clefMode = clefMode,
            chords = slide.notes.asChords(),
            difficulty = difficulty,
            highlightIndex = slide.highlightIndex,
        )
    }

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
            StaffCanvas(
                model = staffModel,
                slide = slide,
                visibleSlotCount = visibleCount,
                extendStaffLinesToEnd = true,
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
    val context = LocalContext.current
    val sounds = remember(context) { QuizSounds(context.assets) }
    DisposableEffect(sounds) {
        onDispose { sounds.release() }
    }
    var didNavigate by remember { mutableStateOf(false) }
    LaunchedEffect(state.finished) {
        if (state.finished && !didNavigate) {
            didNavigate = true
            onFinished(state.score)
        }
    }
    LaunchedEffect(state.answerFeedback?.id) {
        state.answerFeedback?.let(sounds::play)
    }
    IntervalQuizScreen(
        intervals = state.intervals,
        remainingMillis = state.remainingMillis,
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
    remainingMillis: Long,
    durationMillis: Long,
    answersLocked: Boolean,
    difficulty: Int,
    onAnswer: (IntervalName) -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Quiz
    val visibleCount = QUEUE_SIZE
    val slide = rememberStaffSlide(
        notes = intervals,
        visibleCount = visibleCount,
        followTimeMillis = tuning.noteFollowTimeMillis,
        minSpeedSlotsPerSecond = tuning.noteFollowMinSpeedSlotsPerSecond,
    )
    val staffModel = remember(slide.notes, slide.highlightIndex, difficulty) {
        StaffRenderModel(
            clefMode = ClefMode.SOL,
            chords = slide.notes.map { it.asChord() },
            difficulty = difficulty,
            highlightIndex = slide.highlightIndex,
        )
    }

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
            StaffCanvas(
                model = staffModel,
                slide = slide,
                visibleSlotCount = visibleCount,
                extendStaffLinesToEnd = true,
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
    val context = LocalContext.current
    val sounds = remember(context) { QuizSounds(context.assets) }
    DisposableEffect(sounds) {
        onDispose { sounds.release() }
    }
    var didNavigate by remember { mutableStateOf(false) }
    LaunchedEffect(state.finished) {
        if (state.finished && !didNavigate) {
            didNavigate = true
            onFinished(state.score)
        }
    }
    LaunchedEffect(state.answerFeedback?.id) {
        state.answerFeedback?.let(sounds::play)
    }
    ChordQuizScreen(
        chords = state.chords,
        remainingMillis = state.remainingMillis,
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
    remainingMillis: Long,
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
    val slide = rememberStaffSlide(
        notes = chords,
        visibleCount = visibleCount,
        followTimeMillis = tuning.noteFollowTimeMillis,
        minSpeedSlotsPerSecond = tuning.noteFollowMinSpeedSlotsPerSecond,
    )
    val staffModel = remember(slide.notes, slide.highlightIndex, clefMode, difficulty) {
        StaffRenderModel(
            clefMode = clefMode,
            chords = slide.notes.map { it.asChord() },
            difficulty = difficulty,
            highlightIndex = slide.highlightIndex,
        )
    }

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
            StaffCanvas(
                model = staffModel,
                slide = slide,
                visibleSlotCount = visibleCount,
                extendStaffLinesToEnd = true,
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
    remainingMillis: Long,
    durationMillis: Long,
    onStop: () -> Unit,
) {
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
            (remainingMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
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

private fun chordAnswerColumns(count: Int): Int {
    val maxRows = LayoutTuning.Quiz.chordAnswerMaxRows
    if (count <= maxRows) return count.coerceAtLeast(1)
    return (count + maxRows - 1) / maxRows
}
