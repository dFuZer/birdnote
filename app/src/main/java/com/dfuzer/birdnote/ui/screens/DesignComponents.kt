package com.dfuzer.birdnote.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.domain.ClefMode
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.theme.DarkBlue
import com.dfuzer.birdnote.ui.theme.DisabledGrey
import com.dfuzer.birdnote.ui.theme.Highlight
import com.dfuzer.birdnote.ui.theme.LightBlue
import com.dfuzer.birdnote.ui.theme.Neutral

private const val ASSET_ROOT = "file:///android_asset/"

@Composable
fun DecoratedScreen(
    modifier: Modifier = Modifier,
    homePlacement: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LightBlue)
            .clipToBounds(),
    ) {
        val shortSide = minOf(maxWidth, maxHeight)
        val background = LayoutTuning.Background
        val pianoSize = shortSide * if (homePlacement) background.pianoHomeSize else background.pianoStandardSize
        val scoreWidth = maxWidth * if (homePlacement) background.scoreHomeWidth else background.scoreStandardWidth
        val scoreHeight = maxHeight * if (homePlacement) background.scoreHomeHeight else background.scoreStandardHeight
        val noteSize = shortSide * if (homePlacement) background.noteHomeSize else background.noteStandardSize

        AsyncImage(
            model = "${ASSET_ROOT}bg-piano.svg",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(pianoSize)
                .align(Alignment.TopStart)
                .offset(
                    x = pianoSize * if (homePlacement) background.pianoHomeX else background.pianoStandardX,
                    y = pianoSize * background.pianoY,
                ),
        )
        AsyncImage(
            model = "${ASSET_ROOT}bg-score.svg",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(scoreWidth)
                .height(scoreHeight)
                .align(Alignment.CenterEnd)
                .offset(
                    x = scoreWidth * if (homePlacement) background.scoreHomeX else background.scoreStandardX,
                ),
        )
        AsyncImage(
            model = "${ASSET_ROOT}bg-note.svg",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(noteSize)
                .align(Alignment.BottomStart)
                .offset(
                    x = noteSize * if (homePlacement) background.noteHomeX else background.noteStandardX,
                    y = noteSize * background.noteY,
                ),
        )
        content()
    }
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = DarkBlue,
    textStyle: TextStyle = MaterialTheme.typography.labelLarge,
) {
    val tuning = LayoutTuning.Common
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(tuning.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Neutral,
            disabledContainerColor = DisabledGrey,
            disabledContentColor = Neutral,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = tuning.buttonElevation),
        contentPadding = PaddingValues(
            horizontal = tuning.buttonHorizontalPadding,
            vertical = tuning.buttonVerticalPadding,
        ),
        modifier = modifier
            .widthIn(min = tuning.buttonMinWidth, max = tuning.buttonMaxWidth)
            .height(tuning.buttonHeight),
    ) {
        Text(
            text = text,
            style = textStyle,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun BackButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.Common
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(tuning.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkBlue,
            contentColor = Neutral,
        ),
        contentPadding = PaddingValues(),
        modifier = modifier
            .size(tuning.backButtonSize)
            .semantics { contentDescription = label },
    ) {
        Canvas(modifier = Modifier.size(tuning.backArrowSize)) {
            val centerY = size.height / 2f
            val tipX = size.width * tuning.backArrowTipXFraction
            val tailX = size.width * tuning.backArrowTailXFraction
            val wingX = size.width * tuning.backArrowWingXFraction
            val wingOffset = size.height * tuning.backArrowWingOffsetFraction
            val strokeWidth = tuning.backArrowStrokeWidth.toPx()
            drawLine(
                color = Neutral,
                start = Offset(tipX, centerY),
                end = Offset(tailX, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Neutral,
                start = Offset(tipX, centerY),
                end = Offset(wingX, centerY - wingOffset),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Neutral,
                start = Offset(tipX, centerY),
                end = Offset(wingX, centerY + wingOffset),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun DifficultyButton(
    selected: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.Setup
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(commonTuning.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Highlight else DarkBlue,
            contentColor = Neutral,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(
            width = tuning.difficultyButtonWidth,
            height = tuning.difficultyButtonHeight,
        ),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun PreferredClefToggles(
    selected: Set<Clef>,
    onToggle: (Clef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.PreferredClefs
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(tuning.toggleGap),
    ) {
        Clef.entries.forEach { clef ->
            val on = clef in selected
            ClefToggle(
                text = clef.label(),
                selected = on,
                enabled = !on || selected.size > 1,
                onClick = { onToggle(clef) },
            )
        }
    }
}

@Composable
private fun ClefToggle(
    text: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val commonTuning = LayoutTuning.Common
    val tuning = LayoutTuning.PreferredClefs
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(commonTuning.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Highlight else DarkBlue,
            contentColor = Neutral,
            disabledContainerColor = if (selected) Highlight else DisabledGrey,
            disabledContentColor = Neutral,
        ),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .semantics { this.selected = selected }
            .size(width = tuning.toggleWidth, height = tuning.toggleHeight),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
fun Clef.label(): String = when (this) {
    Clef.SOL -> stringResource(R.string.clef_sol)
    Clef.FA -> stringResource(R.string.clef_fa)
    Clef.ALTO -> stringResource(R.string.clef_alto)
    Clef.TENOR -> stringResource(R.string.clef_tenor)
}

@Composable
fun ClefMode.label(): String = when (this) {
    ClefMode.SOL -> Clef.SOL.label()
    ClefMode.FA -> Clef.FA.label()
    ClefMode.ALTO -> Clef.ALTO.label()
    ClefMode.TENOR -> Clef.TENOR.label()
    ClefMode.SOL_FA -> stringResource(R.string.clef_sol_fa)
}
