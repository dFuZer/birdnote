package com.dfuzer.birdnote.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.components.AppButton
import com.dfuzer.birdnote.ui.components.BackButton
import com.dfuzer.birdnote.ui.components.DecoratedScreen
import com.dfuzer.birdnote.ui.components.FittedText
import com.dfuzer.birdnote.ui.theme.BirdTeal
import com.dfuzer.birdnote.ui.theme.DarkBlue
import com.dfuzer.birdnote.ui.theme.Neutral
import com.dfuzer.birdnote.ui.theme.StaffBlue

private const val ASSET_ROOT = "file:///android_asset/"

private val CardFill = Neutral
private val CardBorder = DarkBlue.copy(alpha = 0.14f)

@Composable
fun WelcomePromiseScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.WelcomePromise
    DecoratedScreen(modifier = modifier) {
        BackButton(
            label = stringResource(R.string.back),
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(LayoutTuning.Common.backButtonMargin),
        )
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= tuning.wideBreakpoint && maxWidth > maxHeight
            val padding = Modifier.padding(
                horizontal = tuning.contentHorizontalPadding,
                vertical = tuning.contentVerticalPadding,
            )
            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(padding),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(tuning.columnGap),
                ) {
                    PromiseIntro(Modifier.weight(1f).fillMaxHeight())
                    PromisePanel(
                        onContinue = onContinue,
                        modifier = Modifier.weight(1.1f).fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PromiseIntro(Modifier.fillMaxWidth())
                    PromisePanel(
                        onContinue = onContinue,
                        modifier = Modifier
                            .weight(1f)
                            .widthIn(max = 480.dp)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PromiseIntro(modifier: Modifier = Modifier) {
    val tuning = LayoutTuning.WelcomePromise
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.bodyLarge,
            color = BirdTeal,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(tuning.titleGap))
        Text(
            text = stringResource(R.string.welcome_promise_title),
            style = MaterialTheme.typography.headlineMedium,
            color = DarkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(tuning.titleGap + 8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val side = minOf(maxWidth * tuning.birdWidthFraction, tuning.birdMax)
            if (side > 0.dp) {
                PromiseBird(
                    boxSize = side,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun PromisePanel(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.WelcomePromise
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.welcome_promise_body),
            style = MaterialTheme.typography.bodyLarge,
            color = DarkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp),
        )
        Spacer(Modifier.height(tuning.titleGap + 4.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(tuning.cardGap, Alignment.CenterVertically),
        ) {
            PromiseCard(
                label = stringResource(R.string.welcome_promise_no_ads),
                icon = PromiseIcon.NoAds,
            )
            PromiseCard(
                label = stringResource(R.string.welcome_promise_no_paywall),
                icon = PromiseIcon.NoPaywall,
            )
            PromiseCard(
                label = stringResource(R.string.welcome_promise_open_source),
                icon = PromiseIcon.OpenSource,
            )
        }
        Spacer(Modifier.height(tuning.buttonTopGap))
        AppButton(
            text = stringResource(R.string.welcome_promise_continue),
            onClick = onContinue,
        )
    }
}

private enum class PromiseIcon(val asset: String) {
    OpenSource("book-open.svg"),
    NoAds("play-off.svg"),
    NoPaywall("lock-open.svg"),
}

@Composable
private fun PromiseCard(
    label: String,
    icon: PromiseIcon,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(LayoutTuning.WelcomePromise.cardCorner)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(3.dp, shape, ambientColor = DarkBlue.copy(alpha = 0.18f), spotColor = BirdTeal.copy(alpha = 0.16f))
            .clip(shape)
            .background(CardFill)
            .border(1.dp, CardBorder, shape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(BirdTeal.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = "${ASSET_ROOT}${icon.asset}",
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(StaffBlue),
                modifier = Modifier.size(22.dp),
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            FittedText(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = DarkBlue,
                ),
            )
        }
    }
}

@Composable
private fun PromiseBird(
    boxSize: Dp,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "promise-bird")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    Box(
        modifier = modifier.size(boxSize),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = BirdTeal.copy(alpha = 0.16f),
                radius = size.minDimension * 0.42f,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .graphicsLayer {
                    translationY = (bob - 0.5f) * 8.dp.toPx()
                }
                .shadow(
                    elevation = 12.dp,
                    shape = CircleShape,
                    ambientColor = DarkBlue.copy(alpha = 0.28f),
                    spotColor = BirdTeal.copy(alpha = 0.38f),
                )
                .background(Neutral, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = "${ASSET_ROOT}low-poly-bird.svg",
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(0.74f),
            )
        }
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = boxSize * 0.08f, bottom = boxSize * 0.06f)
                .size(boxSize * 0.22f),
        ) {
            drawCircle(BirdTeal)
            val heart = heartPath(size)
            drawPath(heart, Neutral)
        }
    }
}

private fun heartPath(size: Size): Path {
    val w = size.width
    val h = size.height
    return Path().apply {
        moveTo(w * 0.5f, h * 0.78f)
        cubicTo(w * 0.18f, h * 0.58f, w * 0.14f, h * 0.32f, w * 0.32f, h * 0.24f)
        cubicTo(w * 0.42f, h * 0.19f, w * 0.5f, h * 0.28f, w * 0.5f, h * 0.36f)
        cubicTo(w * 0.5f, h * 0.28f, w * 0.58f, h * 0.19f, w * 0.68f, h * 0.24f)
        cubicTo(w * 0.86f, h * 0.32f, w * 0.82f, h * 0.58f, w * 0.5f, h * 0.78f)
        close()
    }
}
