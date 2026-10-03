package com.dfuzer.birdnote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.domain.IntervalConfig
import com.dfuzer.birdnote.domain.MAX_DIFFICULTY
import com.dfuzer.birdnote.domain.MIN_DIFFICULTY
import com.dfuzer.birdnote.domain.intervalPitchRange
import com.dfuzer.birdnote.domain.previewIntervals
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.staff.StaffCanvas
import com.dfuzer.birdnote.ui.staff.StaffRenderModel
import com.dfuzer.birdnote.ui.staff.asChord
import com.dfuzer.birdnote.ui.theme.DarkBlue
import com.dfuzer.birdnote.ui.theme.Neutral

@Composable
fun IntervalSetupScreen(
    onStartClick: (difficulty: Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.Setup
    var difficulty by rememberSaveable { mutableIntStateOf(MIN_DIFFICULTY) }
    val config = IntervalConfig(difficulty)
    val preview = StaffRenderModel(
        clefMode = ClefMode.SOL,
        chords = previewIntervals(config).map { it.asChord() },
        difficulty = difficulty,
        ranges = mapOf(Clef.SOL to intervalPitchRange()),
    )

    DecoratedScreen(modifier = modifier) {
        BackButton(
            label = stringResource(R.string.back),
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(commonTuning.backButtonMargin),
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = tuning.contentStartPadding,
                    end = tuning.contentEndPadding,
                    top = tuning.contentTopPadding,
                    bottom = tuning.contentBottomPadding,
                ),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(tuning.columnsGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier
                        .weight(tuning.controlsWeight)
                        .widthIn(max = tuning.controlsMaxWidth),
                    verticalArrangement = Arrangement.spacedBy(tuning.controlsGap),
                ) {
                    Text(
                        text = stringResource(R.string.difficulty),
                        style = MaterialTheme.typography.titleMedium,
                        color = DarkBlue,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(tuning.difficultyGap)) {
                        (MIN_DIFFICULTY..MAX_DIFFICULTY).forEach { level ->
                            DifficultyButton(
                                selected = difficulty == level,
                                text = level.toString(),
                                onClick = { difficulty = level },
                            )
                        }
                    }
                }
                Card(
                    shape = RoundedCornerShape(tuning.previewCornerRadius),
                    colors = CardDefaults.cardColors(containerColor = Neutral),
                    modifier = Modifier
                        .weight(tuning.previewWeight)
                        .fillMaxSize(),
                ) {
                    StaffCanvas(
                        model = preview,
                        noteAreaExtraLeftPaddingInLineSpaces =
                            tuning.previewNoteAreaExtraLeftPaddingInLineSpaces,
                        easeChanges = true,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(tuning.previewInnerPadding),
                    )
                }
            }
        }
        AppButton(
            text = stringResource(R.string.lets_go),
            onClick = { onStartClick(difficulty) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = tuning.startButtonBottomMargin),
        )
    }
}
