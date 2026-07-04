package com.semstress.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.semstress.mobile.domain.Position
import com.semstress.mobile.engine.Match3Engine
import com.semstress.mobile.ui.sprites.SpriteAtlas
import com.semstress.mobile.ui.theme.Gold
import com.semstress.mobile.ui.theme.Latte
import com.semstress.mobile.ui.theme.Mint

private const val SPRITE_FRAME_DURATION_MS = 80L
private const val BOARD_SPACING_DP = 6
private const val CELL_CORNER_RADIUS_DP = 12
private const val OVERLAY_CORNER_RADIUS_DP = 10
private const val OVERLAY_INSET_DP = 2
private const val PIECE_INSET_DP = 3
private const val EXPLOSION_INSET_DP = 1
private const val SELECTED_ALPHA = 0.38f
private const val HIGHLIGHT_ALPHA = 0.55f
private const val BACKGROUND_ALPHA = 0.32f

/** Selection/animation overlays for the board, kept apart from the piece values themselves. */
data class BoardSelectionState(
    val selected: Position?,
    val highlighted: Set<Position>,
    val exploding: Set<Position>
)

private data class BoardDrawContext(
    val spriteAtlas: SpriteAtlas?,
    val frame: Int,
    val cellSizePx: Float,
    val pitchPx: Float,
    val textMeasurer: TextMeasurer,
    val pieceTextStyle: TextStyle,
    val explosionTextStyle: TextStyle
)

/**
 * Board rendered as a single [Canvas] (RR-20). Cells used to be one Composable each
 * (`BoardView`/`PieceCell`), re-laid-out and re-drawn ~12.5x/second forever because of a global
 * sprite-frame ticker read by every cell. Here the ticker ([rememberFrameTicker]) is only read
 * inside the draw phase, so it invalidates drawing but never triggers recomposition; the board only
 * recomposes when the actual game state (board/selection) changes. Tap and drag are resolved by
 * [boardGestures] directly on the canvas, reusing the same cell-hit-testing idea `resolveCellAtOffset`
 * already used, since there is no longer a per-cell node to attach a `clickable` to.
 */
@Composable
fun BoardCanvas(
    board: List<List<Int>>,
    selection: BoardSelectionState,
    spriteAtlas: SpriteAtlas?,
    onCellTap: (row: Int, col: Int) -> Unit,
    onCellDragSwap: (fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) -> Unit
) {
    val rows = board.size
    val cols = board.firstOrNull()?.size ?: 0
    if (rows == 0 || cols == 0) {
        return
    }

    val frameTime = rememberFrameTicker()
    val textMeasurer = rememberTextMeasurer()
    val pieceTextStyle = MaterialTheme.typography.headlineSmall
    val explosionTextStyle = MaterialTheme.typography.headlineMedium

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            val spacing = BOARD_SPACING_DP.dp
            val cellSize = (maxWidth - (spacing * (cols + 1))) / cols
            val density = LocalDensity.current
            val spacingPx = with(density) { spacing.toPx() }
            val cellSizePx = with(density) { cellSize.toPx() }
            val geometry = BoardGeometry(
                rows = rows,
                cols = cols,
                cellSizePx = cellSizePx,
                spacingPx = spacingPx,
                boardWidthPx = (cols * cellSizePx) + ((cols - 1) * spacingPx),
                boardHeightPx = (rows * cellSizePx) + ((rows - 1) * spacingPx)
            )
            val boardHeight = with(density) { geometry.boardHeightPx.toDp() }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boardHeight)
                    .boardGestures(geometry, onCellTap, onCellDragSwap)
            ) {
                val context = BoardDrawContext(
                    spriteAtlas = spriteAtlas,
                    frame = currentSpriteFrame(spriteAtlas, frameTime.value),
                    cellSizePx = cellSizePx,
                    pitchPx = geometry.pitchPx,
                    textMeasurer = textMeasurer,
                    pieceTextStyle = pieceTextStyle,
                    explosionTextStyle = explosionTextStyle
                )
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        drawCell(Position(row, col), board[row][col], selection, context)
                    }
                }
            }
        }
    }
}

private fun currentSpriteFrame(spriteAtlas: SpriteAtlas?, frameTimeMs: Long): Int {
    val frameCount = spriteAtlas?.frameCount ?: 1
    if (frameCount <= 1) {
        return 0
    }
    return ((frameTimeMs / SPRITE_FRAME_DURATION_MS) % frameCount).toInt()
}

@Composable
private fun rememberFrameTicker(): State<Long> {
    val time = remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withFrameMillis { time.longValue = it }
        }
    }
    return time
}

private fun DrawScope.drawCell(
    position: Position,
    value: Int,
    selection: BoardSelectionState,
    context: BoardDrawContext
) {
    val hasPiece = value != Match3Engine.EMPTY
    val topLeft = Offset(position.col * context.pitchPx, position.row * context.pitchPx)

    if (hasPiece) {
        drawRoundRect(
            color = Latte.copy(alpha = BACKGROUND_ALPHA),
            topLeft = topLeft,
            size = Size(context.cellSizePx, context.cellSizePx),
            cornerRadius = CornerRadius(CELL_CORNER_RADIUS_DP.dp.toPx())
        )
    }

    drawSelectionOverlay(position, selection, topLeft, context.cellSizePx)

    if (hasPiece) {
        drawPiece(value, topLeft, context)
    }
    if (position in selection.exploding) {
        drawExplosion(topLeft, context)
    }
}

private fun DrawScope.drawSelectionOverlay(
    position: Position,
    selection: BoardSelectionState,
    topLeft: Offset,
    cellSizePx: Float
) {
    val inset = OVERLAY_INSET_DP.dp.toPx()
    val overlayTopLeft = topLeft + Offset(inset, inset)
    val overlaySize = Size(cellSizePx - inset * 2, cellSizePx - inset * 2)
    val overlayCornerRadius = CornerRadius(OVERLAY_CORNER_RADIUS_DP.dp.toPx())

    if (position == selection.selected) {
        drawRoundRect(
            color = Gold.copy(alpha = SELECTED_ALPHA),
            topLeft = overlayTopLeft,
            size = overlaySize,
            cornerRadius = overlayCornerRadius
        )
    }
    if (position in selection.highlighted) {
        drawRoundRect(
            color = Mint.copy(alpha = HIGHLIGHT_ALPHA),
            topLeft = overlayTopLeft,
            size = overlaySize,
            cornerRadius = overlayCornerRadius
        )
    }
}

private fun DrawScope.drawPiece(value: Int, topLeft: Offset, context: BoardDrawContext) {
    val inset = PIECE_INSET_DP.dp.toPx()
    val sheet = context.spriteAtlas?.pieceSheet(value)
    if (sheet != null) {
        drawSpriteFrame(sheet, topLeft, inset, context)
    } else {
        val symbol = fallbackPieceSymbol(value)
        drawCenteredText(context.textMeasurer, symbol, context.pieceTextStyle, topLeft, context.cellSizePx)
    }
}

private fun DrawScope.drawExplosion(topLeft: Offset, context: BoardDrawContext) {
    val inset = EXPLOSION_INSET_DP.dp.toPx()
    val sheet = context.spriteAtlas?.explosionSheet()
    if (sheet != null) {
        drawSpriteFrame(sheet, topLeft, inset, context)
    } else {
        drawCenteredText(context.textMeasurer, "🔥", context.explosionTextStyle, topLeft, context.cellSizePx)
    }
}

private fun DrawScope.drawSpriteFrame(sheet: ImageBitmap, topLeft: Offset, inset: Float, context: BoardDrawContext) {
    val atlas = requireNotNull(context.spriteAtlas)
    val size = (context.cellSizePx - inset * 2).toInt().coerceAtLeast(1)
    drawImage(
        image = sheet,
        srcOffset = atlas.srcOffsetFor(context.frame),
        srcSize = atlas.frameSize,
        dstOffset = IntOffset((topLeft.x + inset).toInt(), (topLeft.y + inset).toInt()),
        dstSize = IntSize(size, size)
    )
}

private fun DrawScope.drawCenteredText(
    textMeasurer: TextMeasurer,
    text: String,
    style: TextStyle,
    topLeft: Offset,
    cellSizePx: Float
) {
    val layout = textMeasurer.measure(text, style)
    val offset = Offset(
        x = topLeft.x + (cellSizePx - layout.size.width) / 2f,
        y = topLeft.y + (cellSizePx - layout.size.height) / 2f
    )
    drawText(layout, topLeft = offset)
}
