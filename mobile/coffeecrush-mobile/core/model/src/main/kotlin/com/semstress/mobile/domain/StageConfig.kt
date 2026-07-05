package com.semstress.mobile.domain

data class StageConfig(
    val id: Int,
    val name: String,
    val description: String,
    val rows: Int,
    val cols: Int,
    val pieceTypes: Int,
    val minMatchSize: Int = 3,
    val scoreMatch3: Int = 500,
    val scoreMatch4: Int = 1000,
    val scoreMatch5Plus: Int = 1500,
    val cascadeMultiplier: Int = 1,
    val scoreCascade: Boolean = true,
    val initialMoves: Int,
    val targetScore: Int,
    val consumeInvalidMove: Boolean = false,
    val onlyAdjacentSwap: Boolean = true,
    val backgroundName: String = "coffee_bg",
    val musicName: String = "orchestronika_motivation",
    val musicVolumePercent: Int = 70,
    val collectObjective: CollectObjective? = null,
    val region: String? = null,
    val isZenMode: Boolean = false
)
