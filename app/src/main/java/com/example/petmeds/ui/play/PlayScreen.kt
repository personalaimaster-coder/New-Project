package com.example.petmeds.ui.play

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

private object PlayPalette {
    val SkyTop = Color(0xFF4A9BFF)
    val SkyBottom = Color(0xFFA9D9FF)
    val GrassTop = Color(0xFF8BD13C)
    val GrassMid = Color(0xFF55B82E)
    val GrassDark = Color(0xFF2E7D14)
    val Cloud = Color(0xFFFFFFFF)
    val LaneLine = Color(0xFFFFFFFF)
    val CoinYellow = Color(0xFFFFD12F)
    val CoralAccent = Color(0xFFFF7043)
    val DarkText = Color(0xFF3E2723)
    val TreatHalo = Color(0xFFFFEB3B)
    val PoisonHalo = Color(0xFFE53935)
    val OverlayBg = Color(0xFFFFF8E1)
    val ButtonGreen = Color(0xFF43A047)
    val ShadowBlack = Color(0xFF000000)
}

private object PuppyColors {
    val FurBrown = Color(0xFFC07A3C)
    val FurDark = Color(0xFF9C5C26)
    val BodyWhite = Color(0xFFFCF5EC)
    val EarPink = Color(0xFFE9B6A0)
    val Nose = Color(0xFF221208)
    val EyeBlack = Color(0xFF181410)
    val EyeWhite = Color(0xFFFFFFFF)
    val Tongue = Color(0xFFF26B7A)
    val MouthDark = Color(0xFF2A1411)
}

/** Tasty things the puppy is going for. Cycled by `FallingItem.variantIndex % size`. */
private val TREAT_EMOJIS = listOf("\uD83E\uDDB4", "\uD83C\uDF56", "\uD83C\uDF57", "\uD83E\uDD69", "\uD83E\uDD53", "\uD83E\uDDC0")

/** Things that are toxic / bad for dogs: chocolate, grapes, pills, coffee, candy. */
private val POISON_EMOJIS = listOf("\uD83C\uDF6B", "\uD83C\uDF47", "\uD83D\uDC8A", "\u2615", "\uD83C\uDF6C")

@Composable
fun PlayScreen(
    viewModel: PlayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.phase) {
        if (state.phase != GamePhase.PLAYING) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (state.phase == GamePhase.PLAYING) {
            val now = withFrameMillis { it }
            val delta = (now - last).coerceAtLeast(0L)
            last = now
            if (delta > 0L) viewModel.onFrame(delta)
        }
    }

    Scaffold(
        containerColor = PlayPalette.SkyBottom,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            Playfield(
                state = state,
                onSwipe = viewModel::swipe,
                modifier = Modifier.fillMaxSize(),
            )

            HudRow(
                score = state.score,
                highScore = state.highScore,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            )

            AnimatedVisibility(
                visible = state.phase == GamePhase.IDLE,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                IdleOverlay(
                    highScore = state.highScore,
                    onPlay = viewModel::start,
                )
            }

            AnimatedVisibility(
                visible = state.phase == GamePhase.GAME_OVER,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center),
            ) {
                GameOverOverlay(
                    score = state.score,
                    highScore = state.highScore,
                    isNewBest = state.score > 0 && state.score > state.previousHighScore,
                    onPlayAgain = viewModel::start,
                )
            }
        }
    }
}

@Composable
private fun Playfield(
    state: GameState,
    onSwipe: (Swipe) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 40.dp.toPx() }

    val transition = rememberInfiniteTransition(label = "play")
    val hopPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 620, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hopPhase",
    )
    val cloudDrift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "cloudDrift",
    )
    val animatedLane by animateFloatAsState(
        targetValue = state.puppyLane.toFloat(),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "puppyLane",
    )

    BoxWithConstraints(
        modifier = modifier
            .pointerInput(state.phase) {
                if (state.phase != GamePhase.PLAYING) return@pointerInput
                var dragX = 0f
                var emitted = false
                detectHorizontalDragGestures(
                    onDragStart = { dragX = 0f; emitted = false },
                    onDragEnd = { dragX = 0f; emitted = false },
                    onDragCancel = { dragX = 0f; emitted = false },
                ) { _, dragAmount ->
                    dragX += dragAmount
                    if (!emitted && abs(dragX) >= swipeThresholdPx) {
                        onSwipe(if (dragX > 0) Swipe.RIGHT else Swipe.LEFT)
                        emitted = true
                    }
                }
            },
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val laneWidthPx = widthPx / LANE_COUNT
        val grassStartFraction = 0.72f

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawBackground(
                grassStartFraction = grassStartFraction,
                laneCount = LANE_COUNT,
                cloudDrift = cloudDrift,
            )
        }

        val itemSize = 52.dp
        val itemSizePx = with(density) { itemSize.toPx() }
        for (item in state.items) {
            val laneCenterX = (item.lane + 0.5f) * laneWidthPx
            val itemX = (laneCenterX - itemSizePx / 2f).roundToInt()
            val itemY = (item.yFraction * heightPx - itemSizePx / 2f).roundToInt()
            val itemPhase = ((hopPhase + (item.id % 7) * 0.13f) % 1f)
            val itemWobble = sin(itemPhase.toDouble() * 2.0 * PI).toFloat() * 6f
            FallingItemSprite(
                item = item,
                rotationDegrees = itemWobble,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(itemX, itemY) }
                    .size(itemSize),
            )
        }

        // Puppy hop math: half-sine bounce, with squash-and-stretch.
        val hopValue = abs(sin(hopPhase.toDouble() * PI)).toFloat()
        val puppySize = 96.dp
        val puppySizePx = with(density) { puppySize.toPx() }
        val shadowSize = 64.dp
        val shadowSizePx = with(density) { shadowSize.toPx() }
        val hopHeightPx = with(density) { 18.dp.toPx() }

        val puppyLaneCenter = (animatedLane + 0.5f) * laneWidthPx
        val puppyX = (puppyLaneCenter - puppySizePx / 2f).roundToInt()
        val puppyBaseY = 0.90f * heightPx - puppySizePx / 2f
        val puppyY = (puppyBaseY - hopHeightPx * hopValue).roundToInt()
        val shadowX = (puppyLaneCenter - shadowSizePx / 2f).roundToInt()
        val shadowY = (0.97f * heightPx - with(density) { 6.dp.toPx() }).roundToInt()
        val squashY = 0.85f + 0.15f * hopValue
        val squashX = 1.10f - 0.10f * hopValue

        // What the puppy is looking at: the lowest item above the puppy row.
        val nearestItem = state.items
            .filter { it.yFraction in 0f..0.85f }
            .maxByOrNull { it.yFraction }
        val gazeX = nearestItem?.lane?.toFloat()?.let { itemLane ->
            ((itemLane - animatedLane) * 0.9f).coerceIn(-1f, 1f)
        } ?: 0f
        // Body lean: tilts in the direction of in-flight lane motion.
        val leanDegrees = ((state.puppyLane.toFloat() - animatedLane) * 14f).coerceIn(-14f, 14f)

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(shadowX, shadowY) }
                .size(width = shadowSize, height = 12.dp)
                .graphicsLayer {
                    scaleX = 0.55f + 0.45f * (1f - hopValue)
                    scaleY = 0.55f + 0.45f * (1f - hopValue)
                    alpha = 0.15f + 0.25f * (1f - hopValue)
                }
                .clip(CircleShape)
                .background(PlayPalette.ShadowBlack),
        )

        PuppySprite(
            isAlive = state.phase != GamePhase.GAME_OVER,
            gazeX = gazeX,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(puppyX, puppyY) }
                .size(puppySize)
                .graphicsLayer {
                    scaleX = squashX
                    scaleY = squashY
                    rotationZ = leanDegrees
                },
        )
    }
}

@Composable
private fun PuppySprite(
    isAlive: Boolean,
    gazeX: Float,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "puppyParts")
    val earWiggle by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "earWiggle",
    )
    val pawBob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 460, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pawBob",
    )
    val tongueBob by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tongueBob",
    )

    Canvas(modifier = modifier) {
        drawPuppy(
            isAlive = isAlive,
            gazeX = gazeX,
            earWiggle = if (isAlive) earWiggle else 0f,
            pawBob = if (isAlive) pawBob else 0f,
            tongueBob = if (isAlive) tongueBob else 0f,
        )
    }
}

private fun DrawScope.drawPuppy(
    isAlive: Boolean,
    gazeX: Float,
    earWiggle: Float,
    pawBob: Float,
    tongueBob: Float,
) {
    val w = size.width
    val h = size.height
    val outline = Stroke(width = 0.013f * w, cap = StrokeCap.Round)

    // ---- BODY (drawn first; brown back/sides) ----
    drawOval(
        color = PuppyColors.FurBrown,
        topLeft = Offset(0.22f * w, 0.56f * h),
        size = Size(0.56f * w, 0.40f * h),
    )
    // White chest patch on the body.
    drawOval(
        color = PuppyColors.BodyWhite,
        topLeft = Offset(0.30f * w, 0.66f * h),
        size = Size(0.40f * w, 0.28f * h),
    )

    // ---- RAISED PAWS in front of the chest, bobbing gently ----
    val pawLOff = pawBob * 0.012f * h
    val pawROff = -pawBob * 0.012f * h
    // Left paw.
    drawOval(
        color = PuppyColors.FurBrown,
        topLeft = Offset(0.30f * w, 0.66f * h + pawLOff),
        size = Size(0.16f * w, 0.20f * h),
    )
    drawOval(
        color = PuppyColors.EarPink,
        topLeft = Offset(0.34f * w, 0.74f * h + pawLOff),
        size = Size(0.08f * w, 0.09f * h),
    )
    // Right paw.
    drawOval(
        color = PuppyColors.FurBrown,
        topLeft = Offset(0.54f * w, 0.66f * h + pawROff),
        size = Size(0.16f * w, 0.20f * h),
    )
    drawOval(
        color = PuppyColors.EarPink,
        topLeft = Offset(0.58f * w, 0.74f * h + pawROff),
        size = Size(0.08f * w, 0.09f * h),
    )

    // ---- EARS (drawn before the head so the head covers their base) ----
    // Big triangular ears sticking up-and-out from the top of the head.
    rotate(degrees = -32f + earWiggle, pivot = Offset(0.36f * w, 0.28f * h)) {
        drawOval(
            color = PuppyColors.FurDark,
            topLeft = Offset(0.14f * w, 0.02f * h),
            size = Size(0.26f * w, 0.36f * h),
        )
        drawOval(
            color = PuppyColors.EarPink,
            topLeft = Offset(0.20f * w, 0.10f * h),
            size = Size(0.14f * w, 0.22f * h),
        )
    }
    rotate(degrees = 32f - earWiggle, pivot = Offset(0.64f * w, 0.28f * h)) {
        drawOval(
            color = PuppyColors.FurDark,
            topLeft = Offset(0.60f * w, 0.02f * h),
            size = Size(0.26f * w, 0.36f * h),
        )
        drawOval(
            color = PuppyColors.EarPink,
            topLeft = Offset(0.66f * w, 0.10f * h),
            size = Size(0.14f * w, 0.22f * h),
        )
    }

    // ---- HEAD ----
    // Whole head + face group tilts gently toward the food.
    val headTilt = gazeX * 7f
    rotate(degrees = headTilt, pivot = Offset(0.50f * w, 0.40f * h)) {
        // Brown head circle.
        drawCircle(
            color = PuppyColors.FurBrown,
            radius = 0.30f * w,
            center = Offset(0.50f * w, 0.36f * h),
        )
        // White muzzle / lower-face patch (creates the corgi "mask" look).
        drawOval(
            color = PuppyColors.BodyWhite,
            topLeft = Offset(0.30f * w, 0.42f * h),
            size = Size(0.40f * w, 0.26f * h),
        )
        // White stripe up the forehead (between the eyes).
        drawOval(
            color = PuppyColors.BodyWhite,
            topLeft = Offset(0.46f * w, 0.18f * h),
            size = Size(0.08f * w, 0.28f * h),
        )

        if (isAlive) {
            // Cute round black eyes positioned just above the muzzle.
            val eyeR = 0.045f * w
            val gazeOffX = gazeX * 0.010f * w
            drawCircle(PuppyColors.EyeBlack, eyeR, Offset(0.39f * w + gazeOffX, 0.38f * h))
            drawCircle(PuppyColors.EyeBlack, eyeR, Offset(0.61f * w + gazeOffX, 0.38f * h))
            // Bright catchlights for a happy expression.
            drawCircle(
                color = PuppyColors.EyeWhite,
                radius = eyeR * 0.40f,
                center = Offset(0.39f * w + gazeOffX + 0.014f * w, 0.38f * h - 0.014f * w),
            )
            drawCircle(
                color = PuppyColors.EyeWhite,
                radius = eyeR * 0.40f,
                center = Offset(0.61f * w + gazeOffX + 0.014f * w, 0.38f * h - 0.014f * w),
            )
        } else {
            // X eyes for game over.
            val half = 0.034f * w
            for (cx in listOf(0.39f * w, 0.61f * w)) {
                drawLine(
                    color = PuppyColors.EyeBlack,
                    start = Offset(cx - half, 0.38f * h - half),
                    end = Offset(cx + half, 0.38f * h + half),
                    strokeWidth = outline.width,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = PuppyColors.EyeBlack,
                    start = Offset(cx - half, 0.38f * h + half),
                    end = Offset(cx + half, 0.38f * h - half),
                    strokeWidth = outline.width,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Small black nose just under the white stripe.
        drawCircle(
            color = PuppyColors.Nose,
            radius = 0.034f * w,
            center = Offset(0.50f * w, 0.48f * h),
        )

        if (isAlive) {
            // Tiny smiley mouth — short upward curves on either side of a center dip.
            drawArc(
                color = PuppyColors.Nose,
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(0.42f * w, 0.52f * h),
                size = Size(0.16f * w, 0.06f * h),
                style = outline,
            )
            // Cute little tongue poking out between the smile, bobbing gently.
            val tongueY = 0.55f * h + tongueBob * 0.008f * h
            drawOval(
                color = PuppyColors.Tongue,
                topLeft = Offset(0.45f * w, tongueY),
                size = Size(0.10f * w, 0.06f * h),
            )
            // Tongue centerline.
            drawLine(
                color = PuppyColors.MouthDark.copy(alpha = 0.4f),
                start = Offset(0.50f * w, tongueY + 0.005f * h),
                end = Offset(0.50f * w, tongueY + 0.05f * h),
                strokeWidth = outline.width * 0.5f,
                cap = StrokeCap.Round,
            )
        } else {
            // Sad mouth on game over.
            drawArc(
                color = PuppyColors.Nose,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(0.42f * w, 0.55f * h),
                size = Size(0.16f * w, 0.06f * h),
                style = outline,
            )
        }
    }
}

private fun DrawScope.drawBackground(
    grassStartFraction: Float,
    laneCount: Int,
    cloudDrift: Float,
) {
    val grassY = size.height * grassStartFraction
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(PlayPalette.SkyTop, PlayPalette.SkyBottom),
            startY = 0f,
            endY = grassY,
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, grassY),
    )

    val driftPx = size.width * cloudDrift
    val margin = 140.dp.toPx()
    val totalRange = size.width + margin * 2f
    fun wrap(baseFraction: Float, speed: Float): Float =
        ((baseFraction * size.width + driftPx * speed) % totalRange + totalRange) % totalRange - margin
    drawCloud(
        center = Offset(wrap(0.15f, 1.0f), size.height * 0.10f),
        width = 80.dp.toPx(),
    )
    drawCloud(
        center = Offset(wrap(0.65f, 0.7f), size.height * 0.18f),
        width = 100.dp.toPx(),
    )
    drawCloud(
        center = Offset(wrap(0.40f, 0.45f), size.height * 0.30f),
        width = 70.dp.toPx(),
    )

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(PlayPalette.GrassTop, PlayPalette.GrassMid, PlayPalette.GrassDark),
            startY = grassY,
            endY = size.height,
        ),
        topLeft = Offset(0f, grassY),
        size = Size(size.width, size.height - grassY),
    )

    val tuftRadius = 11.dp.toPx()
    val tuftCount = 16
    for (i in 0..tuftCount) {
        val cx = size.width * i.toFloat() / tuftCount
        drawCircle(
            color = PlayPalette.GrassTop,
            radius = tuftRadius,
            center = Offset(cx, grassY - tuftRadius * 0.15f),
        )
    }

    val laneDashEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 14.dp.toPx()))
    val laneWidth = size.width / laneCount
    for (lane in 1 until laneCount) {
        val x = laneWidth * lane
        drawLine(
            color = PlayPalette.LaneLine.copy(alpha = 0.55f),
            start = Offset(x, grassY + tuftRadius),
            end = Offset(x, size.height),
            strokeWidth = 3.dp.toPx(),
            pathEffect = laneDashEffect,
        )
    }
}

private fun DrawScope.drawCloud(center: Offset, width: Float) {
    val r = width * 0.30f
    val cx = center.x
    val cy = center.y
    drawCircle(PlayPalette.Cloud, r * 1.05f, Offset(cx, cy))
    drawCircle(PlayPalette.Cloud, r * 0.80f, Offset(cx - r * 1.10f, cy + r * 0.20f))
    drawCircle(PlayPalette.Cloud, r * 0.70f, Offset(cx + r * 1.10f, cy + r * 0.20f))
    drawCircle(PlayPalette.Cloud, r * 0.65f, Offset(cx - r * 0.55f, cy - r * 0.55f))
    drawCircle(PlayPalette.Cloud, r * 0.55f, Offset(cx + r * 0.55f, cy - r * 0.45f))
    drawOval(
        color = PlayPalette.Cloud,
        topLeft = Offset(cx - r * 1.55f, cy + r * 0.10f),
        size = Size(r * 3.10f, r * 0.85f),
    )
}

@Composable
private fun FallingItemSprite(
    item: FallingItem,
    rotationDegrees: Float,
    modifier: Modifier = Modifier,
) {
    val emoji = when (item.kind) {
        ItemKind.TREAT -> TREAT_EMOJIS[item.variantIndex % TREAT_EMOJIS.size]
        ItemKind.POISON -> POISON_EMOJIS[item.variantIndex % POISON_EMOJIS.size]
    }
    val halo = when (item.kind) {
        ItemKind.TREAT -> PlayPalette.TreatHalo
        ItemKind.POISON -> PlayPalette.PoisonHalo
    }
    Box(
        modifier = modifier.graphicsLayer { rotationZ = rotationDegrees },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(halo.copy(alpha = 0.40f)),
        )
        Text(emoji, fontSize = 34.sp)
    }
}

@Composable
private fun HudRow(
    score: Int,
    highScore: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        HudChip(label = "Score", value = score.toString(), accent = PlayPalette.CoinYellow)
        HudChip(label = "Best", value = highScore.toString(), accent = PlayPalette.CoralAccent)
    }
}

@Composable
private fun HudChip(label: String, value: String, accent: Color) {
    Surface(
        shape = RoundedCornerShape(50),
        color = accent,
        shadowElevation = 6.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PlayPalette.DarkText,
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = PlayPalette.DarkText,
            )
        }
    }
}

@Composable
private fun IdleOverlay(
    highScore: Int,
    onPlay: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = PlayPalette.OverlayBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("\uD83D\uDC36", fontSize = 64.sp)
            Text(
                "Treat Catcher",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PlayPalette.DarkText,
            )
            Text(
                "Swipe left or right to move your puppy between 3 lanes. Catch the bones \uD83E\uDDB4 and meat \uD83C\uDF56, dodge the chocolate \uD83C\uDF6B and grapes \uD83C\uDF47.",
                style = MaterialTheme.typography.bodyMedium,
                color = PlayPalette.DarkText.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
            if (highScore > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PlayPalette.CoinYellow,
                ) {
                    Text(
                        "Best: $highScore",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlayPalette.DarkText,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onPlay,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlayPalette.ButtonGreen,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 36.dp, vertical = 14.dp),
            ) {
                Text("Play", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun GameOverOverlay(
    score: Int,
    highScore: Int,
    isNewBest: Boolean,
    onPlayAgain: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = PlayPalette.OverlayBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("\uD83C\uDF6B", fontSize = 48.sp)
            Text(
                "Oh no!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = PlayPalette.DarkText,
            )
            Text(
                "Score: $score",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = PlayPalette.DarkText,
            )
            if (isNewBest) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = PlayPalette.CoinYellow,
                ) {
                    Text(
                        "\u2728 New best! \u2728",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = PlayPalette.DarkText,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            } else {
                Text(
                    "Best: $highScore",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PlayPalette.DarkText.copy(alpha = 0.75f),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onPlayAgain,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp),
                ) {
                    Text("Play again", fontWeight = FontWeight.ExtraBold, color = PlayPalette.DarkText)
                }
            }
        }
    }
}
