package com.semstress.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.domain.PlayerProgress
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.ui.components.CoffeePanel
import com.semstress.mobile.ui.components.CoffeePrimaryButton
import com.semstress.mobile.ui.components.StarRating
import com.semstress.mobile.ui.components.StatChip
import com.semstress.mobile.ui.components.StatRow
import com.semstress.mobile.ui.theme.CoffeeTheme

private const val MAX_STARS_PER_STAGE = 3

@Composable
fun StageMenuScreen(
    stages: List<StageConfig>,
    progress: PlayerProgress,
    selectedStageId: Int,
    sound: StageMenuScreenSound,
    actions: StageMenuScreenActions
) {
    val colors = CoffeeTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        colors.panelBackground,
                        colors.surfaceBoard.copy(alpha = 0.75f),
                        colors.surfaceBoardBorder.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        StageMenuHeader(sound, actions)

        Spacer(modifier = Modifier.height(16.dp))

        ProgressCard(progress = progress, totalStages = stages.size)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.select_your_stage),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = colors.hudText
        )

        Spacer(modifier = Modifier.height(8.dp))

        JourneyMap(
            stages = stages,
            progress = progress,
            selectedStageId = selectedStageId,
            onSelectStage = actions.onSelectStage,
            modifier = Modifier.weight(1f)
        )

        CoffeePrimaryButton(
            text = stringResource(R.string.play_selected_stage),
            onClick = actions.onPlaySelectedStage,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StageMenuHeader(sound: StageMenuScreenSound, actions: StageMenuScreenActions) {
    val colors = CoffeeTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = colors.hudText
            )
            Text(
                text = stringResource(R.string.menu_subtitle),
                style = MaterialTheme.typography.titleMedium,
                color = colors.hudTextMuted
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            OutlinedButton(onClick = actions.onToggleMusic) {
                Text(stringResource(if (sound.isMusicMuted) R.string.toggle_music_off else R.string.toggle_music_on))
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(onClick = actions.onToggleSfx) {
                Text(stringResource(if (sound.isSfxMuted) R.string.toggle_sfx_off else R.string.toggle_sfx_on))
            }
        }
    }
}

@Composable
private fun ProgressCard(progress: PlayerProgress, totalStages: Int) {
    val stagesLabel = stringResource(R.string.progress_stages)
    val pointsLabel = stringResource(R.string.progress_points)
    val averageLabel = stringResource(R.string.progress_average)
    val starsLabel = stringResource(R.string.progress_stars)
    CoffeePanel {
        Text(
            text = stringResource(R.string.progress_overall),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        StatRow(
            stats = listOf(
                stagesLabel to "${progress.completedStagesCount()} / $totalStages",
                pointsLabel to progress.totalScore().toString(),
                averageLabel to progress.averageScore().toString(),
                starsLabel to "${progress.totalStars()} / ${totalStages * MAX_STARS_PER_STAGE}"
            )
        )
    }
}

/** GP-03: bundles a stage card's derived progress flags to keep [StageCard]'s arity in check. */
internal data class StageCardStatus(
    val unlocked: Boolean,
    val selected: Boolean,
    val completed: Boolean,
    val stars: Int
)

@Composable
internal fun StageCard(
    stage: StageConfig,
    status: StageCardStatus,
    onClick: () -> Unit
) {
    val colors = CoffeeTheme.colors
    val container = when {
        !status.unlocked -> MaterialTheme.colorScheme.surfaceVariant
        status.selected -> colors.surfaceBoardBorder.copy(alpha = 0.35f)
        status.completed -> colors.success.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        enabled = status.unlocked,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StageCardBadge(stage = stage, unlocked = status.unlocked)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stage.name,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stage.description,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall
            )

            if (status.completed) {
                Spacer(modifier = Modifier.height(4.dp))
                StarRating(stars = status.stars, starSize = 14.dp)
            }

            Spacer(modifier = Modifier.height(8.dp))
            StageCardStatusRow(unlocked = status.unlocked, completed = status.completed)
        }
    }
}

@Composable
private fun StageCardBadge(stage: StageConfig, unlocked: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (unlocked) {
            Text(
                text = stage.id.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
        } else {
            Icon(Icons.Default.Lock, contentDescription = stringResource(R.string.stage_status_locked))
        }
    }
}

@Composable
private fun StageCardStatusRow(unlocked: Boolean, completed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val statusText = if (unlocked) {
            stringResource(R.string.stage_status_unlocked)
        } else {
            stringResource(R.string.stage_status_locked)
        }
        StatChip(text = statusText)
        if (completed) {
            Spacer(modifier = Modifier.width(6.dp))
            StatChip(text = stringResource(R.string.stage_status_completed))
        }
    }
}
