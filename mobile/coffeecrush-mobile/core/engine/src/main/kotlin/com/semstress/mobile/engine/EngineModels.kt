package com.semstress.mobile.engine

import com.semstress.mobile.domain.Position

data class MoveOutcome(
    val valid: Boolean,
    val points: Int,
    val cascades: Int
)

data class AnimatedMoveOutcome(
    val valid: Boolean,
    val points: Int,
    val cascades: Int,
    val rounds: List<AnimationRound>
)

data class ResolveOutcome(
    val points: Int,
    val cascades: Int
)

data class AnimatedResolveOutcome(
    val points: Int,
    val cascades: Int,
    val rounds: List<AnimationRound>
)

/** A single piece dropping one step due to gravity, or a new piece filling an empty cell. */
sealed interface BoardEvent {
    data class Moved(val from: Position, val to: Position, val piece: Int) : BoardEvent
    data class Spawned(val position: Position, val piece: Int) : BoardEvent
}

/**
 * One cascade step: the pieces matched at [matchedPositions], then the sequence of [BoardEvent]
 * frames replaying how gravity settled the board afterwards. Each inner list of [fallSteps] is one
 * animation frame — either every piece that fell in a single gravity pass, or a single newly
 * spawned piece — mirroring the frame granularity of the desktop reference implementation, but as
 * O(pieces that moved) events instead of a full `rows x cols` board snapshot per frame.
 */
data class AnimationRound(
    val matchedPositions: List<Position>,
    val fallSteps: List<List<BoardEvent>>,
    val roundPoints: Int,
    val specialSpawns: List<Position> = emptyList()
)

data class MatchGroup(
    val positions: Set<Position>,
    val runLengths: List<Int>
)

/** GP-01: a single detected run (all same value, contiguous, in one direction) with its cell order preserved. */
data class MatchRun(
    val positions: List<Position>,
    val length: Int
)

/**
 * GP-01: result of tapping a special piece. [milledPieces] pairs each affected neighbor with the
 * piece value it held before being cleared, so callers can credit collect objectives.
 */
data class SpecialActivationOutcome(
    val activated: Boolean,
    val points: Int,
    val milledPieces: List<Pair<Position, Int>>,
    val fallSteps: List<List<BoardEvent>>
)
