package com.semstress.mobile.engine

import com.semstress.mobile.domain.Position
import com.semstress.mobile.domain.StageConfig
import kotlin.math.abs

class Match3Engine(
    private val config: StageConfig
) {
    companion object {
        const val EMPTY = -1
        private const val MAX_SHUFFLE_ATTEMPTS = 300
    }

    fun tryMove(
        board: Match3Board,
        first: Position,
        second: Position
    ): MoveOutcome {
        val animated = tryMoveAnimated(board, first, second)
        return MoveOutcome(
            valid = animated.valid,
            points = animated.points,
            cascades = animated.cascades
        )
    }

    fun tryMoveAnimated(
        board: Match3Board,
        first: Position,
        second: Position
    ): AnimatedMoveOutcome {
        if (!board.isValid(first) || !board.isValid(second)) {
            return AnimatedMoveOutcome(valid = false, points = 0, cascades = 0, rounds = emptyList())
        }

        if (config.onlyAdjacentSwap && !isAdjacent(first, second)) {
            return AnimatedMoveOutcome(valid = false, points = 0, cascades = 0, rounds = emptyList())
        }

        board.swap(first, second)
        val matched = findMatches(board)
        if (matched.positions.isEmpty()) {
            board.swap(first, second)
            return AnimatedMoveOutcome(valid = false, points = 0, cascades = 0, rounds = emptyList())
        }

        val resolved = resolveBoardAnimated(board)
        return AnimatedMoveOutcome(
            valid = true,
            points = resolved.points,
            cascades = resolved.cascades,
            rounds = resolved.rounds
        )
    }

    fun resolveBoard(board: Match3Board): ResolveOutcome {
        val animated = resolveBoardAnimated(board, captureRounds = false)
        return ResolveOutcome(points = animated.points, cascades = animated.cascades)
    }

    fun resolveBoardAnimated(board: Match3Board): AnimatedResolveOutcome {
        return resolveBoardAnimated(board, captureRounds = true)
    }

    private fun resolveBoardAnimated(
        board: Match3Board,
        captureRounds: Boolean
    ): AnimatedResolveOutcome {
        var totalPoints = 0
        var cascadeLevel = 0
        val rounds = mutableListOf<AnimationRound>()

        while (true) {
            val matches = findMatches(board)
            if (matches.positions.isEmpty()) {
                return AnimatedResolveOutcome(
                    points = totalPoints,
                    cascades = cascadeLevel,
                    rounds = rounds
                )
            }

            cascadeLevel++
            val roundPoints = if (cascadeLevel == 1 || config.scoreCascade) {
                calculatePoints(matches.runLengths, cascadeLevel)
            } else {
                0
            }
            totalPoints += roundPoints

            val stateBeforeClear = if (captureRounds) board.snapshot() else emptyList()
            val matchedPositions = if (captureRounds) {
                matches.positions.sortedWith(compareBy<Position> { it.row }.thenBy { it.col })
            } else {
                emptyList()
            }

            clearMatched(board, matches.positions)
            val stateAfterClear = if (captureRounds) board.snapshot() else emptyList()
            val fallFrames = if (captureRounds) {
                collapseAndRefillCapturingFrames(board)
            } else {
                collapseAndRefill(board)
                emptyList()
            }

            if (captureRounds) {
                rounds += AnimationRound(
                    stateBeforeClear = stateBeforeClear,
                    matchedPositions = matchedPositions,
                    stateAfterClear = stateAfterClear,
                    fallFrames = fallFrames,
                    roundPoints = roundPoints
                )
            }
        }
    }

    fun ensurePlayableBoard(board: Match3Board) {
        if (findMatches(board).positions.isNotEmpty() || !hasAvailableMove(board)) {
            shuffleWithoutMatches(board)
        }
    }

    fun hasAvailableMove(board: Match3Board): Boolean {
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                if (col + 1 < board.cols && wouldCreateMatch(board, row, col, row, col + 1)) {
                    return true
                }
                if (row + 1 < board.rows && wouldCreateMatch(board, row, col, row + 1, col)) {
                    return true
                }
            }
        }
        return false
    }

    fun shuffleWithoutMatches(board: Match3Board) {
        repeat(MAX_SHUFFLE_ATTEMPTS) {
            board.fillRandom()
            if (findMatches(board).positions.isEmpty() && hasAvailableMove(board)) {
                return
            }
        }
        applyFallback(board)
    }

    private fun applyFallback(board: Match3Board) {
        val types = maxOf(1, board.pieceTypes)
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                board.set(row, col, ((row * 2) + col) % types)
            }
        }
        if (board.rows >= 2 && board.cols >= 3) {
            val a = 0
            val b = if (types > 1) 1 else 0
            board.set(0, 0, a)
            board.set(0, 1, b)
            board.set(0, 2, a)
            board.set(1, 1, a)
        }
        if (findMatches(board).positions.isNotEmpty() || !hasAvailableMove(board)) {
            shuffleWithoutMatches(board)
        }
    }

    private fun calculatePoints(runLengths: List<Int>, cascadeLevel: Int): Int {
        var points = 0
        runLengths.forEach { length ->
            points += when {
                length >= 5 -> config.scoreMatch5Plus
                length >= 4 -> config.scoreMatch4
                else -> config.scoreMatch3
            }
        }
        val baseMultiplier = maxOf(1, config.cascadeMultiplier)
        val cascadeMultiplier = 1 + ((cascadeLevel - 1) * (baseMultiplier - 1))
        return points * cascadeMultiplier
    }

    private fun clearMatched(board: Match3Board, matched: Set<Position>) {
        matched.forEach { position ->
            board.set(position.row, position.col, EMPTY)
        }
    }

    private fun collapseAndRefill(board: Match3Board) {
        while (dropStep(board)) {
            // Repeat until stable.
        }
        fillEmptyWithNewPieces(board)
    }

    private fun collapseAndRefillCapturingFrames(board: Match3Board): List<List<List<Int>>> {
        val frames = mutableListOf<List<List<Int>>>()
        while (dropStep(board)) {
            frames += board.snapshot()
        }
        fillEmptyWithNewPieces(
            board = board,
            onPieceAdded = {
                frames += board.snapshot()
            }
        )
        return frames
    }

    private fun dropStep(board: Match3Board): Boolean {
        var moved = false
        for (row in board.rows - 2 downTo 0) {
            for (col in 0 until board.cols) {
                val value = board.get(row, col)
                if (value == EMPTY) {
                    continue
                }
                if (board.get(row + 1, col) == EMPTY) {
                    board.set(row + 1, col, value)
                    board.set(row, col, EMPTY)
                    moved = true
                }
            }
        }
        return moved
    }

    private fun fillEmptyWithNewPieces(
        board: Match3Board,
        onPieceAdded: (() -> Unit)? = null
    ) {
        for (col in 0 until board.cols) {
            for (row in board.rows - 1 downTo 0) {
                if (board.get(row, col) == EMPTY) {
                    board.set(row, col, board.nextPiece())
                    onPieceAdded?.invoke()
                }
            }
        }
    }

    private fun findMatches(board: Match3Board): MatchGroup {
        val matched = linkedSetOf<Position>()
        val runLengths = mutableListOf<Int>()

        for (row in 0 until board.rows) {
            var col = 0
            while (col < board.cols) {
                val start = col
                val value = board.get(row, col)
                while (col + 1 < board.cols && board.get(row, col + 1) == value) {
                    col++
                }
                val length = col - start + 1
                if (value != EMPTY && length >= config.minMatchSize) {
                    for (c in start..col) {
                        matched += Position(row, c)
                    }
                    runLengths += length
                }
                col++
            }
        }

        for (col in 0 until board.cols) {
            var row = 0
            while (row < board.rows) {
                val start = row
                val value = board.get(row, col)
                while (row + 1 < board.rows && board.get(row + 1, col) == value) {
                    row++
                }
                val length = row - start + 1
                if (value != EMPTY && length >= config.minMatchSize) {
                    for (r in start..row) {
                        matched += Position(r, col)
                    }
                    runLengths += length
                }
                row++
            }
        }

        return MatchGroup(positions = matched, runLengths = runLengths)
    }

    private fun wouldCreateMatch(
        board: Match3Board,
        rowA: Int,
        colA: Int,
        rowB: Int,
        colB: Int
    ): Boolean {
        val first = Position(rowA, colA)
        val second = Position(rowB, colB)
        board.swap(first, second)
        val creates = createsMatchAt(board, rowA, colA) || createsMatchAt(board, rowB, colB)
        board.swap(first, second)
        return creates
    }

    private fun createsMatchAt(board: Match3Board, row: Int, col: Int): Boolean {
        val value = board.get(row, col)
        if (value == EMPTY) {
            return false
        }

        val horizontal = 1 +
            countDirection(board, row, col, 0, -1, value) +
            countDirection(board, row, col, 0, 1, value)
        if (horizontal >= config.minMatchSize) {
            return true
        }

        val vertical = 1 +
            countDirection(board, row, col, -1, 0, value) +
            countDirection(board, row, col, 1, 0, value)
        return vertical >= config.minMatchSize
    }

    private fun countDirection(
        board: Match3Board,
        row: Int,
        col: Int,
        deltaRow: Int,
        deltaCol: Int,
        value: Int
    ): Int {
        var r = row + deltaRow
        var c = col + deltaCol
        var count = 0

        while (r in 0 until board.rows && c in 0 until board.cols && board.get(r, c) == value) {
            count++
            r += deltaRow
            c += deltaCol
        }

        return count
    }

    private fun isAdjacent(first: Position, second: Position): Boolean {
        val rowDiff = abs(first.row - second.row)
        val colDiff = abs(first.col - second.col)
        return (rowDiff + colDiff) == 1
    }
}
