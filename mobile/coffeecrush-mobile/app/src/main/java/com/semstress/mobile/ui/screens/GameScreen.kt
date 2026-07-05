package com.semstress.mobile.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.audio.SfxEffect
import com.semstress.mobile.debug.DebugMenuState
import com.semstress.mobile.ui.components.CoffeeIconButton
import com.semstress.mobile.ui.components.StarRating
import com.semstress.mobile.ui.sprites.SpriteAtlas
import com.semstress.mobile.ui.state.GameUiState
import com.semstress.mobile.ui.state.GameViewModel
import com.semstress.mobile.ui.theme.CoffeeTheme

@Composable
fun GameScreen(
    game: GameUiState,
    sound: GameScreenSound,
    spriteAtlas: SpriteAtlas?,
    actions: GameScreenActions,
    debugTools: GameScreenDebugTools
) {
    var showExitConfirmation by remember { mutableStateOf(false) }
    val requestExit: () -> Unit = {
        if (game.finished) actions.onBackToMenu() else showExitConfirmation = true
    }

    BackHandler(enabled = !game.finished, onBack = requestExit)
    GameSfxAndHaptics(game, sound)

    val colors = CoffeeTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.surfaceBoardBorder.copy(alpha = 0.4f), colors.surfaceBoard, colors.panelBackground)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GameHeader(game, sound, actions, requestExit)
        DebugMenuTrigger(debugTools)

        Spacer(modifier = Modifier.height(12.dp))

        Scoreboard(score = game.points, target = game.target, moves = game.moves)
        FloatingPointsBanner(points = game.points)
        ComboBanner(message = game.message?.takeIf { it != GameViewModel.INVALID_MOVE_MESSAGE })

        Spacer(modifier = Modifier.height(12.dp))

        val shakingPositions = game.invalidSwap?.let { (first, second) -> setOf(first, second) } ?: emptySet()
        BoardCanvas(
            board = game.board,
            selection = BoardSelectionState(
                selected = game.selected,
                highlighted = game.highlightedMatches,
                exploding = game.explodingMatches,
                shaking = shakingPositions,
                invalidMoveNonce = game.invalidMoveNonce
            ),
            spriteAtlas = spriteAtlas,
            onCellTap = actions.onCellTap,
            onCellDragSwap = actions.onCellDragSwap
        )
    }

    if (showExitConfirmation) {
        ExitConfirmationDialog(
            onConfirm = {
                showExitConfirmation = false
                actions.onBackToMenu()
            },
            onDismiss = { showExitConfirmation = false }
        )
    }

    if (game.finished) {
        GameResultDialog(game, actions.onReplayStage, actions.onBackToMenu)
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

/** UX-05: system controls (back/music/sfx) as discreet top icon buttons instead of full-width text buttons. */
@Composable
private fun GameHeader(
    game: GameUiState,
    sound: GameScreenSound,
    actions: GameScreenActions,
    onBackClick: () -> Unit
) {
    val colors = CoffeeTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoffeeIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back_to_menu),
            onClick = onBackClick
        )

        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = game.stageName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = colors.hudText
            )
            Text(
                text = game.stageDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.hudTextMuted
            )
        }

        Row {
            CoffeeIconButton(
                icon = if (sound.isMusicMuted) Icons.Filled.MusicOff else Icons.Filled.MusicNote,
                contentDescription = stringResource(
                    if (sound.isMusicMuted) R.string.toggle_music_off else R.string.toggle_music_on
                ),
                onClick = actions.onToggleMusic
            )
            val sfxIcon = if (sound.isSfxMuted) {
                Icons.AutoMirrored.Filled.VolumeOff
            } else {
                Icons.AutoMirrored.Filled.VolumeUp
            }
            CoffeeIconButton(
                icon = sfxIcon,
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
        if (game.message == GameViewModel.INVALID_MOVE_MESSAGE) {
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

@Composable
private fun ExitConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_game_confirmation_title)) },
        text = { Text(stringResource(R.string.exit_game_confirmation_message)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.exit_game_confirmation_confirm))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.exit_game_confirmation_dismiss))
            }
        }
    )
}

@Composable
private fun GameResultDialog(game: GameUiState, onReplayStage: () -> Unit, onBackToMenu: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = stringResource(if (game.won) R.string.stage_won_title else R.string.stage_lost_title),
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column {
                Text(
                    stringResource(
                        R.string.stage_result_summary,
                        game.points,
                        game.target,
                        game.moves
                    )
                )
                if (game.won) {
                    Spacer(modifier = Modifier.height(8.dp))
                    StarRating(stars = game.starsEarned)
                }
            }
        },
        confirmButton = {
            Button(onClick = onReplayStage) {
                Text(stringResource(R.string.play_again))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onBackToMenu) {
                Text(stringResource(R.string.back_to_menu))
            }
        }
    )
}

internal fun fallbackPieceSymbol(value: Int): String {
    return when ((value % 6 + 6) % 6) {
        0 -> "☕"
        1 -> "🫘"
        2 -> "🥛"
        3 -> "🍪"
        4 -> "🟤"
        else -> "⭐"
    }
}
