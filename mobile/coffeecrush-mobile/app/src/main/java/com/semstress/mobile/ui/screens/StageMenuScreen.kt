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
import androidx.compose.material3.Button
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
import com.semstress.mobile.ui.theme.Caramel
import com.semstress.mobile.ui.theme.CoffeeDark
import com.semstress.mobile.ui.theme.CoffeeLight
import com.semstress.mobile.ui.theme.Cream
import com.semstress.mobile.ui.theme.Mint

@Composable
fun StageMenuScreen(
    stages: List<StageConfig>,
    progress: PlayerProgress,
    selectedStageId: Int,
    sound: StageMenuScreenSound,
    actions: StageMenuScreenActions
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Cream, CoffeeLight.copy(alpha = 0.75f), Caramel.copy(alpha = 0.5f))
                )
            )
            .padding(16.dp)
    ) {
        StageMenuHeader(sound, actions)

        Spacer(modifier = Modifier.height(16.dp))

        ProgressCard(progress = progress, totalStages = stages.size)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Selecione sua fase",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CoffeeDark
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

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = actions.onPlaySelectedStage
        ) {
            Text("Jogar fase selecionada")
        }
    }
}

@Composable
private fun StageMenuHeader(sound: StageMenuScreenSound, actions: StageMenuScreenActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(
                text = "Coffee Crush",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = CoffeeDark
            )
            Text(
                text = "Menu de fases",
                style = MaterialTheme.typography.titleMedium,
                color = CoffeeDark.copy(alpha = 0.7f)
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
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = "Progresso geral",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ProgressMetric("Fases", "${progress.completedStagesCount()} / $totalStages")
                ProgressMetric("Pontos", progress.totalScore().toString())
                ProgressMetric("Media", progress.averageScore().toString())
            }
        }
    }
}

@Composable
private fun ProgressMetric(title: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
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
    val container = when {
        !unlocked -> MaterialTheme.colorScheme.surfaceVariant
        selected -> Caramel.copy(alpha = 0.35f)
        completed -> Mint.copy(alpha = 0.22f)
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
                    Icon(Icons.Default.Lock, contentDescription = "Bloqueada")
                }
            }

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusChip(text = if (unlocked) "Liberada" else "Bloqueada")
                if (completed) {
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusChip(text = "Concluida")
                }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
