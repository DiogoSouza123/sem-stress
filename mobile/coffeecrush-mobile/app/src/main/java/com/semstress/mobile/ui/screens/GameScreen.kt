package com.semstress.mobile.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.semstress.mobile.R
import com.semstress.mobile.audio.SfxEffect
import com.semstress.mobile.debug.DebugMenuState
import com.semstress.mobile.engine.EmptyCupState
import com.semstress.mobile.engine.Match3Engine
import com.semstress.mobile.ui.components.CoffeePanel
import com.semstress.mobile.ui.components.CoffeePrimaryButton
import com.semstress.mobile.ui.components.CoffeeSecondaryButton
import com.semstress.mobile.ui.components.OverlayLabelStyle
import com.semstress.mobile.ui.components.SpriteIconButton
import com.semstress.mobile.ui.sprites.SpriteAtlas
import com.semstress.mobile.ui.state.GameUiState
import com.semstress.mobile.ui.state.GameViewModel
import com.semstress.mobile.ui.theme.CoffeeTheme

private val FeedbackSlotHeight = 40.dp

@Composable
fun GameScreen(
    game: GameUiState,
    sound: GameScreenSound,
    spriteAtlas: SpriteAtlas?,
    actions: GameScreenActions,
    debugTools: GameScreenDebugTools
) {
    var showPauseMenu by remember { mutableStateOf(false) }
    val requestPause: () -> Unit = {
        if (game.finished) actions.onBackToMenu() else showPauseMenu = true
    }

    BackHandler(enabled = !game.finished, onBack = requestPause)
    GameSfxAndHaptics(game, sound)

    Box(modifier = Modifier.fillMaxSize()) {
        // Swappable full-bleed scenery (same scheme as bg_title / the stage map): the artwork has
        // no baked-in UI, all HUD elements float on top.
        Image(
            painter = painterResource(R.drawable.bg_game),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            GameHeader(game, sound, actions, requestPause)
            DebugMenuTrigger(debugTools)

            Spacer(modifier = Modifier.height(12.dp))

            Scoreboard(score = game.points, target = game.target, moves = game.moves, isZenMode = game.isZenMode)
            if (!game.isZenMode) {
                Spacer(modifier = Modifier.height(8.dp))
                AromaMeter(
                    aroma = game.aroma,
                    aromaCapacity = game.aromaCapacity,
                    onActivate = actions.onActivateBaristaSkill
                )
            }
            CollectObjectiveChip(game)

            Spacer(modifier = Modifier.height(12.dp))

            BoardSection(game, spriteAtlas, actions, sound.isSymbolModeEnabled)

            FeedbackSlot(game)
        }
    }

    if (showPauseMenu) {
        PauseDialog(
            onContinue = { showPauseMenu = false },
            onRestart = {
                showPauseMenu = false
                actions.onReplayStage()
            },
            onExit = {
                showPauseMenu = false
                actions.onBackToMenu()
            }
        )
    }

    if (game.finished) {
        GameResultOverlay(game, actions.onReplayStage, actions.onBackToMenu)
    }
}

/**
 * Fixed-height feedback slot BELOW the board: the +points and combo banners appear inside this
 * always-reserved space, so they can never push the board around when they come and go.
 */
@Composable
private fun FeedbackSlot(game: GameUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(FeedbackSlotHeight),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FloatingPointsBanner(points = game.points)
        ComboBanner(message = resolveGameMessage(game.message))
    }
}

@Composable
private fun BoardSection(
    game: GameUiState,
    spriteAtlas: SpriteAtlas?,
    actions: GameScreenActions,
    symbolModeEnabled: Boolean
) {
    val shakingPositions = game.invalidSwap?.let { (first, second) -> setOf(first, second) } ?: emptySet()
    val hintedPositions = game.hintMove?.let { (first, second) -> setOf(first, second) } ?: emptySet()
    BoardCanvas(
        board = game.board,
        selection = BoardSelectionState(
            selected = game.selected,
            highlighted = game.highlightedMatches,
            exploding = game.explodingMatches,
            shaking = shakingPositions,
            invalidMoveNonce = game.invalidMoveNonce,
            hinted = hintedPositions,
            symbolModeEnabled = symbolModeEnabled,
            cupScorePerPiece = game.scorePerPiece
        ),
        spriteAtlas = spriteAtlas,
        onCellTap = actions.onCellTap,
        onCellDragSwap = actions.onCellDragSwap
    )

    if (isFirstMoveOfTutorialStage(game)) {
        Spacer(modifier = Modifier.height(8.dp))
        TutorialHint()
    }
}

/** CQ-03: shows the debug-panel entry point when [GameScreenDebugTools.host] is available (debug builds only). */
@Composable
private fun DebugMenuTrigger(debugTools: GameScreenDebugTools) {
    var showDebugMenu by remember { mutableStateOf(false) }
    if (debugTools.host.isAvailable) {
        OutlinedButton(onClick = { showDebugMenu = true }) {
            Text("Debug")
        }
    }
    debugTools.host.Menu(
        state = DebugMenuState(visible = showDebugMenu),
        actions = debugTools.actions,
        featureFlags = debugTools.featureFlags,
        onDismiss = { showDebugMenu = false }
    )
}

/** UX-05: system controls use the same sprite-button chrome as the title and stage selector. */
@Composable
private fun GameHeader(
    game: GameUiState,
    sound: GameScreenSound,
    actions: GameScreenActions,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        SpriteIconButton(
            spriteRes = R.drawable.icon_back,
            contentDescription = stringResource(R.string.back_to_menu),
            onClick = onBackClick
        )

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            // Cream-on-shadow so the title stays readable over the scenery artwork.
            Text(
                text = game.stageName,
                style = MaterialTheme.typography.headlineSmall.merge(OverlayLabelStyle),
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = game.stageDescription,
                style = MaterialTheme.typography.bodyMedium.merge(OverlayLabelStyle)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
}

/** RR-22: reacts to game-state transitions with a short SFX + a subtle haptic pulse. */
@Composable
private fun GameSfxAndHaptics(game: GameUiState, sound: GameScreenSound) {
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(game.selected) {
        if (game.selected != null && !sound.isSfxMuted) {
            sound.sfxPlayer.play(SfxEffect.SELECT)
        }
    }
    LaunchedEffect(game.animating) {
        if (game.animating && !sound.isSfxMuted) {
            sound.sfxPlayer.play(SfxEffect.SWAP)
        }
    }
    LaunchedEffect(game.explodingMatches) {
        if (game.explodingMatches.isNotEmpty()) {
            if (!sound.isSfxMuted) {
                sound.sfxPlayer.play(SfxEffect.MATCH)
            }
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LaunchedEffect(game.message) {
        if (game.message == GameViewModel.INVALID_MOVE_MESSAGE_KEY) {
            if (!sound.isSfxMuted) {
                sound.sfxPlayer.play(SfxEffect.INVALID_MOVE)
            }
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
    LaunchedEffect(game.finished, game.won) {
        if (game.finished && game.won) {
            if (!sound.isSfxMuted) {
                sound.sfxPlayer.play(SfxEffect.VICTORY)
            }
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }
}

/** UX-10: pause overlay (continuar/recomecar/sair) shown from the back gesture/button mid-game. */
@Composable
private fun PauseDialog(onContinue: () -> Unit, onRestart: () -> Unit, onExit: () -> Unit) {
    Dialog(onDismissRequest = onContinue) {
        CoffeePanel {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.pause_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = CoffeeTheme.colors.hudText
                )
                Spacer(modifier = Modifier.height(16.dp))
                CoffeePrimaryButton(
                    text = stringResource(R.string.pause_continue),
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                CoffeeSecondaryButton(
                    text = stringResource(R.string.pause_restart),
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                CoffeeSecondaryButton(
                    text = stringResource(R.string.pause_exit),
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** UX-12: [GameUiState.message] only carries a stable key - the warm, localized copy lives in strings.xml. */
@Composable
private fun resolveGameMessage(key: String?): String? {
    return when {
        key == null || key == GameViewModel.INVALID_MOVE_MESSAGE_KEY -> null
        key == GameViewModel.SHUFFLED_MESSAGE_KEY -> stringResource(R.string.game_message_shuffled)
        key.startsWith(GameViewModel.COMBO_MESSAGE_KEY_PREFIX) -> {
            val cascades = key.removePrefix(GameViewModel.COMBO_MESSAGE_KEY_PREFIX).toIntOrNull()
            cascades?.let { stringResource(R.string.game_message_combo, it) }
        }
        else -> null
    }
}

internal fun fallbackPieceSymbol(value: Int): String {
    return when {
        value == Match3Engine.SPECIAL_GRINDER -> "⚙️"
        value == Match3Engine.SPECIAL_FRENCH_PRESS -> "⬇️"
        EmptyCupState.matches(value) -> "💨"
        else -> when ((value % 6 + 6) % 6) {
            0 -> "☕"
            1 -> "🫘"
            2 -> "🥛"
            3 -> "🍪"
            4 -> "🟤"
            else -> "⭐"
        }
    }
}
