package com.semstress.mobile.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.semstress.mobile.domain.Position
import com.semstress.mobile.engine.Match3Engine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

/** Release past half a cell commits the swap; anything less springs back. */
private const val SWAP_COMMIT_FRACTION = 0.5f

/** How far a piece can be pulled toward a non-swappable direction (rubber-band feel). */
private const val RUBBER_BAND_FRACTION = 0.22f

private const val SWAP_PREVIEW_MS = 70
private const val SETTLE_BACK_MS = 140
private const val OUTCOME_TIMEOUT_MS = 600L

/** Callbacks emitted by [boardGestures] while the player drags a piece. */
interface BoardDragListener {
    fun onDragStart(cell: Position)
    fun onDrag(cell: Position, delta: Offset)
    fun onDragEnd(cell: Position, delta: Offset)
    fun onDragCancel()
}

/**
 * UX-06: Candy-Crush-style live swap preview. While dragging, the grabbed piece follows the finger
 * along the dominant axis (clamped to one cell) and the displaced neighbor counter-slides. On
 * release the pair either commits (past [SWAP_COMMIT_FRACTION] of a cell) or springs back. After
 * committing, the preview holds the swapped pose until the engine answers: a board change means
 * the swap was applied (the preview clears in place, seamlessly), while an invalid-move nonce bump
 * animates both pieces back to their cells — the classic "try and bounce back".
 *
 * [offset]/[origin] are only read inside the board's draw phase, so the preview animates without
 * recomposing the board (same approach as the sprite ticker, RR-20).
 */
internal class BoardDragController(private val scope: CoroutineScope) : BoardDragListener {

    /** Refreshed every composition; plain vars because only gesture/draw code reads them. */
    var board: List<List<Int>> = emptyList()
    var geometry: BoardGeometry? = null
    var onSwap: (Position, Position) -> Unit = { _, _ -> }

    var origin by mutableStateOf<Position?>(null)
        private set
    var awaitingOutcome by mutableStateOf(false)
        private set
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)

    /** The neighbor currently being displaced by the preview, or null when idle. */
    fun targetCell(): Position? = origin?.let { neighborToward(board, it, offset.value) }

    override fun onDragStart(cell: Position) {
        if (awaitingOutcome || pieceAt(board, cell) == Match3Engine.EMPTY) {
            return
        }
        origin = cell
        scope.launch { offset.snapTo(Offset.Zero) }
    }

    override fun onDrag(cell: Position, delta: Offset) {
        val geometry = geometry ?: return
        if (awaitingOutcome || origin != cell) {
            return
        }
        val clamped = clampDelta(board, cell, delta, geometry)
        scope.launch { offset.snapTo(clamped) }
    }

    override fun onDragEnd(cell: Position, delta: Offset) {
        val geometry = geometry ?: return
        if (awaitingOutcome || origin != cell) {
            return
        }
        val clamped = clampDelta(board, cell, delta, geometry)
        val target = neighborToward(board, cell, clamped)
        val committed = target != null &&
            pieceAt(board, target) != Match3Engine.EMPTY &&
            maxOf(abs(clamped.x), abs(clamped.y)) >= geometry.pitchPx * SWAP_COMMIT_FRACTION
        if (committed) {
            commitSwap(cell, checkNotNull(target), geometry)
        } else {
            settleBack()
        }
    }

    override fun onDragCancel() {
        settleBack()
    }

    /** The engine applied the move: the board now IS the previewed pose, clear without animating. */
    fun onBoardChanged() {
        if (!awaitingOutcome) return
        scope.launch {
            awaitingOutcome = false
            offset.snapTo(Offset.Zero)
            origin = null
        }
    }

    /** The engine rejected the move: bounce both pieces back to their cells. */
    fun onInvalidMove() {
        if (awaitingOutcome) {
            settleBack()
        }
    }

    private fun commitSwap(from: Position, to: Position, geometry: BoardGeometry) {
        scope.launch {
            val full = Offset(
                (to.col - from.col) * geometry.pitchPx,
                (to.row - from.row) * geometry.pitchPx
            )
            offset.animateTo(full, tween(SWAP_PREVIEW_MS))
            awaitingOutcome = true
            onSwap(from, to)
            // Fallback: if neither a board change nor an invalid-move signal arrives, settle back.
            delay(OUTCOME_TIMEOUT_MS)
            if (awaitingOutcome) {
                settleBackNow()
            }
        }
    }

    private fun settleBack() {
        scope.launch { settleBackNow() }
    }

    private suspend fun settleBackNow() {
        awaitingOutcome = false
        offset.animateTo(Offset.Zero, tween(SETTLE_BACK_MS, easing = FastOutSlowInEasing))
        origin = null
    }
}

/** Clamps the raw finger delta to the dominant axis, at most one cell toward a swappable neighbor. */
private fun clampDelta(board: List<List<Int>>, cell: Position, raw: Offset, geometry: BoardGeometry): Offset {
    val horizontal = abs(raw.x) >= abs(raw.y)
    val component = if (horizontal) raw.x else raw.y
    val neighbor = neighborToward(board, cell, if (horizontal) Offset(component, 0f) else Offset(0f, component))
    val swappable = neighbor != null && pieceAt(board, neighbor) != Match3Engine.EMPTY
    val maxMagnitude = geometry.pitchPx * (if (swappable) 1f else RUBBER_BAND_FRACTION)
    val clamped = min(abs(component), maxMagnitude) * sign(component)
    return if (horizontal) Offset(clamped, 0f) else Offset(0f, clamped)
}

private fun neighborToward(board: List<List<Int>>, cell: Position, delta: Offset): Position? {
    if (delta == Offset.Zero) return null
    val horizontal = abs(delta.x) >= abs(delta.y)
    val row = cell.row + if (horizontal) 0 else sign(delta.y).toInt()
    val col = cell.col + if (horizontal) sign(delta.x).toInt() else 0
    val inGrid = row in board.indices && col in (board.firstOrNull()?.indices ?: IntRange.EMPTY)
    return if (inGrid && (row != cell.row || col != cell.col)) Position(row, col) else null
}

private fun pieceAt(board: List<List<Int>>, cell: Position): Int =
    board.getOrNull(cell.row)?.getOrNull(cell.col) ?: Match3Engine.EMPTY

/**
 * Creates the drag controller and wires its outcome signals: a board change while a committed
 * preview is pending means the engine applied the swap; an invalid-move nonce bump means it was
 * rejected and the pieces bounce back.
 */
@Composable
internal fun rememberBoardDragController(
    board: List<List<Int>>,
    invalidMoveNonce: Int,
    onSwap: (Position, Position) -> Unit
): BoardDragController {
    val scope = rememberCoroutineScope()
    val controller = remember { BoardDragController(scope) }
    controller.board = board
    controller.onSwap = onSwap
    LaunchedEffect(board) { controller.onBoardChanged() }
    LaunchedEffect(invalidMoveNonce) { controller.onInvalidMove() }
    return controller
}
