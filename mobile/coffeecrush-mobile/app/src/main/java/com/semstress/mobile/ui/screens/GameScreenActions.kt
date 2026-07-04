package com.semstress.mobile.ui.screens

/** Bundles [GameScreen]'s callbacks so the composable stays under the parameter-count limit. */
data class GameScreenActions(
    val onCellTap: (row: Int, col: Int) -> Unit,
    val onCellDragSwap: (fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) -> Unit,
    val onBackToMenu: () -> Unit,
    val onReplayStage: () -> Unit,
    val onToggleMusic: () -> Unit
)
