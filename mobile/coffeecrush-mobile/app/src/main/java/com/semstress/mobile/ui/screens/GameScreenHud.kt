package com.semstress.mobile.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.ui.components.CoffeePanel
import com.semstress.mobile.ui.components.CoffeePrimaryButton
import com.semstress.mobile.ui.components.ProgressCup
import com.semstress.mobile.ui.components.StatChip
import com.semstress.mobile.ui.components.StatColumn
import com.semstress.mobile.ui.state.GameUiState
import com.semstress.mobile.ui.theme.CoffeeTheme
import com.semstress.mobile.ui.theme.Cream
import com.semstress.mobile.ui.theme.TABULAR_NUMBER_FEATURE
import kotlinx.coroutines.delay

private const val BANNER_SHADOW_ALPHA = 0.7f
private const val BANNER_SHADOW_BLUR = 8f
private const val LOW_MOVES_THRESHOLD = 5
private const val PULSE_MIN_ALPHA = 0.4f
private const val PULSE_STEP_MS = 500
private const val FLOATING_POINTS_VISIBLE_MS = 900L
private const val FIRST_STAGE_ID = 1
private const val AROMA_BAR_SHAPE_PERCENT = 50
private val AROMA_BAR_HEIGHT = 10.dp
private val SCOREBOARD_CUP_SIZE = 40.dp

/** UX-05: replaces the flat "Pontos/Meta/Mov" row with a filling [ProgressCup] toward the target. */
@Composable
internal fun Scoreboard(score: Int, target: Int, moves: Int, isZenMode: Boolean = false) {
    val colors = CoffeeTheme.colors
    val pointsLabel = stringResource(R.string.hud_points)

    if (isZenMode) {
        CoffeePanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatColumn(label = pointsLabel, value = score.toString())
                StatChip(text = stringResource(R.string.zen_mode_badge))
            }
        }
        return
    }

    val progress = if (target > 0) score.toFloat() / target else 0f
    val lowOnMoves = moves in 1..LOW_MOVES_THRESHOLD
    val movesPulse = rememberPulseAlpha(enabled = lowOnMoves && !rememberReducedMotionEnabled())
    val movesLabel = stringResource(R.string.hud_moves)

    val accessibilitySummary = stringResource(R.string.hud_accessibility_summary, score, target, moves)
    CoffeePanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) { contentDescription = accessibilitySummary },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressCup(progress = progress, cupWidth = SCOREBOARD_CUP_SIZE, cupHeight = SCOREBOARD_CUP_SIZE)
            StatColumn(label = pointsLabel, value = score.toString())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(movesLabel, style = MaterialTheme.typography.labelMedium, color = colors.hudTextMuted)
                Text(
                    text = moves.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TABULAR_NUMBER_FEATURE),
                    fontWeight = FontWeight.ExtraBold,
                    color = if (lowOnMoves) colors.danger else colors.hudText,
                    modifier = Modifier.alpha(movesPulse.value)
                )
            }
        }
    }
}

@Composable
private fun rememberPulseAlpha(enabled: Boolean): Animatable<Float, AnimationVector1D> {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(enabled) {
        if (enabled) {
            while (true) {
                alpha.animateTo(PULSE_MIN_ALPHA, tween(PULSE_STEP_MS))
                alpha.animateTo(1f, tween(PULSE_STEP_MS))
            }
        } else {
            alpha.snapTo(1f)
        }
    }
    return alpha
}

/** UX-05: brief "+N" feedback near the HUD when points increase, instead of only a delayed total. */
@Composable
internal fun FloatingPointsBanner(points: Int) {
    var previousPoints by remember { mutableIntStateOf(points) }
    var delta by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(points) {
        val diff = points - previousPoints
        previousPoints = points
        if (diff > 0) {
            delta = diff
            delay(FLOATING_POINTS_VISIBLE_MS)
            delta = null
        }
    }
    AnimatedVisibility(
        visible = delta != null,
        enter = fadeIn() + slideInVertically { fullHeight -> fullHeight / 2 },
        exit = fadeOut() + slideOutVertically { fullHeight -> -fullHeight }
    ) {
        Text(
            text = "+${delta ?: 0}",
            // Drop shadow keeps the banner readable over the scenery artwork below the board.
            style = MaterialTheme.typography.titleMedium.copy(
                shadow = Shadow(color = Color.Black.copy(alpha = BANNER_SHADOW_ALPHA), blurRadius = BANNER_SHADOW_BLUR)
            ),
            fontWeight = FontWeight.ExtraBold,
            color = CoffeeTheme.colors.success
        )
    }
}

/** UX-05: an animated scale/fade banner for combo and shuffle messages (invalid move uses shake instead, no text). */
@Composable
internal fun ComboBanner(message: String?) {
    AnimatedVisibility(
        visible = message != null,
        enter = scaleIn() + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        Text(
            text = message ?: "",
            style = MaterialTheme.typography.titleMedium.copy(
                shadow = Shadow(color = Color.Black.copy(alpha = BANNER_SHADOW_ALPHA), blurRadius = BANNER_SHADOW_BLUR)
            ),
            fontWeight = FontWeight.ExtraBold,
            color = Cream
        )
    }
}

/** GP-02: shows the optional collect objective's progress as a HUD chip, when the stage has one. */
@Composable
internal fun CollectObjectiveChip(game: GameUiState) {
    if (game.collectPieceType == null) {
        return
    }
    Spacer(modifier = Modifier.height(8.dp))
    StatChip(text = stringResource(R.string.collect_objective_progress, game.collectProgress, game.collectTarget))
}

/** GP-08: true right at the start of stage 1, before the player has made any move or selection. */
internal fun isFirstMoveOfTutorialStage(game: GameUiState): Boolean {
    return game.stageId == FIRST_STAGE_ID &&
        game.moves == game.initialMoves &&
        game.selected == null &&
        !game.animating &&
        !game.finished
}

/** GP-08: short, always-visible tip for the first stage, replacing a paragraph-heavy tutorial screen. */
@Composable
internal fun TutorialHint() {
    CoffeePanel {
        Text(
            text = stringResource(R.string.tutorial_hint_message),
            style = MaterialTheme.typography.bodyMedium,
            color = CoffeeTheme.colors.hudText
        )
    }
}

/**
 * GP-04: fills as pieces are matched; once full, "Degustacao" reveals a valid move for a few
 * seconds - the only barista skill implemented in this pass (see backlog note for Torra
 * Perfeita/Mao Firme/Dose Dupla and the pre-stage equip screen, both deferred).
 */
@Composable
internal fun AromaMeter(aroma: Int, aromaCapacity: Int, onActivate: () -> Unit) {
    val colors = CoffeeTheme.colors
    val fraction = if (aromaCapacity > 0) (aroma.toFloat() / aromaCapacity).coerceIn(0f, 1f) else 0f
    val full = aroma >= aromaCapacity

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.aroma_meter_label),
                style = MaterialTheme.typography.labelMedium,
                color = colors.hudTextMuted
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(AROMA_BAR_HEIGHT)
                    .clip(RoundedCornerShape(AROMA_BAR_SHAPE_PERCENT))
                    .background(colors.progressTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(AROMA_BAR_SHAPE_PERCENT))
                        .background(colors.warning)
                )
            }
        }
        if (full) {
            Spacer(modifier = Modifier.height(6.dp))
            CoffeePrimaryButton(
                text = stringResource(R.string.activate_barista_skill),
                onClick = onActivate,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
