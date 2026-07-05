package com.semstress.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semstress.mobile.domain.PlayerProgress
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.ui.theme.CoffeeSemanticColors
import com.semstress.mobile.ui.theme.CoffeeTheme

private const val REGION_HEADER_PILL_PERCENT = 50
private val REGION_ACCENTS: List<(CoffeeSemanticColors) -> Color> = listOf(
    { it.surfaceBoardBorder },
    { it.success },
    { it.warning },
    { it.pieceHighlight },
    { it.danger }
)
private val NODE_INDENT = 48.dp

/**
 * UX-04b: a vertically scrolling "Jornada do Café" replacing the flat stage grid (UX-04a) - stages
 * grouped into named regions (from [StageConfig.region], falling back to a single ungrouped region),
 * laid out in a zig-zag trail with a connecting line, instead of a uniform grid of cards.
 *
 * Scope: region-specific background art and music swap-on-scroll (per ui-ux.md §3) are deferred;
 * regions are distinguished here only by an accent color and header, reusing today's single menu track.
 */
@Composable
fun JourneyMap(
    stages: List<StageConfig>,
    progress: PlayerProgress,
    selectedStageId: Int,
    onSelectStage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CoffeeTheme.colors
    val regions = stages.groupBy { it.region ?: "Jornada do Café" }
    val regionNames = regions.keys.toList()

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        regionNames.forEachIndexed { regionIndex, regionName ->
            val accent = REGION_ACCENTS[regionIndex % REGION_ACCENTS.size](colors)
            item(key = "region-$regionName") {
                RegionHeader(name = regionName, accent = accent)
            }
            items(regions.getValue(regionName), key = { it.id }) { stage ->
                val nodeIndex = regions.getValue(regionName).indexOf(stage)
                JourneyNode(
                    stage = stage,
                    status = StageCardStatus(
                        unlocked = progress.isUnlocked(stage.id),
                        selected = selectedStageId == stage.id,
                        completed = progress.scoreFor(stage.id) > 0,
                        stars = progress.starsFor(stage.id)
                    ),
                    accent = accent,
                    alignEnd = nodeIndex % 2 == 1,
                    onClick = { if (progress.isUnlocked(stage.id)) onSelectStage(stage.id) }
                )
            }
        }
    }
}

@Composable
private fun RegionHeader(name: String, accent: Color) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = CoffeeTheme.colors.hudText
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .height(4.dp)
                .width(64.dp)
                .clip(RoundedCornerShape(REGION_HEADER_PILL_PERCENT))
                .background(accent)
        )
    }
}

/** A single stage card offset left/right along the trail, connected by a vertical accent line. */
@Composable
private fun JourneyNode(
    stage: StageConfig,
    status: StageCardStatus,
    accent: Color,
    alignEnd: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxHeight()
                .width(2.dp)
                .background(accent.copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .align(if (alignEnd) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(
                    start = if (alignEnd) NODE_INDENT else 0.dp,
                    end = if (alignEnd) 0.dp else NODE_INDENT
                )
        ) {
            StageCard(stage = stage, status = status, onClick = onClick)
        }
    }
}
