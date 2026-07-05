package com.semstress.mobile.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semstress.mobile.ui.theme.CoffeeTheme
import com.semstress.mobile.ui.theme.TABULAR_NUMBER_FEATURE

/**
 * UX-01: canonical card surface with a subtle kraft-paper-like border, used for
 * HUD panels (scoreboard, progress card) instead of ad hoc [Card] usages per screen.
 */
@Composable
fun CoffeePanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CoffeeTheme.colors.panelBackground),
        border = BorderStroke(1.dp, CoffeeTheme.colors.panelBorder),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/** UX-01: a labelled statistic used inside [CoffeePanel]s (replaces per-screen `Metric` composables). */
@Composable
fun StatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = CoffeeTheme.colors.hudTextMuted)
        Text(
            value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = TABULAR_NUMBER_FEATURE),
            fontWeight = FontWeight.ExtraBold
        )
    }
}

/** UX-01: a row of [StatColumn]s spaced evenly, the common layout for HUD/progress panels. */
@Composable
fun StatRow(stats: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        stats.forEach { (label, value) -> StatColumn(label = label, value = value) }
    }
}
