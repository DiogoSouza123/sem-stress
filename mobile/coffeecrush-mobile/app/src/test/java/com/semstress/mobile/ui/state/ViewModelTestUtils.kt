package com.semstress.mobile.ui.state

import com.semstress.mobile.domain.PlayerProgress
import com.semstress.mobile.domain.Position
import com.semstress.mobile.domain.StageCatalog
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.engine.Match3Board
import com.semstress.mobile.engine.Match3Engine

fun catalogOf(vararg stages: StageConfig): StageCatalog = StageCatalog(
    stages = stages.toList(),
    menuMusicName = "menu_music",
    menuMusicVolumePercent = 70
)

fun progressUnlockingUpTo(stageId: Int): PlayerProgress = PlayerProgress(highestUnlockedStage = stageId)

/**
 * Finds an adjacent pair of positions on [board] that is valid/invalid under [config]'s rules,
 * using the real engine as ground truth. Needed because [GameViewModel] creates its board with an
 * unseeded RNG, so the concrete layout is unknown ahead of time.
 */
fun findMovePair(board: List<List<Int>>, config: StageConfig, wantValid: Boolean): Pair<Position, Position>? {
    val engine = Match3Engine(config)
    return adjacentPairs(board.size, board[0].size)
        .firstOrNull { pair -> isMatch(engine, board, config.pieceTypes, pair) == wantValid }
}

private fun adjacentPairs(rows: Int, cols: Int): Sequence<Pair<Position, Position>> = sequence {
    for (row in 0 until rows) {
        for (col in 0 until cols) {
            if (col + 1 < cols) yield(Position(row, col) to Position(row, col + 1))
            if (row + 1 < rows) yield(Position(row, col) to Position(row + 1, col))
        }
    }
}

private fun isMatch(
    engine: Match3Engine,
    board: List<List<Int>>,
    pieceTypes: Int,
    pair: Pair<Position, Position>
): Boolean {
    val scratch = Match3Board(board.size, board[0].size, pieceTypes, seed = 1L)
    scratch.overwrite(board)
    return engine.tryMove(scratch, pair.first, pair.second).valid
}
