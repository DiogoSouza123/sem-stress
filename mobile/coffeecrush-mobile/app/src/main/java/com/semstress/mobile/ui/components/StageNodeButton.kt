package com.semstress.mobile.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.semstress.mobile.R
import com.semstress.mobile.ui.theme.Cream
import com.semstress.mobile.ui.theme.GameFontFamily
import com.semstress.mobile.ui.theme.Gold

/**
 * Node sprite size. Public so [com.semstress.mobile.ui.screens.JourneyMap] can turn a map
 * anchor into a pixel offset.
 */
val StageNodeSize = 60.dp

/** Column width (wider than the sprite) so the name above and stars below center without clipping. */
val StageNodeContainerWidth = 104.dp

/** Fixed height reserved for the name label above the sprite, so the sprite center stays predictable. */
val StageNodeLabelHeight = 34.dp

private const val PULSE_SCALE = 1.12f
private const val PULSE_DURATION_MS = 650
private const val LABEL_FONT_SIZE_SP = 13
private const val LABEL_MAX_LINES = 2
private const val LABEL_SHADOW_ALPHA = 0.8f
private const val LABEL_SHADOW_BLUR = 6f
private const val LOCKED_LABEL_ALPHA = 0.6f
private const val GLOW_ALPHA = 0.55f
private const val GLOW_RADIUS_SCALE = 0.85f
private const val STARS_BACKGROUND_ALPHA = 0.45f
private val StarsBackgroundShape = RoundedCornerShape(6.dp)

data class StageNodeState(
    val stageNumber: Int,
    val unlocked: Boolean,
    val completed: Boolean,
    val selected: Boolean,
    val isNextPlayable: Boolean,
    val stars: Int
)

/**
 * A sprite-based level node for the constellation map ([com.semstress.mobile.ui.screens.JourneyMap]).
 * The badge artwork is a swappable PNG per state (completed/current/locked in `drawable-nodpi/`);
 * the stage name (which carries the stage number, e.g. "Fase 1 - Introducao") sits above the
 * sprite in the bundled game font, and the star rating below it gets a dark backing plate so it
 * reads over the wood artwork. The pulse for [StageNodeState.isNextPlayable] is read inside
 * [graphicsLayer] (draw phase, no recomposition) so only that single node pays for a running
 * animation loop — the same draw-time-animation approach already used for the board (RR-20).
 */
@Composable
fun StageNodeButton(
    state: StageNodeState,
    stageName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(state.isNextPlayable) {
        if (state.isNextPlayable) {
            while (true) {
                scale.animateTo(PULSE_SCALE, tween(PULSE_DURATION_MS))
                scale.animateTo(1f, tween(PULSE_DURATION_MS))
            }
        } else {
            scale.snapTo(1f)
        }
    }

    val statusLabel = stringResource(
        if (state.unlocked) R.string.stage_status_unlocked else R.string.stage_status_locked
    )
    val accessibilityLabel = stringResource(R.string.stage_node_accessibility, stageName, statusLabel)

    Column(
        modifier = modifier.width(StageNodeContainerWidth),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        NodeNameLabel(stageName = stageName, unlocked = state.unlocked)

        NodeSprite(
            state = state,
            modifier = Modifier
                .size(StageNodeSize)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clickable(enabled = state.unlocked, onClick = onClick)
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityLabel
                }
        )

        if (state.completed) {
            Spacer(modifier = Modifier.height(3.dp))
            Box(
                modifier = Modifier
                    .clip(StarsBackgroundShape)
                    .background(Color.Black.copy(alpha = STARS_BACKGROUND_ALPHA))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                StarRating(stars = state.stars, starSize = 12.dp)
            }
        }
    }
}

@Composable
private fun NodeNameLabel(stageName: String, unlocked: Boolean) {
    Box(
        modifier = Modifier.height(StageNodeLabelHeight).width(StageNodeContainerWidth),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = stageName,
            style = TextStyle(
                fontFamily = GameFontFamily,
                fontSize = LABEL_FONT_SIZE_SP.sp,
                color = Cream.copy(alpha = if (unlocked) 1f else LOCKED_LABEL_ALPHA),
                shadow = Shadow(
                    color = Color.Black.copy(alpha = LABEL_SHADOW_ALPHA),
                    blurRadius = LABEL_SHADOW_BLUR
                )
            ),
            textAlign = TextAlign.Center,
            maxLines = LABEL_MAX_LINES
        )
    }
}

@Composable
private fun NodeSprite(state: StageNodeState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawBehind {
            if (state.selected || state.isNextPlayable) {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Gold.copy(alpha = GLOW_ALPHA), Color.Transparent),
                        radius = size.maxDimension * GLOW_RADIUS_SCALE
                    ),
                    radius = size.maxDimension * GLOW_RADIUS_SCALE
                )
            }
        },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(spriteFor(state)),
            contentDescription = null,
            modifier = Modifier.matchParentSize()
        )
    }
}

private fun spriteFor(state: StageNodeState): Int = when {
    !state.unlocked -> R.drawable.node_locked
    state.completed -> R.drawable.node_completed
    else -> R.drawable.node_current
}
