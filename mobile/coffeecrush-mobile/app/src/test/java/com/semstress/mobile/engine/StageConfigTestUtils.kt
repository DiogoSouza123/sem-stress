package com.semstress.mobile.engine

import com.semstress.mobile.domain.StageConfig

@Suppress("LongParameterList")
fun stageConfig(
    rows: Int = 8,
    cols: Int = 8,
    pieceTypes: Int = 5,
    minMatchSize: Int = 3,
    scoreMatch3: Int = 500,
    scoreMatch4: Int = 1000,
    scoreMatch5Plus: Int = 1500,
    cascadeMultiplier: Int = 1,
    scoreCascade: Boolean = true,
    initialMoves: Int = 20,
    targetScore: Int = 2000,
    consumeInvalidMove: Boolean = false,
    onlyAdjacentSwap: Boolean = true
): StageConfig = StageConfig(
    id = 0,
    name = "Test Stage",
    description = "Stage used in unit tests",
    rows = rows,
    cols = cols,
    pieceTypes = pieceTypes,
    minMatchSize = minMatchSize,
    scoreMatch3 = scoreMatch3,
    scoreMatch4 = scoreMatch4,
    scoreMatch5Plus = scoreMatch5Plus,
    cascadeMultiplier = cascadeMultiplier,
    scoreCascade = scoreCascade,
    initialMoves = initialMoves,
    targetScore = targetScore,
    consumeInvalidMove = consumeInvalidMove,
    onlyAdjacentSwap = onlyAdjacentSwap
)
