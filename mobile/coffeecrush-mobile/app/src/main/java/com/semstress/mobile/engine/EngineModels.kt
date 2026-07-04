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

data class AnimationRound(
    val stateBeforeClear: List<List<Int>>,
    val matchedPositions: List<Position>,
    val stateAfterClear: List<List<Int>>,
    val fallFrames: List<List<List<Int>>>,
    val roundPoints: Int
)

data class MatchGroup(
    val positions: Set<Position>,
    val runLengths: List<Int>
)
