package com.semstress.mobile.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.ui.components.SpriteIconButton

private const val LOGO_WIDTH_FRACTION = 0.78f
private val PlayButtonSize = 96.dp

data class TitleScreenSound(
    val isMusicMuted: Boolean,
    val isSfxMuted: Boolean
)

data class TitleScreenActions(
    val onPlay: () -> Unit,
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

            // Play sprite is self-explanatory — no label needed, contentDescription covers a11y.
            SpriteIconButton(
                spriteRes = R.drawable.icon_play,
                contentDescription = stringResource(R.string.title_play),
                onClick = actions.onPlay,
                size = PlayButtonSize
            )

            Spacer(modifier = Modifier.height(24.dp))

            TitleControlsRow(sound, actions)
        }
    }
}

/** Same sprite controls as the stage menu's top bar, so the chrome reads as one system. */
@Composable
private fun TitleControlsRow(sound: TitleScreenSound, actions: TitleScreenActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SpriteIconButton(
            spriteRes = R.drawable.icon_settings,
            label = stringResource(R.string.title_settings_label),
            contentDescription = stringResource(R.string.open_settings),
            onClick = actions.onOpenSettings
        )
        SpriteIconButton(
            spriteRes = if (sound.isMusicMuted) R.drawable.icon_music_off else R.drawable.icon_music_on,
            label = stringResource(R.string.title_music_label),
            contentDescription = stringResource(
                if (sound.isMusicMuted) R.string.toggle_music_off else R.string.toggle_music_on
            ),
            onClick = actions.onToggleMusic
        )
        SpriteIconButton(
            spriteRes = if (sound.isSfxMuted) R.drawable.icon_toggle_off else R.drawable.icon_toggle_on,
            label = stringResource(R.string.title_sfx_label),
            contentDescription = stringResource(
                if (sound.isSfxMuted) R.string.toggle_sfx_off else R.string.toggle_sfx_on
            ),
            onClick = actions.onToggleSfx
        )
    }
}
