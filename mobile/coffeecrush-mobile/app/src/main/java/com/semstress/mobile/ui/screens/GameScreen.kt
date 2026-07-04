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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.ui.sprites.SpriteAtlas
import com.semstress.mobile.ui.state.GameUiState
import com.semstress.mobile.ui.theme.Caramel
import com.semstress.mobile.ui.theme.CoffeeDark
import com.semstress.mobile.ui.theme.Cream
import com.semstress.mobile.ui.theme.Latte

@Composable
fun GameScreen(
    game: GameUiState,
    isMusicMuted: Boolean,
    spriteAtlas: SpriteAtlas?,
    actions: GameScreenActions
) {
    var showExitConfirmation by remember { mutableStateOf(false) }
    val requestExit: () -> Unit = {
        if (game.finished) {
            actions.onBackToMenu()
        } else {
            showExitConfirmation = true
        }
    }

    BackHandler(enabled = !game.finished, onBack = requestExit)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Caramel.copy(alpha = 0.4f), Latte, Cream)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        GameHeader(game, isMusicMuted, actions.onToggleMusic)

        Spacer(modifier = Modifier.height(12.dp))

        Scoreboard(score = game.points, target = game.target, moves = game.moves)

        game.message?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = message, style = MaterialTheme.typography.labelLarge, color = CoffeeDark)
        }

        Spacer(modifier = Modifier.height(12.dp))

        BoardCanvas(
            board = game.board,
            selection = BoardSelectionState(game.selected, game.highlightedMatches, game.explodingMatches),
            spriteAtlas = spriteAtlas,
            onCellTap = actions.onCellTap,
            onCellDragSwap = actions.onCellDragSwap
        )

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = requestExit,
            enabled = !game.animating
        ) {
            Text("Voltar ao menu")
        }
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

@Composable
private fun GameHeader(game: GameUiState, isMusicMuted: Boolean, onToggleMusic: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.stageName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = CoffeeDark
            )
            Text(
                text = game.stageDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = CoffeeDark.copy(alpha = 0.75f)
            )
        }

        OutlinedButton(onClick = onToggleMusic) {
            Text(if (isMusicMuted) "Som: OFF" else "Som: ON")
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
                text = if (game.won) "Fase concluida!" else "Fim da rodada",
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Text("Pontuacao: ${game.points}\nMeta: ${game.target}\nMovimentos restantes: ${game.moves}")
        },
        confirmButton = {
            Button(onClick = onReplayStage) {
                Text("Jogar novamente")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onBackToMenu) {
                Text("Voltar ao menu")
            }
        }
    )
}

@Composable
private fun Scoreboard(score: Int, target: Int, moves: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Metric("Pontos", score.toString())
            Metric("Meta", target.toString())
            Metric("Mov", moves.toString())
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
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
