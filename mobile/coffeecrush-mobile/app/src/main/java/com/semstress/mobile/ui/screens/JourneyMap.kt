package com.semstress.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.domain.PlayerProgress
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.ui.components.StageNodeButton
import com.semstress.mobile.ui.components.StageNodeContainerWidth
import com.semstress.mobile.ui.components.StageNodeLabelHeight
import com.semstress.mobile.ui.components.StageNodeSize
import com.semstress.mobile.ui.components.StageNodeState
import com.semstress.mobile.ui.theme.Cream
import com.semstress.mobile.ui.theme.Gold
import kotlin.math.sin
import kotlin.random.Random

/** Vertical distance between consecutive nodes; the map's total height scales linearly with it. */
private val NodeSpacingY = 108.dp

/** Breathing room above the first node and below the last one. */
private val MapVerticalPadding = 64.dp

// Two superimposed sine waves give the zig-zag an organic, constellation-like drift while
// remaining fully deterministic: any stage count maps to positions with no hand-tuned anchors.
private const val WAVE_MAIN_AMPLITUDE = 0.24f
private const val WAVE_MAIN_FREQUENCY = 1.9f
private const val WAVE_DRIFT_AMPLITUDE = 0.09f
private const val WAVE_DRIFT_FREQUENCY = 0.63f
private const val X_CENTER_FRACTION = 0.5f

private const val LINE_WIDTH_DP = 2.5f
private const val LINE_GLOW_WIDTH_DP = 9f
private const val LINE_GLOW_ALPHA = 0.20f
private const val DOTTED_LINE_ALPHA = 0.35f
private const val DASH_ON_DP = 10f
private const val DASH_OFF_DP = 16f

private const val VIGNETTE_ALPHA = 0.30f

private const val STAR_SEED = 42
private const val STARS_PER_NODE = 5
private const val STAR_MIN_RADIUS = 1.2f
private const val STAR_MAX_RADIUS = 2.8f
private const val STAR_MIN_ALPHA = 0.10f
private const val STAR_MAX_ALPHA = 0.32f

/** Decorative background star: fractions of the full map size, resolved to px at draw time. */
private data class MapStar(val xFraction: Float, val yFraction: Float, val radius: Float, val alpha: Float)

/** Bundles [JourneyMap]'s data inputs so the composable stays under the parameter limit. */
data class JourneyMapContent(
    val stages: List<StageConfig>,
    val progress: PlayerProgress,
    val selectedStageId: Int
)

/**
 * UX-04: constellation stage selector. Node positions are procedural (sine-wave drift over a
 * vertical axis), so the map scales to any stage count with no per-stage layout data — adding a
 * stage to the catalog JSON is all it takes. The wood texture tiles seamlessly behind a Canvas
 * that draws the constellation itself: golden glowing segments between reached stages, dotted
 * segments into the locked ones, plus faint decorative stars. Everything scrolls as one unit and
 * the map opens auto-centered on the player's current stage.
 *
 * [contentPadding] is the standard scrollable-under-floating-bars treatment: the caller reserves
 * the height of any chrome overlaying the map (top bar, bottom bar, insets) so the first/last
 * nodes rest clear of it, while the map still scrolls behind the bars in between.
 */
@Composable
fun JourneyMap(
    content: JourneyMapContent,
    onSelectStage: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    val (stages, progress, selectedStageId) = content
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewportHeight = maxHeight
        val mapWidth = maxWidth
        val topReserved = contentPadding.calculateTopPadding()
        val bottomReserved = contentPadding.calculateBottomPadding()
        val mapHeight = (
            MapVerticalPadding * 2 + topReserved + bottomReserved +
                NodeSpacingY * (stages.size - 1).coerceAtLeast(0)
            ).coerceAtLeast(viewportHeight)

        val scrollState = rememberScrollState()
        val density = LocalDensity.current

        // Open centered on the player's current stage so late-game players never scroll to reach it.
        val currentIndex = stages.indexOfFirst { it.id == progress.highestUnlockedStage }.coerceAtLeast(0)
        LaunchedEffect(stages.size, currentIndex) {
            val targetPx = with(density) {
                (nodeCenterY(currentIndex, mapHeight, bottomReserved) - viewportHeight / 2).toPx()
            }
            scrollState.scrollTo(targetPx.toInt().coerceIn(0, scrollState.maxValue))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(mapHeight)) {
                ConstellationCanvas(stages, progress, bottomReserved)

                stages.forEachIndexed { index, stage ->
                    StageNodeButton(
                        state = StageNodeState(
                            stageNumber = stage.id,
                            unlocked = progress.isUnlocked(stage.id),
                            completed = progress.scoreFor(stage.id) > 0,
                            selected = selectedStageId == stage.id,
                            isNextPlayable = stage.id == progress.highestUnlockedStage &&
                                progress.scoreFor(stage.id) == 0,
                            stars = progress.starsFor(stage.id)
                        ),
                        stageName = stage.name,
                        onClick = { if (progress.isUnlocked(stage.id)) onSelectStage(stage.id) },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(
                                x = mapWidth * nodeXFraction(index) - StageNodeContainerWidth / 2,
                                // Label sits above the sprite, so shift up to keep the sprite's
                                // center (not the column's) on the constellation line.
                                y = nodeCenterY(index, mapHeight, bottomReserved) -
                                    StageNodeLabelHeight - StageNodeSize / 2
                            )
                    )
                }
            }
        }
    }
}

private fun nodeXFraction(index: Int): Float =
    X_CENTER_FRACTION +
        WAVE_MAIN_AMPLITUDE * sin(index * WAVE_MAIN_FREQUENCY) +
        WAVE_DRIFT_AMPLITUDE * sin(index * WAVE_DRIFT_FREQUENCY)

/** Stage 1 sits at the BOTTOM of the map and later stages climb upward, like an ascent. */
private fun nodeCenterY(index: Int, mapHeight: Dp, bottomReserved: Dp): Dp =
    mapHeight - MapVerticalPadding - bottomReserved - NodeSpacingY * index

@Composable
private fun ConstellationCanvas(stages: List<StageConfig>, progress: PlayerProgress, bottomReserved: Dp) {
    val woodTexture = ImageShader(
        ImageBitmap.imageResource(R.drawable.bg_stage_map),
        TileMode.Repeated,
        TileMode.Repeated
    )
    val backgroundBrush = remember(woodTexture) { ShaderBrush(woodTexture) }
    val decorativeStars = remember(stages.size) { generateStars(stages.size) }
    val nodeSpacingPx = with(LocalDensity.current) { NodeSpacingY.toPx() }
    val edgePaddingPx = with(LocalDensity.current) { (MapVerticalPadding + bottomReserved).toPx() }

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(backgroundBrush)
        drawRect(
            Brush.verticalGradient(
                listOf(
                    Color.Black.copy(alpha = VIGNETTE_ALPHA),
                    Color.Transparent,
                    Color.Black.copy(alpha = VIGNETTE_ALPHA)
                )
            )
        )
        drawDecorativeStars(decorativeStars)
        drawConnections(stages, progress) { index ->
            Offset(size.width * nodeXFraction(index), size.height - edgePaddingPx - nodeSpacingPx * index)
        }
    }
}

private fun DrawScope.drawDecorativeStars(stars: List<MapStar>) {
    stars.forEach { star ->
        drawCircle(
            color = Gold.copy(alpha = star.alpha),
            radius = star.radius * density,
            center = Offset(star.xFraction * size.width, star.yFraction * size.height)
        )
    }
}

private fun DrawScope.drawConnections(
    stages: List<StageConfig>,
    progress: PlayerProgress,
    centerOf: DrawScope.(Int) -> Offset
) {
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(DASH_ON_DP * density, DASH_OFF_DP * density))
    for (i in 0 until stages.size - 1) {
        val from = centerOf(i)
        val to = centerOf(i + 1)
        if (progress.isUnlocked(stages[i + 1].id)) {
            // Reached segment: thin golden line over a wide translucent pass for a glow feel.
            drawLine(
                color = Gold.copy(alpha = LINE_GLOW_ALPHA),
                start = from,
                end = to,
                strokeWidth = LINE_GLOW_WIDTH_DP * density,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Gold,
                start = from,
                end = to,
                strokeWidth = LINE_WIDTH_DP * density,
                cap = StrokeCap.Round
            )
        } else {
            drawLine(
                color = Cream.copy(alpha = DOTTED_LINE_ALPHA),
                start = from,
                end = to,
                strokeWidth = LINE_WIDTH_DP * density,
                cap = StrokeCap.Round,
                pathEffect = dashEffect
            )
        }
    }
}

private fun generateStars(stageCount: Int): List<MapStar> {
    val random = Random(STAR_SEED)
    return List(stageCount.coerceAtLeast(1) * STARS_PER_NODE) {
        MapStar(
            xFraction = random.nextFloat(),
            yFraction = random.nextFloat(),
            radius = STAR_MIN_RADIUS + random.nextFloat() * (STAR_MAX_RADIUS - STAR_MIN_RADIUS),
            alpha = STAR_MIN_ALPHA + random.nextFloat() * (STAR_MAX_ALPHA - STAR_MIN_ALPHA)
        )
    }
}
