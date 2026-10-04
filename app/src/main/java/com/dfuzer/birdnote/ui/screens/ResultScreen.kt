package com.dfuzer.birdnote.ui.screens

import androidx.annotation.PluralsRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.BestComparison
import com.dfuzer.birdnote.domain.compareToBest
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.components.AppButton
import com.dfuzer.birdnote.ui.components.DecoratedScreen

@Composable
fun ResultScreen(
    score: Int,
    previousBest: Int?,
    onRestartClick: () -> Unit,
    onMenuClick: () -> Unit,
    modifier: Modifier = Modifier,
    @PluralsRes scorePluralRes: Int = R.plurals.score_notes,
) {
    val tuning = LayoutTuning.Result
    DecoratedScreen(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(tuning.contentPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.bravo),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.your_score),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = pluralStringResource(scorePluralRes, score, score),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = comparisonLine(compareToBest(previousBest, score)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = tuning.buttonGap, bottom = tuning.scoreBottomSpacing),
            )
            AppButton(
                text = stringResource(R.string.restart),
                onClick = onRestartClick,
            )
            AppButton(
                text = stringResource(R.string.menu),
                onClick = onMenuClick,
                modifier = Modifier.padding(top = tuning.buttonGap),
            )
        }
    }
}

@Composable
private fun comparisonLine(comparison: BestComparison): String = when (comparison) {
    BestComparison.FirstRun -> stringResource(R.string.result_first_run)
    is BestComparison.AboveBest -> stringResource(R.string.result_above_best, comparison.by)
    is BestComparison.ShortOfBest -> stringResource(R.string.result_short_of_best, comparison.by)
    BestComparison.Tie -> stringResource(R.string.result_tie)
}
