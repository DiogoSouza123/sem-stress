package com.semstress.mobile.engine

import com.semstress.mobile.domain.Position
import com.semstress.mobile.domain.StageConfig
import kotlin.math.abs

class Match3Engine(
    private val config: StageConfig
) {
    companion object {
        const val EMPTY = -1

        /** GP-01: "Moedor" special piece, created by a match-4; mills its 8 neighbors on activation. */
        const val SPECIAL_GRINDER = -2

        private const val MAX_SHUFFLE_ATTEMPTS = 300
        private const val GRINDER_MATCH_LENGTH = 4
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
            val runs = findMatchRuns(board)
            if (runs.isEmpty()) {
                return AnimatedResolveOutcome(
                    points = totalPoints,
                    cascades = cascadeLevel,
                    rounds = rounds
                )
            }

            cascadeLevel++
            val roundPoints = if (cascadeLevel == 1 || config.scoreCascade) {
                calculatePoints(runs.map { it.length }, cascadeLevel)
            } else {
                0
            }
            totalPoints += roundPoints

            val allPositions = runs.flatMapTo(linkedSetOf()) { it.positions }
            val specialSpawns = selectSpecialSpawns(runs)
            val matchedPositions = if (captureRounds) {
                allPositions.sortedWith(compareBy<Position> { it.row }.thenBy { it.col })
            } else {
                emptyList()
            }

            clearMatched(board, allPositions)
            specialSpawns.forEach { position -> board.set(position.row, position.col, SPECIAL_GRINDER) }
            val fallSteps = if (captureRounds) {
                collapseAndRefillCapturingEvents(board)
            } else {
                collapseAndRefill(board)
                emptyList()
            }

            if (captureRounds) {
                rounds += AnimationRound(
                    matchedPositions = matchedPositions,
                    fallSteps = fallSteps,
                    roundPoints = roundPoints,
                    specialSpawns = specialSpawns
                )
            }
        }
    }

    /** GP-01: taps a [SPECIAL_GRINDER] piece, milling its 8 neighbors (Moore neighborhood) for bonus points. */
    fun activateSpecialPiece(board: Match3Board, position: Position): SpecialActivationOutcome {
        if (!board.isValid(position) || board.get(position.row, position.col) != SPECIAL_GRINDER) {
            return SpecialActivationOutcome(
                activated = false,
                points = 0,
                milledPieces = emptyList(),
                fallSteps = emptyList()
            )
        }

        val milled = moorePositions(board, position).mapNotNull { neighbor ->
            val value = board.get(neighbor.row, neighbor.col)
            if (value >= 0) neighbor to value else null
        }

        board.set(position.row, position.col, EMPTY)
        milled.forEach { (neighbor, _) -> board.set(neighbor.row, neighbor.col, EMPTY) }

        val fallSteps = collapseAndRefillCapturingEvents(board)
        return SpecialActivationOutcome(
            activated = true,
            points = milled.size * config.scoreMatch3,
            milledPieces = milled,
            fallSteps = fallSteps
        )
    }

    private fun moorePositions(board: Match3Board, center: Position): List<Position> {
        val deltas = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
        return deltas.mapNotNull { (deltaRow, deltaCol) ->
            val row = center.row + deltaRow
            val col = center.col + deltaCol
            if (row in 0 until board.rows && col in 0 until board.cols) Position(row, col) else null
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

    /** GP-08: the first swap that would create a match, used to highlight a hint after inactivity. */
    fun findAvailableMove(board: Match3Board): Pair<Position, Position>? {
        val positions = (0 until board.rows).flatMap { row -> (0 until board.cols).map { col -> Position(row, col) } }
        return positions.firstNotNullOfOrNull { position -> candidateMoveAt(board, position) }
    }

    private fun candidateMoveAt(board: Match3Board, position: Position): Pair<Position, Position>? {
        val right = Position(position.row, position.col + 1)
        val down = Position(position.row + 1, position.col)
        val canSwapRight = position.col + 1 < board.cols &&
            wouldCreateMatch(board, position.row, position.col, right.row, right.col)
        val canSwapDown = position.row + 1 < board.rows &&
            wouldCreateMatch(board, position.row, position.col, down.row, down.col)
        return when {
            canSwapRight -> position to right
            canSwapDown -> position to down
            else -> null
        }
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
        while (dropStep(board).isNotEmpty()) {
            // Repeat until stable.
        }
        fillEmptyWithNewPieces(board)
    }

    private fun collapseAndRefillCapturingEvents(board: Match3Board): List<List<BoardEvent>> {
        val steps = mutableListOf<List<BoardEvent>>()
        while (true) {
            val moves = dropStep(board)
            if (moves.isEmpty()) {
                break
            }
            steps += moves.map { (from, to) -> BoardEvent.Moved(from, to, board.get(to.row, to.col)) }
        }
        fillEmptyWithNewPieces(
            board = board,
            onPieceAdded = { position, piece ->
                steps += listOf(BoardEvent.Spawned(position, piece))
            }
        )
        return steps
    }

    private fun dropStep(board: Match3Board): List<Pair<Position, Position>> {
        val moves = mutableListOf<Pair<Position, Position>>()
        for (row in board.rows - 2 downTo 0) {
            for (col in 0 until board.cols) {
                val value = board.get(row, col)
                if (value == EMPTY) {
                    continue
                }
                if (board.get(row + 1, col) == EMPTY) {
                    board.set(row + 1, col, value)
                    board.set(row, col, EMPTY)
                    moves += Position(row, col) to Position(row + 1, col)
                }
            }
        }
        return moves
    }

    private fun fillEmptyWithNewPieces(
        board: Match3Board,
        onPieceAdded: ((Position, Int) -> Unit)? = null
    ) {
        for (col in 0 until board.cols) {
            for (row in board.rows - 1 downTo 0) {
                if (board.get(row, col) == EMPTY) {
                    val piece = board.nextPiece()
                    board.set(row, col, piece)
                    onPieceAdded?.invoke(Position(row, col), piece)
                }
            }
        }
    }

    private fun findMatches(board: Match3Board): MatchGroup {
        val runs = findMatchRuns(board)
        val matched = linkedSetOf<Position>()
        val runLengths = mutableListOf<Int>()
        runs.forEach { run ->
            matched += run.positions
            runLengths += run.length
        }
        return MatchGroup(positions = matched, runLengths = runLengths)
    }

    /**
     * GP-01: unlike [findMatches] (which flattens everything into one position set for scoring),
     * this keeps each run's cells in order so special-piece creation can pick a specific cell
     * (e.g. the last cell of a match-4) instead of clearing the whole run uniformly. A cell's value
     * being negative (EMPTY or a special piece marker) always excludes it from matching.
     */
    private fun findMatchRuns(board: Match3Board): List<MatchRun> {
        val runs = mutableListOf<MatchRun>()

        for (row in 0 until board.rows) {
            var col = 0
            while (col < board.cols) {
                val start = col
                val value = board.get(row, col)
                while (col + 1 < board.cols && board.get(row, col + 1) == value) {
                    col++
                }
                val length = col - start + 1
                if (value >= 0 && length >= config.minMatchSize) {
                    runs += MatchRun((start..col).map { c -> Position(row, c) }, length)
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
                if (value >= 0 && length >= config.minMatchSize) {
                    runs += MatchRun((start..row).map { r -> Position(r, col) }, length)
                }
                row++
            }
        }

        return runs
    }

    /** GP-01: the last cell of each exact match-4 run becomes a Moedor instead of being cleared. */
    private fun selectSpecialSpawns(runs: List<MatchRun>): List<Position> {
        return runs.filter { it.length == GRINDER_MATCH_LENGTH }.map { it.positions.last() }
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
        if (value < 0) {
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
