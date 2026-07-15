package com.semstress.mobile.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.semstress.mobile.ui.theme.Cream

private const val LABEL_SHADOW_ALPHA = 0.7f
private const val LABEL_SHADOW_BLUR = 8f

/** Text style for labels drawn directly over artwork (readable on any background). */
val OverlayLabelStyle = TextStyle(
    color = Cream,
    shadow = Shadow(color = Color.Black.copy(alpha = LABEL_SHADOW_ALPHA), blurRadius = LABEL_SHADOW_BLUR)
)

/**
 * Sprite-based icon button (candy button pack) with an optional label right below it, used by
 * the title screen and the stage menu chrome. Pass `label = null` for self-explanatory sprites
 * (e.g. the play button) — [contentDescription] still covers accessibility.
 */
@Composable
fun SpriteIconButton(
    spriteRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    label: String? = null,
    size: Dp = 46.dp
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painter = painterResource(spriteRes),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
                .clickable(onClick = onClick)
        )
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.merge(OverlayLabelStyle),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
