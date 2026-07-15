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
 * GP-01: a special piece created this round, and the board value it should become.
 * [blastAlongRow] is the direction a Moedor burns when it detonates: always along the run that
 * created it (horizontal match = row, vertical match = column), so the blast and the match form
 * one continuous line instead of a cross.
 */
data class SpecialSpawn(
    val position: Position,
    val pieceValue: Int,
    val blastAlongRow: Boolean = false
)

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
    val specialSpawns: List<SpecialSpawn> = emptyList()
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
 * GP-01: result of activating a special piece (Moedor/Prensa Francesa/Xicara Vazia), whether by tap
 * or (Xicara Vazia only) automatically once its countdown expires. [affectedPieces] pairs each
 * cleared cell with the piece value it held before being cleared, so callers can credit collect
 * objectives; [triggerPosition] is the special piece's own cell, cleared separately by the caller
 * so it can be included in the highlight/explosion animation alongside [affectedPieces].
 * [cascadeRounds]/[cascadePoints] carry the regular match resolution that runs after the
 * activation's refill settles — alignments created by the power-up explode like any other match.
 */
data class SpecialActivationOutcome(
    val activated: Boolean,
    val points: Int,
    val affectedPieces: List<Pair<Position, Int>>,
    val fallSteps: List<List<BoardEvent>>,
    val triggerPosition: Position? = null,
    val cascadeRounds: List<AnimationRound> = emptyList(),
    val cascadePoints: Int = 0
)
