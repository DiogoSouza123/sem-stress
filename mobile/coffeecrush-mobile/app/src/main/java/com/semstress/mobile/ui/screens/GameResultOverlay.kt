package com.semstress.mobile.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.ui.components.CoffeePanel
import com.semstress.mobile.ui.components.CoffeePrimaryButton
import com.semstress.mobile.ui.components.CoffeeSecondaryButton
import com.semstress.mobile.ui.components.ProgressCup
import com.semstress.mobile.ui.state.GameUiState
import com.semstress.mobile.ui.theme.CoffeeTheme
import com.semstress.mobile.ui.theme.TABULAR_NUMBER_FEATURE
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val SCRIM_ALPHA = 0.55f
private const val POINTS_COUNT_UP_MS = 900
private const val STAR_BOUNCE_MS = 260
private const val STAR_STAGGER_MS = 140L
private const val RESULT_STARS_TOTAL = 3
private val RESULT_CUP_SIZE = 72.dp
private val RESULT_STAR_SIZE = 28.dp

/**
 * UX-07: a dedicated full-screen overlay replacing the plain [androidx.compose.material3.AlertDialog]
 * result popup - a filling cup + count-up score + sequential star bounce-in for victories, and a warm,
 * non-punishing message with progress-toward-goal for defeats. No Lottie confetti / rewarded-ad slot
 * yet (see backlog note).
 */
@Composable
fun GameResultOverlay(game: GameUiState, onReplayStage: () -> Unit, onBackToMenu: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM_ALPHA)),
        contentAlignment = Alignment.Center
    ) {
        CoffeePanel(modifier = Modifier.padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(if (game.won) R.string.stage_won_title else R.string.stage_lost_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoffeeTheme.colors.hudText
                )
                Spacer(modifier = Modifier.height(12.dp))

                val progress = if (game.target > 0) game.points.toFloat() / game.target else 0f
                ProgressCup(progress = progress, cupWidth = RESULT_CUP_SIZE, cupHeight = RESULT_CUP_SIZE)
                Spacer(modifier = Modifier.height(8.dp))
                AnimatedScoreText(game.points)

                if (game.won) {
                    Spacer(modifier = Modifier.height(8.dp))
                    BouncingStarRating(stars = game.starsEarned)
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.stage_lost_encouragement),
                        style = MaterialTheme.typography.bodyMedium,
                        color = CoffeeTheme.colors.hudTextMuted
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                CoffeePrimaryButton(
                    text = stringResource(R.string.play_again),
                    onClick = onReplayStage,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                CoffeeSecondaryButton(
                    text = stringResource(R.string.back_to_menu),
                    onClick = onBackToMenu,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AnimatedScoreText(finalScore: Int) {
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(finalScore) {
        animatedScore.animateTo(finalScore.toFloat(), tween(POINTS_COUNT_UP_MS, easing = LinearOutSlowInEasing))
    }
    Text(
        text = animatedScore.value.roundToInt().toString(),
        style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = TABULAR_NUMBER_FEATURE),
        fontWeight = FontWeight.ExtraBold,
        color = CoffeeTheme.colors.hudText
    )
}

/** UX-07: stars scale in from zero, one after another, instead of appearing all at once. */
@Composable
private fun BouncingStarRating(stars: Int) {
    val filledCount = stars.coerceIn(0, RESULT_STARS_TOTAL)
    Row {
        repeat(RESULT_STARS_TOTAL) { index ->
            val filled = index < filledCount
            val scaleAnim = remember(stars) { Animatable(0f) }
            LaunchedEffect(stars) {
                if (filled) {
                    delay(index * STAR_STAGGER_MS)
                    scaleAnim.animateTo(1f, tween(STAR_BOUNCE_MS))
                } else {
                    scaleAnim.snapTo(1f)
                }
            }
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (filled) CoffeeTheme.colors.warning else CoffeeTheme.colors.hudTextMuted,
                modifier = Modifier
                    .size(RESULT_STAR_SIZE)
                    .graphicsLayer {
                        scaleX = scaleAnim.value
                        scaleY = scaleAnim.value
                    }
            )
        }
    }
}
