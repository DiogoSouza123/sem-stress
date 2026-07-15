package com.semstress.mobile.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
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
import com.semstress.mobile.ui.sprites.SpriteSheet
import com.semstress.mobile.ui.theme.CoffeeSemanticColors
import com.semstress.mobile.ui.theme.CoffeeTheme

private const val SPRITE_FRAME_DURATION_MS = 80L
private const val CELL_CORNER_RADIUS_DP = 12
private const val OVERLAY_CORNER_RADIUS_DP = 10
private const val OVERLAY_INSET_DP = 2
private const val PIECE_INSET_DP = 3
private const val EXPLOSION_INSET_DP = 1
private const val SELECTED_ALPHA = 0.38f
private const val HIGHLIGHT_ALPHA = 0.55f
private const val BACKGROUND_ALPHA = 0.32f
private const val SHAKE_STEP_MS = 45
private const val SHAKE_AMPLITUDE_DP = 6f
private const val SHAKE_SECOND_STEP_FACTOR = 0.6f
private const val HINT_ALPHA = 0.3f

/** Selection/animation overlays for the board, kept apart from the piece values themselves. */
data class BoardSelectionState(
    val selected: Position?,
    val highlighted: Set<Position>,
    val exploding: Set<Position>,
    val shaking: Set<Position> = emptySet(),
    val invalidMoveNonce: Int = 0,
    val hinted: Set<Position> = emptySet(),
    val symbolModeEnabled: Boolean = false
)

private data class BoardDrawContext(
    val spriteAtlas: SpriteAtlas?,
    val frame: Int,
    val cellSizePx: Float,
    val pitchPx: Float,
    val textMeasurer: TextMeasurer,
    val pieceTextStyle: TextStyle,
    val explosionTextStyle: TextStyle,
    val colors: CoffeeSemanticColors,
    val shakeOffsetPx: Float,
    val symbolModeEnabled: Boolean
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
    val colors = CoffeeTheme.colors
    val reducedMotion = rememberReducedMotionEnabled()
    val shakeOffset = rememberShakeOffsetPx(selection.invalidMoveNonce, reducedMotion)

    val dragController = rememberBoardDragController(board, selection.invalidMoveNonce) { from, to ->
        onCellDragSwap(from.row, from.col, to.row, to.col)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            val geometry = rememberBoardGeometry(rows, cols)
            val boardHeight = with(LocalDensity.current) { geometry.boardHeightPx.toDp() }
            dragController.geometry = geometry

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boardHeight)
                    .boardGestures(geometry, onCellTap, dragController)
            ) {
                val context = BoardDrawContext(
                    spriteAtlas = spriteAtlas,
                    frame = currentSpriteFrame(spriteAtlas, frameTime.value),
                    cellSizePx = geometry.cellSizePx,
                    pitchPx = geometry.pitchPx,
                    textMeasurer = textMeasurer,
                    pieceTextStyle = pieceTextStyle,
                    explosionTextStyle = explosionTextStyle,
                    colors = colors,
                    shakeOffsetPx = shakeOffset.value,
                    symbolModeEnabled = selection.symbolModeEnabled
                )
                val dragOrigin = dragController.origin
                val dragTarget = dragController.targetCell()
                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val position = Position(row, col)
                        val partOfDrag = dragOrigin != null && (position == dragOrigin || position == dragTarget)
                        drawCell(position, board[row][col], selection, context, skipPiece = partOfDrag)
                    }
                }
                drawDragPreview(board, dragOrigin, dragTarget, dragController.offset.value, context)
            }
        }
    }
}

/**
 * Draws the two pieces involved in a live drag on top of everything else: the displaced neighbor
 * counter-slides by the inverse offset and the grabbed piece rides the finger, drawn last so it
 * stays above its neighbor — the Candy Crush layering.
 */
private fun DrawScope.drawDragPreview(
    board: List<List<Int>>,
    origin: Position?,
    target: Position?,
    dragOffset: Offset,
    context: BoardDrawContext
) {
    if (origin == null) {
        return
    }
    if (target != null) {
        val targetValue = board[target.row][target.col]
        if (targetValue != Match3Engine.EMPTY) {
            val topLeft = Offset(target.col * context.pitchPx, target.row * context.pitchPx) - dragOffset
            drawPiece(targetValue, topLeft, context)
        }
    }
    val originValue = board[origin.row][origin.col]
    if (originValue != Match3Engine.EMPTY) {
        val topLeft = Offset(origin.col * context.pitchPx, origin.row * context.pitchPx) + dragOffset
        drawPiece(originValue, topLeft, context)
    }
}

private fun currentSpriteFrame(spriteAtlas: SpriteAtlas?, frameTimeMs: Long): Int {
    val frameCount = spriteAtlas?.maxFrameCount ?: 1
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

/**
 * UX-05: a short, decaying horizontal shake (in px) that replays every time [nonce] changes,
 * used to give invalid-move feedback without text (per ui-ux.md's "sem texto" requirement).
 */
@Composable
private fun rememberShakeOffsetPx(nonce: Int, reducedMotion: Boolean): Animatable<Float, AnimationVector1D> {
    val offset = remember { Animatable(0f) }
    val amplitudePx = with(LocalDensity.current) { SHAKE_AMPLITUDE_DP.dp.toPx() }
    LaunchedEffect(nonce, reducedMotion) {
        if (nonce == 0 || reducedMotion) {
            return@LaunchedEffect
        }
        offset.snapTo(0f)
        val steps = listOf(amplitudePx, -amplitudePx, amplitudePx * SHAKE_SECOND_STEP_FACTOR, 0f)
        steps.forEach { target ->
            offset.animateTo(target, animationSpec = tween(SHAKE_STEP_MS))
        }
    }
    return offset
}

private fun DrawScope.drawCell(
    position: Position,
    value: Int,
    selection: BoardSelectionState,
    context: BoardDrawContext,
    skipPiece: Boolean = false
) {
    val hasPiece = value != Match3Engine.EMPTY
    val topLeft = Offset(position.col * context.pitchPx, position.row * context.pitchPx)

    if (hasPiece) {
        drawRoundRect(
            color = context.colors.surfaceBoard.copy(alpha = BACKGROUND_ALPHA),
            topLeft = topLeft,
            size = Size(context.cellSizePx, context.cellSizePx),
            cornerRadius = CornerRadius(CELL_CORNER_RADIUS_DP.dp.toPx())
        )
    }

    drawSelectionOverlay(position, selection, topLeft, context)

    // Pieces riding the drag preview are drawn later, on top of the whole grid.
    if (hasPiece && !skipPiece) {
        val pieceTopLeft = if (position in selection.shaking) {
            topLeft + Offset(context.shakeOffsetPx, 0f)
        } else {
            topLeft
        }
        drawPiece(value, pieceTopLeft, context)
    }
    if (position in selection.exploding) {
        val inset = EXPLOSION_INSET_DP.dp.toPx()
        val sheet = context.spriteAtlas?.explosionSheet()
        if (sheet != null) {
            drawSpriteFrame(sheet, topLeft, inset, context)
        } else {
            drawCenteredText(context.textMeasurer, "🔥", context.explosionTextStyle, topLeft, context.cellSizePx)
        }
    }
}

private fun DrawScope.drawSelectionOverlay(
    position: Position,
    selection: BoardSelectionState,
    topLeft: Offset,
    context: BoardDrawContext
) {
    val cellSizePx = context.cellSizePx
    val inset = OVERLAY_INSET_DP.dp.toPx()
    val overlayTopLeft = topLeft + Offset(inset, inset)
    val overlaySize = Size(cellSizePx - inset * 2, cellSizePx - inset * 2)
    val overlayCornerRadius = CornerRadius(OVERLAY_CORNER_RADIUS_DP.dp.toPx())

    if (position == selection.selected) {
        drawRoundRect(
            color = context.colors.pieceHighlight.copy(alpha = SELECTED_ALPHA),
            topLeft = overlayTopLeft,
            size = overlaySize,
            cornerRadius = overlayCornerRadius
        )
    }
    if (position in selection.highlighted) {
        drawRoundRect(
            color = context.colors.pieceExplosion.copy(alpha = HIGHLIGHT_ALPHA),
            topLeft = overlayTopLeft,
            size = overlaySize,
            cornerRadius = overlayCornerRadius
        )
    }
    if (position in selection.hinted) {
        drawRoundRect(
            color = context.colors.warning.copy(alpha = HINT_ALPHA),
            topLeft = overlayTopLeft,
            size = overlaySize,
            cornerRadius = overlayCornerRadius
        )
    }
}

/** UX-11: symbol mode always shows the distinct per-type glyph, for players who don't rely on color/sprite art. */
private fun DrawScope.drawPiece(value: Int, topLeft: Offset, context: BoardDrawContext) {
    val inset = PIECE_INSET_DP.dp.toPx()
    val sheet = context.spriteAtlas?.pieceSheet(value)
    if (sheet != null && !context.symbolModeEnabled) {
        drawSpriteFrame(sheet, topLeft, inset, context)
    } else {
        val symbol = fallbackPieceSymbol(value)
        drawCenteredText(context.textMeasurer, symbol, context.pieceTextStyle, topLeft, context.cellSizePx)
    }
}

private fun DrawScope.drawSpriteFrame(sheet: SpriteSheet, topLeft: Offset, inset: Float, context: BoardDrawContext) {
    val size = (context.cellSizePx - inset * 2).toInt().coerceAtLeast(1)
    drawImage(
        image = sheet.bitmap,
        srcOffset = sheet.srcOffsetFor(context.frame),
        srcSize = sheet.frameSize,
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
