package com.semstress.mobile.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.semstress.mobile.R
import com.semstress.mobile.domain.Position
import com.semstress.mobile.engine.Match3Engine
import com.semstress.mobile.ui.sprites.GameSpritePack
import com.semstress.mobile.ui.sprites.rememberGameSpritePack
import com.semstress.mobile.ui.state.GameUiState
import com.semstress.mobile.ui.theme.Caramel
import com.semstress.mobile.ui.theme.CoffeeDark
import com.semstress.mobile.ui.theme.Cream
import com.semstress.mobile.ui.theme.Gold
import com.semstress.mobile.ui.theme.Latte
import com.semstress.mobile.ui.theme.Mint
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs

@Composable
fun GameScreen(
    game: GameUiState,
    isMusicMuted: Boolean,
    onCellTap: (row: Int, col: Int) -> Unit,
    onCellDragSwap: (fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) -> Unit,
    onBackToMenu: () -> Unit,
    onReplayStage: () -> Unit,
    onToggleMusic: () -> Unit
) {
    val spritePack = rememberGameSpritePack()

    var showExitConfirmation by remember { mutableStateOf(false) }
    val requestExit: () -> Unit = {
        if (game.finished) {
            onBackToMenu()
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

        Spacer(modifier = Modifier.height(12.dp))

        Scoreboard(
            score = game.points,
            target = game.target,
            moves = game.moves
        )

        game.message?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge,
                color = CoffeeDark
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        BoardSection(
            game = game,
            spritePack = spritePack,
            onCellTap = onCellTap,
            onCellDragSwap = onCellDragSwap
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
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text(stringResource(R.string.exit_game_confirmation_title)) },
            text = { Text(stringResource(R.string.exit_game_confirmation_message)) },
            confirmButton = {
                Button(onClick = {
                    showExitConfirmation = false
                    onBackToMenu()
                }) {
                    Text(stringResource(R.string.exit_game_confirmation_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitConfirmation = false }) {
                    Text(stringResource(R.string.exit_game_confirmation_dismiss))
                }
            }
        )
    }

    if (game.finished) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = if (game.won) "Fase concluida!" else "Fim da rodada",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    "Pontuacao: ${game.points}\nMeta: ${game.target}\nMovimentos restantes: ${game.moves}"
                )
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

/** RR-20 rollout switch: flip to `false` to fall back to the per-cell Composable board. */
private const val NEW_BOARD_RENDERER = true

@Composable
private fun BoardSection(
    game: GameUiState,
    spritePack: GameSpritePack?,
    onCellTap: (row: Int, col: Int) -> Unit,
    onCellDragSwap: (fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) -> Unit
) {
    if (NEW_BOARD_RENDERER) {
        BoardCanvas(
            board = game.board,
            selection = BoardSelectionState(game.selected, game.highlightedMatches, game.explodingMatches),
            spritePack = spritePack,
            onCellTap = onCellTap,
            onCellDragSwap = onCellDragSwap
        )
    } else {
        val spriteFrame = rememberSpriteFrame(
            frameCount = spritePack?.maxFrameCount ?: 1,
            frameDurationMs = 80
        )
        BoardView(
            board = game.board,
            selected = game.selected,
            highlightedMatches = game.highlightedMatches,
            explodingMatches = game.explodingMatches,
            spritePack = spritePack,
            spriteFrame = spriteFrame,
            onCellTap = onCellTap,
            onCellDragSwap = onCellDragSwap
        )
    }
}

@Composable
private fun BoardView(
    board: List<List<Int>>,
    selected: Position?,
    highlightedMatches: Set<Position>,
    explodingMatches: Set<Position>,
    spritePack: GameSpritePack?,
    spriteFrame: Int,
    onCellTap: (row: Int, col: Int) -> Unit,
    onCellDragSwap: (fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) -> Unit
) {
    val rows = board.size
    val cols = board.firstOrNull()?.size ?: 0
    if (rows == 0 || cols == 0) {
        return
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            val spacing = 6.dp
            val cellSize = (maxWidth - (spacing * (cols + 1))) / cols
            val density = LocalDensity.current
            val spacingPx = with(density) { spacing.toPx() }
            val cellSizePx = with(density) { cellSize.toPx() }
            val boardWidthPx = (cols * cellSizePx) + ((cols - 1) * spacingPx)
            val boardHeightPx = (rows * cellSizePx) + ((rows - 1) * spacingPx)

            var dragStartCell by remember(rows, cols) { mutableStateOf<Position?>(null) }
            var dragCurrentCell by remember(rows, cols) { mutableStateOf<Position?>(null) }

            Column(
                verticalArrangement = Arrangement.spacedBy(spacing),
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(rows, cols, cellSizePx, spacingPx) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                dragStartCell = resolveCellAtOffset(
                                    offset = startOffset,
                                    rows = rows,
                                    cols = cols,
                                    cellSizePx = cellSizePx,
                                    spacingPx = spacingPx,
                                    boardWidthPx = boardWidthPx,
                                    boardHeightPx = boardHeightPx
                                )
                                dragCurrentCell = dragStartCell
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val current = resolveCellAtOffset(
                                    offset = change.position,
                                    rows = rows,
                                    cols = cols,
                                    cellSizePx = cellSizePx,
                                    spacingPx = spacingPx,
                                    boardWidthPx = boardWidthPx,
                                    boardHeightPx = boardHeightPx
                                )
                                if (current != null) {
                                    dragCurrentCell = current
                                }
                            },
                            onDragEnd = {
                                val from = dragStartCell
                                val to = dragCurrentCell
                                if (from != null && to != null && from != to) {
                                    val adjacent = abs(from.row - to.row) + abs(from.col - to.col) == 1
                                    if (adjacent) {
                                        onCellDragSwap(from.row, from.col, to.row, to.col)
                                    }
                                }
                                dragStartCell = null
                                dragCurrentCell = null
                            },
                            onDragCancel = {
                                dragStartCell = null
                                dragCurrentCell = null
                            }
                        )
                    }
            ) {
                for (row in 0 until rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(spacing),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        for (col in 0 until cols) {
                            val value = board[row][col]
                            val position = Position(row, col)
                            PieceCell(
                                value = value,
                                selected = selected == position,
                                highlighted = position in highlightedMatches,
                                exploding = position in explodingMatches,
                                spritePack = spritePack,
                                spriteFrame = spriteFrame,
                                size = cellSize,
                                onClick = { onCellTap(row, col) }
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun resolveCellAtOffset(
    offset: androidx.compose.ui.geometry.Offset,
    rows: Int,
    cols: Int,
    cellSizePx: Float,
    spacingPx: Float,
    boardWidthPx: Float,
    boardHeightPx: Float
): Position? {
    if (offset.x < 0f || offset.y < 0f || offset.x > boardWidthPx || offset.y > boardHeightPx) {
        return null
    }

    val pitch = cellSizePx + spacingPx
    val col = (offset.x / pitch).toInt()
    val row = (offset.y / pitch).toInt()
    if (row !in 0 until rows || col !in 0 until cols) {
        return null
    }

    val localX = offset.x - (col * pitch)
    val localY = offset.y - (row * pitch)
    if (localX > cellSizePx || localY > cellSizePx) {
        return null
    }
    return Position(row, col)
}

@Composable
private fun rememberSpriteFrame(
    frameCount: Int,
    frameDurationMs: Int
): Int {
    var frame by remember(frameCount) { mutableStateOf(0) }
    LaunchedEffect(frameCount, frameDurationMs) {
        frame = 0
        if (frameCount <= 1) {
            return@LaunchedEffect
        }
        while (isActive) {
            delay(frameDurationMs.toLong())
            frame = (frame + 1) % frameCount
        }
    }
    return frame
}

@Composable
private fun PieceCell(
    value: Int,
    selected: Boolean,
    highlighted: Boolean,
    exploding: Boolean,
    spritePack: GameSpritePack?,
    spriteFrame: Int,
    size: Dp,
    onClick: () -> Unit
) {
    val hasPiece = value != Match3Engine.EMPTY
    val explosionFrame = spritePack?.explosionFrame(spriteFrame)
    val selectedAlpha by animateFloatAsState(
        targetValue = if (selected) 0.38f else 0f,
        animationSpec = tween(durationMillis = 120),
        label = "selected_alpha"
    )
    val highlightAlpha by animateFloatAsState(
        targetValue = if (highlighted) 0.55f else 0f,
        animationSpec = tween(durationMillis = 140),
        label = "highlight_alpha"
    )
    val explosionAlpha by animateFloatAsState(
        targetValue = if (exploding) 1f else 0f,
        animationSpec = tween(durationMillis = 90),
        label = "explosion_alpha"
    )
    val explosionScale by animateFloatAsState(
        targetValue = if (exploding) 1f else 0.45f,
        animationSpec = tween(durationMillis = 90),
        label = "explosion_scale"
    )

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(if (hasPiece) Latte.copy(alpha = 0.32f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Gold.copy(alpha = selectedAlpha))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Mint.copy(alpha = highlightAlpha))
        )
        AnimatedContent(
            targetState = value,
            transitionSpec = {
                ((
                    slideInVertically(
                        animationSpec = tween(durationMillis = 100),
                        initialOffsetY = { -it / 2 }
                    ) + fadeIn(animationSpec = tween(durationMillis = 100))
                    ) togetherWith (
                    slideOutVertically(
                        animationSpec = tween(durationMillis = 90),
                        targetOffsetY = { it / 2 }
                    ) + fadeOut(animationSpec = tween(durationMillis = 90))
                    ))
                    .using(SizeTransform(clip = false))
            },
            label = "piece_content"
        ) { targetValue ->
            if (targetValue != Match3Engine.EMPTY) {
                val animatedFrame = spritePack?.pieceFrame(targetValue, spriteFrame)
                if (animatedFrame != null) {
                    Image(
                        bitmap = animatedFrame,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(3.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        text = fallbackPieceSymbol(targetValue),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(1.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = explosionAlpha
                    scaleX = explosionScale
                    scaleY = explosionScale
                },
            contentAlignment = Alignment.Center
        ) {
            if (explosionFrame != null) {
                Image(
                    bitmap = explosionFrame,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(1.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "\uD83D\uDD25",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

internal fun fallbackPieceSymbol(value: Int): String {
    return when ((value % 6 + 6) % 6) {
        0 -> "\u2615"
        1 -> "\uD83E\uDED8"
        2 -> "\uD83E\uDD5B"
        3 -> "\uD83C\uDF6A"
        4 -> "\uD83D\uDFE4"
        else -> "\u2B50"
    }
}
