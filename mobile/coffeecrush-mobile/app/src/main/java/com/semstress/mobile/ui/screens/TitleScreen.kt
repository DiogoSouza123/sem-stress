package com.semstress.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.ui.components.CoffeePrimaryButton
import com.semstress.mobile.ui.theme.CoffeeDark
import com.semstress.mobile.ui.theme.Cream
import com.semstress.mobile.ui.theme.Latte

private const val PILL_SHAPE_PERCENT = 50
private val PillShape = RoundedCornerShape(PILL_SHAPE_PERCENT)
private const val LOGO_WIDTH_FRACTION = 0.78f

data class TitleScreenSound(
    val isMusicMuted: Boolean,
    val isSfxMuted: Boolean
)

data class TitleScreenActions(
    val onPlay: () -> Unit,
    val onPlayZenMode: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onToggleMusic: () -> Unit,
    val onToggleSfx: () -> Unit
)

/**
 * Title screen shown before the stage menu. The artwork is a single swappable drawable
 * ([R.drawable.bg_title]) with NO baked-in text or buttons, so seasonal art (or an animated
 * background later) can replace it without touching this screen: every interactive element is
 * composed on top at fixed positions. Overlay text uses hardcoded cream-on-shadow styling
 * (instead of theme colors) because it must stay readable over arbitrary artwork in both
 * light and dark system themes.
 */
@Composable
fun TitleScreen(
    sound: TitleScreenSound,
    actions: TitleScreenActions
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.bg_title),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo is a swappable transparent PNG layered over the background art,
            // same replacement scheme as R.drawable.bg_title.
            Image(
                painter = painterResource(R.drawable.logo_title),
                contentDescription = stringResource(R.string.app_title),
                modifier = Modifier.fillMaxWidth(LOGO_WIDTH_FRACTION),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.weight(1f))

            CoffeePrimaryButton(
                text = stringResource(R.string.title_play),
                onClick = actions.onPlay,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            ZenModeButton(onClick = actions.onPlayZenMode)

            Spacer(modifier = Modifier.height(24.dp))

            TitleControlsRow(sound, actions)
        }
    }
}

/** Filled (not outlined) so it stays legible over any background artwork. */
@Composable
private fun ZenModeButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PillShape)
            .background(Latte.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.zen_mode),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CoffeeDark
        )
    }
}

@Composable
private fun TitleControlsRow(sound: TitleScreenSound, actions: TitleScreenActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TitleControl(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.title_settings_label),
            contentDescription = stringResource(R.string.open_settings),
            onClick = actions.onOpenSettings
        )
        TitleControl(
            icon = if (sound.isMusicMuted) Icons.Filled.MusicOff else Icons.Filled.MusicNote,
            label = stringResource(R.string.title_music_label),
            contentDescription = stringResource(
                if (sound.isMusicMuted) R.string.toggle_music_off else R.string.toggle_music_on
            ),
            onClick = actions.onToggleMusic
        )
        TitleControl(
            icon = if (sound.isSfxMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
            label = stringResource(R.string.title_sfx_label),
            contentDescription = stringResource(
                if (sound.isSfxMuted) R.string.toggle_sfx_off else R.string.toggle_sfx_on
            ),
            onClick = actions.onToggleSfx
        )
    }
}

@Composable
private fun TitleControl(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(Cream.copy(alpha = 0.9f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = CoffeeDark
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.merge(
                TextStyle(
                    color = Cream,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 8f)
                )
            ),
            fontWeight = FontWeight.SemiBold
        )
    }
}
