package com.semstress.mobile.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.semstress.mobile.ui.theme.CoffeeTheme

private const val MAX_STARS = 3

/** UX-01 / GP-03: renders `stars` filled out of a fixed total of 3. */
@Composable
fun StarRating(
    stars: Int,
    modifier: Modifier = Modifier,
    starSize: Dp = 20.dp
) {
    val filled = stars.coerceIn(0, MAX_STARS)
    Row(modifier = modifier) {
        repeat(MAX_STARS) { index ->
            if (index < filled) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = CoffeeTheme.colors.warning,
                    modifier = Modifier.size(starSize)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.StarOutline,
                    contentDescription = null,
                    tint = CoffeeTheme.colors.hudTextMuted,
                    modifier = Modifier.size(starSize)
                )
            }
        }
    }
}
