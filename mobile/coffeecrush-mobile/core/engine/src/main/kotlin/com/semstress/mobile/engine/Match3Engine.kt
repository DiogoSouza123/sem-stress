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

        /** GP-01: "Prensa Francesa", created by an L/T-shaped match-5; compresses its whole column on activation. */
        const val SPECIAL_FRENCH_PRESS = -3

        private const val MAX_SHUFFLE_ATTEMPTS = 300
        private const val GRINDER_MATCH_LENGTH = 4
        private const val LT_SHAPE_ARM_LENGTH = 3
        private const val EMPTY_CUP_MIN_LENGTH = 5
        private const val VAPOR_CASCADE_THRESHOLD = 3
        private const val VAPOR_TOP_ROWS = 2
        private const val VAPOR_MAX_SHUFFLE_ATTEMPTS = 200

        /** GP-01: true for any special piece value - the three tappable ones, or Xicara Vazia in any of its states. */
        fun isSpecialPiece(value: Int): Boolean {
            return value == SPECIAL_GRINDER || value == SPECIAL_FRENCH_PRESS || EmptyCupState.matches(value)
        }
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

        // The swapped cells are the preferred spawn spots: a special created by this move appears
        // where the player acted (Candy Crush behavior), not at an arbitrary end of the run.
        val resolved = resolveBoardAnimated(board, captureRounds = true, preferredSpawns = setOf(first, second))
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
        captureRounds: Boolean,
        preferredSpawns: Set<Position> = emptySet()
    ): AnimatedResolveOutcome {
        var totalPoints = 0
        var cascadeLevel = 0
        var vaporTriggered = false
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
            // Only the move's own matches (first cascade) anchor to the swapped cells; later
            // cascades were not caused directly by the player's gesture.
            val spawnAnchors = if (cascadeLevel == 1) preferredSpawns else emptySet()
            val specialSpawns = selectSpecialSpawns(runs, board, spawnAnchors)
            val matchedPositions = if (captureRounds) {
                allPositions.sortedWith(compareBy<Position> { it.row }.thenBy { it.col })
            } else {
                emptyList()
            }

            clearMatched(board, allPositions)
            specialSpawns.forEach { spawn -> board.set(spawn.position.row, spawn.position.col, spawn.pieceValue) }
            val fallSteps = if (captureRounds) {
                collapseAndRefillCapturingEvents(board)
            } else {
                collapseAndRefill(board)
                emptyList()
            }

            val vaporDue = !vaporTriggered && cascadeLevel == VAPOR_CASCADE_THRESHOLD
            vaporTriggered = vaporTriggered || vaporDue
            val vaporEvents = if (vaporDue) steamReshuffleTopRows(board) else emptyList()

            if (captureRounds) {
                rounds += AnimationRound(
                    matchedPositions = matchedPositions,
                    fallSteps = if (vaporEvents.isEmpty()) fallSteps else fallSteps + listOf(vaporEvents),
                    roundPoints = roundPoints,
                    specialSpawns = specialSpawns
                )
            }
        }
    }

    /**
     * GP-01: activates whichever special piece sits at [position] - Moedor (mills 8 neighbors),
     * Prensa Francesa (compresses its column) or Xicara Vazia (explodes a 3x3 area, tap-detonated
     * early instead of waiting out its countdown - see [tickEmptyCups]).
     */
    fun activateSpecialPiece(board: Match3Board, position: Position): SpecialActivationOutcome {
        if (!board.isValid(position)) {
            return notActivated()
        }
        val value = board.get(position.row, position.col)
        return when {
            value == SPECIAL_GRINDER -> activateGrinder(board, position)
            value == SPECIAL_FRENCH_PRESS -> activateFrenchPress(board, position)
            EmptyCupState.matches(value) -> activateEmptyCup(board, position, EmptyCupState.absorbed(value))
            else -> notActivated()
        }
    }

    private fun notActivated(): SpecialActivationOutcome {
        return SpecialActivationOutcome(
            activated = false,
            points = 0,
            affectedPieces = emptyList(),
            fallSteps = emptyList()
        )
    }

    private fun activateGrinder(board: Match3Board, position: Position): SpecialActivationOutcome {
        val milled = moorePositions(board, position).mapNotNull { neighbor ->
            val value = board.get(neighbor.row, neighbor.col)
            if (value >= 0) neighbor to value else null
        }
        board.set(position.row, position.col, EMPTY)
        milled.forEach { (neighbor, _) -> board.set(neighbor.row, neighbor.col, EMPTY) }
        return settleActivation(
            board = board,
            points = milled.size * config.scoreMatch3,
            affectedPieces = milled,
            triggerPosition = position
        )
    }

    /** GP-01: clears the whole column - the pieces above "crush" the ones below - and refills it from the top. */
    private fun activateFrenchPress(board: Match3Board, position: Position): SpecialActivationOutcome {
        val affected = (0 until board.rows).mapNotNull { row ->
            val value = board.get(row, position.col)
            if (value >= 0) Position(row, position.col) to value else null
        }
        for (row in 0 until board.rows) {
            board.set(row, position.col, EMPTY)
        }
        return settleActivation(
            board = board,
            points = affected.size * config.scoreMatch3,
            affectedPieces = affected,
            triggerPosition = position
        )
    }

    /** GP-01: explodes a 3x3 area; [absorbed] (pieces absorbed while it sat on the board) scales the bonus. */
    private fun activateEmptyCup(board: Match3Board, position: Position, absorbed: Int): SpecialActivationOutcome {
        val area = moorePositions(board, position) + position
        val affected = area.mapNotNull { pos ->
            val value = board.get(pos.row, pos.col)
            if (value >= 0) pos to value else null
        }
        area.forEach { pos -> board.set(pos.row, pos.col, EMPTY) }
        return settleActivation(
            board = board,
            points = (affected.size + absorbed) * config.scoreMatch3,
            affectedPieces = affected,
            triggerPosition = position
        )
    }

    /**
     * Shared tail of every special activation: refill the cleared cells, then run the regular
     * match resolution — alignments of 3+ created by the power-up (or its refill) must explode
     * like any other match instead of sitting on the board.
     */
    private fun settleActivation(
        board: Match3Board,
        points: Int,
        affectedPieces: List<Pair<Position, Int>>,
        triggerPosition: Position
    ): SpecialActivationOutcome {
        val fallSteps = collapseAndRefillCapturingEvents(board)
        val cascade = resolveBoardAnimated(board, captureRounds = true)
        return SpecialActivationOutcome(
            activated = true,
            points = points,
            affectedPieces = affectedPieces,
            fallSteps = fallSteps,
            triggerPosition = triggerPosition,
            cascadeRounds = cascade.rounds,
            cascadePoints = cascade.points
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

    /**
     * GP-01: called once per player move to age every Xicara Vazia on the board by a turn, credit
     * it for any of [matchedThisMove] that landed in its Moore neighborhood, and auto-detonate any
     * whose countdown just ran out (see [activateSpecialPiece] for the player-triggered early path).
     */
    fun tickEmptyCups(board: Match3Board, matchedThisMove: List<Position>): List<SpecialActivationOutcome> {
        val cupPositions = (0 until board.rows).flatMap { row ->
            (0 until board.cols).mapNotNull { col ->
                Position(row, col).takeIf { EmptyCupState.matches(board.get(row, col)) }
            }
        }

        val explosions = mutableListOf<SpecialActivationOutcome>()
        cupPositions.forEach { position ->
            val value = board.get(position.row, position.col)
            if (!EmptyCupState.matches(value)) {
                return@forEach
            }
            val absorbedNow = EmptyCupState.absorbed(value) + matchedThisMove.count { isMooreAdjacent(position, it) }
            val remainingTurns = EmptyCupState.turnsRemaining(value) - 1
            if (remainingTurns <= 0) {
                explosions += activateEmptyCup(board, position, absorbedNow)
            } else {
                board.set(position.row, position.col, EmptyCupState.encode(remainingTurns, absorbedNow))
            }
        }
        return explosions
    }

    private fun isMooreAdjacent(center: Position, other: Position): Boolean {
        return other != center && abs(center.row - other.row) <= 1 && abs(center.col - other.col) <= 1
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

    /**
     * GP-01: Vapor - triggered once a move's cascade reaches [VAPOR_CASCADE_THRESHOLD] levels.
     * Rearranges the existing pieces of the top [VAPOR_TOP_ROWS] rows (never introduces new values)
     * into a permutation with no immediate match and at least one available move, smoothing the
     * "stuck top of board" RNG problem instead of just rewarding cascades with points.
     */
    private fun steamReshuffleTopRows(board: Match3Board): List<BoardEvent.Reshuffled> {
        val affectedRows = minOf(VAPOR_TOP_ROWS, board.rows)
        val positions = (0 until affectedRows).flatMap { row -> (0 until board.cols).map { col -> Position(row, col) } }
        val original = positions.map { board.get(it.row, it.col) }

        var chosen = original
        var attempts = 0
        var valid = false
        while (attempts < VAPOR_MAX_SHUFFLE_ATTEMPTS && !valid) {
            chosen = shuffledValues(original, board)
            applyValues(board, positions, chosen)
            valid = findMatchRuns(board).isEmpty() && hasAvailableMove(board)
            attempts++
        }

        return positions.indices.mapNotNull { index ->
            if (original[index] == chosen[index]) null else BoardEvent.Reshuffled(positions[index], chosen[index])
        }
    }

    private fun shuffledValues(values: List<Int>, board: Match3Board): List<Int> {
        val shuffled = values.toMutableList()
        for (i in shuffled.indices.reversed()) {
            val j = board.nextIndex(i + 1)
            val temp = shuffled[i]
            shuffled[i] = shuffled[j]
            shuffled[j] = temp
        }
        return shuffled
    }

    private fun applyValues(board: Match3Board, positions: List<Position>, values: List<Int>) {
        positions.indices.forEach { index -> board.set(positions[index].row, positions[index].col, values[index]) }
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

    /**
     * GP-01: each exact match-4 run becomes a Moedor; a straight run of 5+ becomes a Xicara Vazia;
     * a length-3 run crossing another length-3 run of the same value (L/T shape) becomes a Prensa
     * Francesa at the intersection instead - checked first, since it consumes the two arms that
     * would otherwise each just be a plain 3-match. The special spawns at the run cell the player's
     * swap landed in when one is part of the run ([preferredSpawns], Candy Crush behavior);
     * cascade-created runs fall back to the run's last cell.
     */
    private fun selectSpecialSpawns(
        runs: List<MatchRun>,
        board: Match3Board,
        preferredSpawns: Set<Position>
    ): List<SpecialSpawn> {
        val (lShapeSpawns, consumedRuns) = findLShapeSpawns(runs, board)
        val spawns = lShapeSpawns.toMutableList()
        runs.filterNot { it in consumedRuns }.forEach { run ->
            when {
                run.length == GRINDER_MATCH_LENGTH ->
                    spawns += SpecialSpawn(spawnCellFor(run, preferredSpawns), SPECIAL_GRINDER)
                run.length >= EMPTY_CUP_MIN_LENGTH -> {
                    val cupValue = EmptyCupState.encode(EmptyCupState.INITIAL_TURNS, absorbed = 0)
                    spawns += SpecialSpawn(spawnCellFor(run, preferredSpawns), cupValue)
                }
            }
        }
        return spawns
    }

    private fun spawnCellFor(run: MatchRun, preferredSpawns: Set<Position>): Position =
        run.positions.firstOrNull { it in preferredSpawns } ?: run.positions.last()

    /**
     * Pairs each length-3 horizontal run with a crossing length-3 vertical run of the same value,
     * to spawn a Prensa Francesa at the intersection - but only when the horizontal run crosses
     * EXACTLY one vertical run. A denser cluster (e.g. a solid 3x3 block) makes every run cross
     * multiple others, which is not really an "L/T" shape - those are left to clear normally.
     */
    private fun findLShapeSpawns(runs: List<MatchRun>, board: Match3Board): Pair<List<SpecialSpawn>, Set<MatchRun>> {
        val horizontalArms = runs.filter { it.length == LT_SHAPE_ARM_LENGTH && it.isHorizontalRun() }
        val verticalArms = runs.filter { it.length == LT_SHAPE_ARM_LENGTH && it.isVerticalRun() }
        val spawns = mutableListOf<SpecialSpawn>()
        val consumed = mutableSetOf<MatchRun>()

        horizontalArms.forEach { horizontalArm ->
            if (horizontalArm in consumed) {
                return@forEach
            }
            val sameValue = board.get(horizontalArm.positions[0].row, horizontalArm.positions[0].col)
            val crossing = verticalArms.filter { arm ->
                arm !in consumed &&
                    board.get(arm.positions[0].row, arm.positions[0].col) == sameValue &&
                    arm.positions.any { it in horizontalArm.positions }
            }
            if (crossing.size == 1) {
                val verticalArm = crossing.single()
                val intersection = horizontalArm.positions.first { it in verticalArm.positions }
                spawns += SpecialSpawn(intersection, SPECIAL_FRENCH_PRESS)
                consumed += horizontalArm
                consumed += verticalArm
            }
        }
        return spawns to consumed
    }

    private fun MatchRun.isHorizontalRun(): Boolean = positions.size > 1 && positions[0].row == positions[1].row

    private fun MatchRun.isVerticalRun(): Boolean = positions.size > 1 && positions[0].col == positions[1].col

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
