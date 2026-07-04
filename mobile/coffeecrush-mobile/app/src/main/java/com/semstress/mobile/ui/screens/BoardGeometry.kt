package com.semstress.mobile.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.semstress.mobile.domain.Position
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Pixel geometry of the board grid, used to translate pointer offsets into [Position]s. Kept
 * separate from [com.semstress.mobile.ui.state.GameUiState] because it is purely a layout concern
 * of [BoardCanvas].
 */
data class BoardGeometry(
    val rows: Int,
    val cols: Int,
    val cellSizePx: Float,
    val spacingPx: Float,
    val boardWidthPx: Float,
    val boardHeightPx: Float
) {
    val pitchPx: Float get() = cellSizePx + spacingPx
}

private fun BoardGeometry.cellAt(offset: Offset): Position? {
    val outsideBoard = offset.x !in 0f..boardWidthPx || offset.y !in 0f..boardHeightPx
    val col = (offset.x / pitchPx).toInt()
    val row = (offset.y / pitchPx).toInt()
    val outsideGrid = row !in 0 until rows || col !in 0 until cols
    if (outsideBoard || outsideGrid) return null
    val local = Offset(offset.x - col * pitchPx, offset.y - row * pitchPx)
    return Position(row, col).takeIf { local.x <= cellSizePx && local.y <= cellSizePx }
}

private fun exceedsTouchSlop(current: Offset, origin: Offset, slop: Float): Boolean {
    val delta = current - origin
    return hypot(delta.x, delta.y) > slop
}

private fun finishGesture(
    startCell: Position?,
    currentCell: Position?,
    dragging: Boolean,
    onCellTap: (row: Int, col: Int) -> Unit,
    onCellDragSwap: (fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) -> Unit
) {
    if (startCell == null) return
    when {
        !dragging -> onCellTap(startCell.row, startCell.col)
        dragging && currentCell != null -> {
            val target = currentCell
            val adjacent = abs(startCell.row - target.row) + abs(startCell.col - target.col) == 1
            if (adjacent && target != startCell) {
                onCellDragSwap(startCell.row, startCell.col, target.row, target.col)
            }
        }
    }
}

/**
 * Resolves tap-to-select and drag-to-swap on a single gesture detector, replacing the previous
 * pairing of per-cell `clickable` (tap) with a board-level `detectDragGestures` (drag): with the
 * whole board now a single [BoardCanvas] draw surface (RR-20), there is no per-cell Composable left
 * to own the tap. A gesture is a tap when the pointer is released before crossing touch slop, and a
 * swap when it is released over an orthogonally adjacent cell.
 */
fun Modifier.boardGestures(
    geometry: BoardGeometry,
    onCellTap: (row: Int, col: Int) -> Unit,
    onCellDragSwap: (fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) -> Unit
): Modifier = pointerInput(geometry) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val startCell = geometry.cellAt(down.position)
        var currentCell = startCell
        var dragging = false
        var released = false
        while (!released) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
            when {
                change == null -> released = true
                !change.pressed -> {
                    finishGesture(startCell, currentCell, dragging, onCellTap, onCellDragSwap)
                    released = true
                }
                else -> {
                    if (!dragging && exceedsTouchSlop(change.position, down.position, viewConfiguration.touchSlop)) {
                        dragging = true
                    }
                    if (dragging) {
                        change.consume()
                        currentCell = geometry.cellAt(change.position) ?: currentCell
                    }
                }
            }
        }
    }
}
