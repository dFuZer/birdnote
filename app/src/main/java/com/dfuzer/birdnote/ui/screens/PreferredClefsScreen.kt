package com.dfuzer.birdnote.ui.screens

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import com.dfuzer.birdnote.R
import com.dfuzer.birdnote.domain.Clef
import com.dfuzer.birdnote.ui.LayoutTuning
import com.dfuzer.birdnote.ui.components.AppButton
import com.dfuzer.birdnote.ui.components.DecoratedScreen
import com.dfuzer.birdnote.ui.components.FittedText
import com.dfuzer.birdnote.ui.components.label
import com.dfuzer.birdnote.ui.staff.assetName
import com.dfuzer.birdnote.ui.staff.glyphBox
import com.dfuzer.birdnote.ui.theme.BirdTeal
import com.dfuzer.birdnote.ui.theme.DarkBlue
import com.dfuzer.birdnote.ui.theme.Neutral
import com.dfuzer.birdnote.ui.theme.StaffBlue
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ASSET_ROOT = "file:///android_asset/"

/** Room around the treble clef, the glyph that hangs farthest past the staff. */
private const val PREVIEW_LINE_SPACES = 7.6f
private const val PREVIEW_BOTTOM_LINE_FRACTION = 5.7f / PREVIEW_LINE_SPACES

private val SelectedCard = Color(0xFFE7F6F4)
private val IdleBorder = DarkBlue.copy(alpha = 0.14f)

@Composable
fun PreferredClefsScreen(
    selected: Set<Clef>,
    onToggle: (Clef) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.PreferredClefs
    DecoratedScreen(modifier = modifier) {
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
                    WelcomeIntro(Modifier.weight(1f).fillMaxHeight())
                    ClefPanel(
                        selected = selected,
                        onToggle = onToggle,
                        onContinue = onContinue,
                        modifier = Modifier.weight(1.15f).fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    WelcomeIntro(Modifier.fillMaxWidth())
                    ClefPanel(
                        selected = selected,
                        onToggle = onToggle,
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
private fun WelcomeIntro(modifier: Modifier = Modifier) {
    val tuning = LayoutTuning.PreferredClefs
    val animate = motionEnabled()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.bodyLarge.copy(letterSpacing = 0.6.sp),
            color = BirdTeal,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(tuning.titleGap))
        Text(
            text = stringResource(R.string.preferred_clefs_title),
            style = MaterialTheme.typography.headlineMedium,
            color = DarkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(tuning.titleGap))
        Text(
            text = stringResource(R.string.welcome_message),
            style = MaterialTheme.typography.bodyLarge,
            color = DarkBlue,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
        Spacer(Modifier.height(tuning.birdToTitle))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val side = minOf(maxWidth * tuning.birdWidthFraction, tuning.birdMax)
            if (side > 0.dp) {
                WelcomeBird(
                    boxSize = side,
                    animate = animate,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun ClefPanel(
    selected: Set<Clef>,
    onToggle: (Clef) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tuning = LayoutTuning.PreferredClefs
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.welcome_clefs),
            style = MaterialTheme.typography.bodyLarge,
            color = DarkBlue,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(tuning.promptBottomGap))
        ClefChoiceGrid(
            selected = selected,
            onToggle = onToggle,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        Spacer(Modifier.height(tuning.hintTopGap))
        Text(
            text = stringResource(R.string.preferred_clefs_hint),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 18.sp),
            color = StaffBlue,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(tuning.buttonTopGap))
        AppButton(
            text = stringResource(R.string.continue_action),
            onClick = onContinue,
        )
    }
}

@Composable
private fun ClefChoiceGrid(
    selected: Set<Clef>,
    onToggle: (Clef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val gap = LayoutTuning.PreferredClefs.cardGap
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(gap),
    ) {
        Clef.entries.chunked(2).forEach { row ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(gap),
            ) {
                row.forEach { clef ->
                    val on = clef in selected
                    ClefChoiceCard(
                        clef = clef,
                        selected = on,
                        enabled = !on || selected.size > 1,
                        onClick = { onToggle(clef) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ClefChoiceCard(
    clef: Clef,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(LayoutTuning.PreferredClefs.cardCorner)
    val borderColor by animateColorAsState(
        targetValue = if (selected) BirdTeal else IdleBorder,
        label = "clef-border",
    )
    val background by animateColorAsState(
        targetValue = if (selected) SelectedCard else Neutral,
        label = "clef-fill",
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 2.5.dp else 1.dp,
        label = "clef-border-width",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 6.dp else 2.dp,
        label = "clef-elevation",
    )
    val mark by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "clef-mark",
    )
    Box(
        modifier = modifier
            .shadow(elevation, shape, ambientColor = DarkBlue.copy(alpha = 0.22f), spotColor = BirdTeal.copy(alpha = 0.2f))
            .clip(shape)
            .background(background)
            .border(borderWidth, borderColor, shape)
            .clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick)
            .semantics { this.selected = selected },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ClefStaffPreview(
                clef = clef,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            FittedText(
                text = clef.label(),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = DarkBlue,
                ),
            )
        }
        if (mark > 0f) {
            SelectedMark(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .graphicsLayer {
                        scaleX = mark
                        scaleY = mark
                        alpha = mark
                    },
            )
        }
    }
}

@Composable
private fun SelectedMark(modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        drawCircle(BirdTeal)
        val path = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.68f)
            lineTo(size.width * 0.74f, size.height * 0.36f)
        }
        drawPath(
            path = path,
            color = Neutral,
            style = Stroke(
                width = 1.8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Composable
private fun ClefStaffPreview(
    clef: Clef,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        BoxWithConstraints(modifier.clipToBounds()) {
            val heightPx = constraints.maxHeight.toFloat()
            val widthPx = constraints.maxWidth.toFloat()
            if (heightPx > 0f && widthPx > 0f) {
                val lineSpacing = heightPx / PREVIEW_LINE_SPACES
                val bottomLineY = heightPx * PREVIEW_BOTTOM_LINE_FRACTION
                val staffLeft = with(density) { 8.dp.toPx() }
                val staffRight = widthPx - staffLeft
                val glyph = clef.glyphBox()
                val clefX = staffLeft + lineSpacing * glyph.xInLineSpaces
                val clefY = bottomLineY + lineSpacing * glyph.topInLineSpaces
                val clefWidth = lineSpacing * glyph.widthInLineSpaces
                val clefHeight = lineSpacing * glyph.heightInLineSpaces
                Canvas(Modifier.fillMaxSize()) {
                val stroke = 1.35.dp.toPx()
                for (line in 0..4) {
                    val y = bottomLineY - line * lineSpacing
                    drawLine(
                        color = StaffBlue,
                        start = Offset(staffLeft, y),
                        end = Offset(staffRight, y),
                        strokeWidth = stroke,
                        cap = StrokeCap.Round,
                    )
                }
                    val clefRight = clefX + clefWidth
                    val noteX = (staffLeft + (staffRight - staffLeft) * 0.72f)
                        .coerceAtLeast(clefRight + lineSpacing * 0.85f)
                    val noteHalf = lineSpacing * 0.7f
                    if (noteX + noteHalf < staffRight) {
                        val noteY = bottomLineY - clef.homeLine() * lineSpacing
                        drawNoteHead(Offset(noteX, noteY), lineSpacing, BirdTeal)
                    }
                }
                AsyncImage(
                    model = "$ASSET_ROOT${clef.assetName()}",
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(StaffBlue),
                    modifier = Modifier
                        .offset { IntOffset(clefX.roundToInt(), clefY.roundToInt()) }
                        .size(
                            width = with(density) { clefWidth.toDp() },
                            height = with(density) { clefHeight.toDp() },
                        ),
                )
            }
        }
    }
}

@Composable
private fun WelcomeBird(
    boxSize: Dp,
    animate: Boolean,
    modifier: Modifier = Modifier,
) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(animate) {
        if (animate) {
            enter.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            )
        } else {
            enter.snapTo(1f)
        }
    }
    val transition = rememberInfiniteTransition(label = "welcome-bird")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bob",
    )
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "sway",
    )
    val flight by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = LinearEasing),
        ),
        label = "flight",
    )
    val motion = if (animate) 1f else 0f
    val appeared = enter.value
    Box(
        modifier = modifier
            .size(boxSize)
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val discRadius = size.minDimension * 0.34f
            val glow = 0.94f + 0.1f * bob * motion
            drawCircle(
                color = BirdTeal.copy(alpha = 0.16f * appeared),
                radius = discRadius * 1.28f * glow,
            )
        }
        if (motion > 0f) {
            RisingCroches(
                progress = flight,
                alphaScale = appeared,
                boxSize = boxSize,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize(0.68f)
                .graphicsLayer {
                    translationY = (bob - 0.5f) * 10.dp.toPx() * motion * appeared
                    alpha = appeared
                    val settle = 0.84f + 0.16f * appeared
                    scaleX = settle
                    scaleY = settle
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
                model = "${ASSET_ROOT}icon.svg",
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize(0.74f)
                    .graphicsLayer {
                        rotationZ = sway * 5f * motion * appeared
                        val breathe = 1f + (bob - 0.5f) * 0.06f * motion
                        scaleX = breathe
                        scaleY = breathe
                    },
            )
        }
    }
}

private const val CROCHE_ASPECT = 2.1521999f / 4.0519995f

@Composable
private fun RisingCroches(
    progress: Float,
    alphaScale: Float,
    boxSize: Dp,
) {
    val notes = listOf(
        CrocheFlight(phase = 0f, xFraction = 0.16f, sizeScale = 0.92f),
        CrocheFlight(phase = 0.38f, xFraction = 0.84f, sizeScale = 1f),
        CrocheFlight(phase = 0.67f, xFraction = 0.3f, sizeScale = 0.74f),
    )
    Box(Modifier.fillMaxSize()) {
        notes.forEach { note ->
        val travel = (progress + note.phase) % 1f
        val fade = sin(travel * PI).toFloat().coerceIn(0f, 1f)
        val height = boxSize * 0.32f * note.sizeScale
        val width = height * CROCHE_ASPECT
        AsyncImage(
            model = "${ASSET_ROOT}croche.svg",
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(BirdTeal),
            modifier = Modifier
                .size(width, height)
                .graphicsLayer {
                    val wobble = sin(travel * PI * 2.0).toFloat() * 4.dp.toPx()
                    translationX = boxSize.toPx() * note.xFraction + wobble - width.toPx() / 2f
                    translationY = lerp(boxSize.toPx() * 0.8f, boxSize.toPx() * 0.08f, travel) -
                        height.toPx() / 2f
                    alpha = fade * 0.85f * alphaScale
                },
            )
        }
    }
}

private data class CrocheFlight(
    val phase: Float,
    val xFraction: Float,
    val sizeScale: Float,
)

private fun DrawScope.drawNoteHead(
    center: Offset,
    lineSpacing: Float,
    color: Color,
) {
    val headW = lineSpacing * 1.15f
    val headH = lineSpacing * 0.82f
    rotate(degrees = -20f, pivot = center) {
        drawOval(
            color = color,
            topLeft = Offset(center.x - headW / 2f, center.y - headH / 2f),
            size = Size(headW, headH),
        )
    }
}

@Composable
private fun motionEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }
}

/** Staff line counted up from the bottom: the note each clef names. */
private fun Clef.homeLine(): Int = when (this) {
    Clef.SOL -> 1
    Clef.FA -> 3
    Clef.ALTO -> 2
    Clef.TENOR -> 3
}
