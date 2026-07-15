package com.semstress.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.domain.PlayerProgress
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.ui.components.SpriteIconButton
import com.semstress.mobile.ui.theme.CoffeeDark
import com.semstress.mobile.ui.theme.Latte
import java.time.LocalDate

private val LogoHeight = 56.dp
private const val PILL_SHAPE_PERCENT = 50
private val PillShape = RoundedCornerShape(PILL_SHAPE_PERCENT)
private val BottomBarShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private const val BOTTOM_BAR_ALPHA = 0.92f
private const val PILL_BACKGROUND_ALPHA = 0.92f
private const val PILL_DISABLED_ALPHA = 0.45f

/**
 * Full-screen stage selector: the constellation [JourneyMap] fills the whole screen and the
 * chrome (logo, sprite buttons, bottom mode bar) floats on top of it. Empty overlay areas have
 * no pointer handling, so map drag gestures pass through everywhere except the controls.
 * Tapping an unlocked node enters the stage directly — there is no separate play CTA.
 */
@Composable
fun StageMenuScreen(
    stages: List<StageConfig>,
    progress: PlayerProgress,
    selectedStageId: Int,
    sound: StageMenuScreenSound,
    actions: StageMenuScreenActions
) {
    // Standard floating-bars-over-scrollable treatment: measure the real height of the top and
    // bottom chrome and reserve it as the map's content padding, so the first/last nodes rest
    // clear of the bars while the map still scrolls behind them.
    val density = LocalDensity.current
    var topBarHeight by remember { mutableStateOf(0.dp) }
    var bottomBarHeight by remember { mutableStateOf(0.dp) }
    val safeInsets = WindowInsets.safeDrawing.asPaddingValues()

    Box(modifier = Modifier.fillMaxSize()) {
        JourneyMap(
            content = JourneyMapContent(stages, progress, selectedStageId),
            onSelectStage = { stageId ->
                actions.onSelectStage(stageId)
                actions.onPlayStage(stageId)
            },
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = safeInsets.calculateTopPadding() + topBarHeight,
                bottom = safeInsets.calculateBottomPadding() + bottomBarHeight
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .onSizeChanged { topBarHeight = with(density) { it.height.toDp() } }
            ) {
                MenuTopBar(sound, actions)
            }

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier.onSizeChanged { bottomBarHeight = with(density) { it.height.toDp() } }
            ) {
                BottomModeBar(progress = progress, actions = actions)
            }
        }
    }
}

@Composable
private fun MenuTopBar(sound: StageMenuScreenSound, actions: StageMenuScreenActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Image(
            painter = painterResource(R.drawable.logo_title),
            contentDescription = stringResource(R.string.app_title),
            modifier = Modifier.height(LogoHeight),
            contentScale = ContentScale.Fit
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SpriteIconButton(
                spriteRes = R.drawable.icon_settings,
                label = stringResource(R.string.title_settings_label),
                contentDescription = stringResource(R.string.open_settings),
                onClick = actions.onOpenSettings
            )
            SpriteIconButton(
                spriteRes = if (sound.isMusicMuted) R.drawable.icon_toggle_off else R.drawable.icon_toggle_on,
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
}

/**
 * GP-05: fixed bottom bar (the mobile-game staple) hosting the extra game modes for now — the
 * daily challenge (seeded, shared board, limited attempts) and zen mode. More entries later.
 */
@Composable
private fun BottomModeBar(progress: PlayerProgress, actions: StageMenuScreenActions) {
    val today = remember { LocalDate.now().toEpochDay() }
    val attemptsRemaining = progress.dailyAttemptsRemaining(today)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(BottomBarShape)
            .background(CoffeeDark.copy(alpha = BOTTOM_BAR_ALPHA))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MenuPillButton(
            text = stringResource(R.string.zen_mode),
            onClick = actions.onPlayZenMode,
            modifier = Modifier.weight(1f)
        )
        MenuPillButton(
            text = stringResource(R.string.daily_challenge, attemptsRemaining),
            onClick = actions.onPlayDailyChallenge,
            modifier = Modifier.weight(1f),
            enabled = attemptsRemaining > 0
        )
    }
}

/** Filled pill (same treatment as the title screen's zen button) so it reads over the artwork. */
@Composable
private fun MenuPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val backgroundAlpha = if (enabled) PILL_BACKGROUND_ALPHA else PILL_DISABLED_ALPHA
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(Latte.copy(alpha = backgroundAlpha))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CoffeeDark
        )
    }
}
