package com.semstress.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.semstress.mobile.ui.components.StatChip
import com.semstress.mobile.ui.components.StatRow
import com.semstress.mobile.ui.theme.CoffeeTheme

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

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(stages) { stage ->
                val unlocked = progress.isUnlocked(stage.id)
                val selected = selectedStageId == stage.id
                val completed = progress.scoreFor(stage.id) > 0

                StageCard(
                    stage = stage,
                    unlocked = unlocked,
                    selected = selected,
                    completed = completed,
                    onClick = { if (unlocked) actions.onSelectStage(stage.id) }
                )
            }
        }

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
                averageLabel to progress.averageScore().toString()
            )
        )
    }
}

@Composable
private fun StageCard(
    stage: StageConfig,
    unlocked: Boolean,
    selected: Boolean,
    completed: Boolean,
    onClick: () -> Unit
) {
    val colors = CoffeeTheme.colors
    val container = when {
        !unlocked -> MaterialTheme.colorScheme.surfaceVariant
        selected -> colors.surfaceBoardBorder.copy(alpha = 0.35f)
        completed -> colors.success.copy(alpha = 0.22f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        onClick = onClick,
        enabled = unlocked,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StageCardBadge(stage = stage, unlocked = unlocked)

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

            Spacer(modifier = Modifier.height(8.dp))
            StageCardStatusRow(unlocked = unlocked, completed = completed)
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
