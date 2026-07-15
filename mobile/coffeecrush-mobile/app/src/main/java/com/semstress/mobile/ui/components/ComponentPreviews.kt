package com.semstress.mobile.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.semstress.mobile.ui.theme.CoffeeCrushTheme

/** UX-01: component gallery previews (light + dark) — no automated screenshot-test infra yet, see backlog note. */
@Preview(name = "Components - Light")
@Composable
private fun ComponentGalleryLightPreview() {
    CoffeeCrushTheme(darkTheme = false) {
        ComponentGallery()
    }
}

@Preview(name = "Components - Dark")
@Composable
private fun ComponentGalleryDarkPreview() {
    CoffeeCrushTheme(darkTheme = true) {
        ComponentGallery()
    }
}

@Composable
private fun ComponentGallery() {
    Column(modifier = Modifier.padding(16.dp)) {
        CoffeePrimaryButton(text = "Jogar fase selecionada", onClick = {})
        Spacer(modifier = Modifier.height(8.dp))
        CoffeeSecondaryButton(text = "Voltar ao menu", onClick = {})
        Spacer(modifier = Modifier.height(12.dp))
        CoffeePanel {
            StatRow(stats = listOf("Pontos" to "3200", "Meta" to "9000", "Mov" to "12"))
        }
        Spacer(modifier = Modifier.height(12.dp))
        StatChip(text = "Concluida")
        Spacer(modifier = Modifier.height(12.dp))
        StarRating(stars = 2)
        Spacer(modifier = Modifier.height(12.dp))
        ProgressCup(progress = 0.65f)
        Spacer(modifier = Modifier.height(12.dp))
        StageNodeButtonGallery()
    }
}

@Composable
private fun StageNodeButtonGallery() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StageNodeButton(
            state = StageNodeState(
                stageNumber = 1,
                unlocked = false,
                completed = false,
                selected = false,
                isNextPlayable = false,
                stars = 0
            ),
            stageName = "Fase 1",
            onClick = {}
        )
        StageNodeButton(
            state = StageNodeState(
                stageNumber = 2,
                unlocked = true,
                completed = false,
                selected = true,
                isNextPlayable = false,
                stars = 0
            ),
            stageName = "Fase 2",
            onClick = {}
        )
        StageNodeButton(
            state = StageNodeState(
                stageNumber = 3,
                unlocked = true,
                completed = false,
                selected = false,
                isNextPlayable = true,
                stars = 0
            ),
            stageName = "Fase 3",
            onClick = {}
        )
        StageNodeButton(
            state = StageNodeState(
                stageNumber = 4,
                unlocked = true,
                completed = true,
                selected = false,
                isNextPlayable = false,
                stars = 2
            ),
            stageName = "Fase 4",
            onClick = {}
        )
    }
}
