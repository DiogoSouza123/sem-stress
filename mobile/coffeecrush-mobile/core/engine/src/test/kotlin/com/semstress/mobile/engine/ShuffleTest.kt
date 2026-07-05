package com.semstress.mobile.engine

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShuffleTest {

    private val engine = Match3Engine(stageConfig(rows = 4, cols = 4, pieceTypes = 5))

    @Test
    fun `detecta quando ha movimento disponivel`() {
        val board = boardFrom(
            """
            0 1 0 2
            3 0 4 1
            2 4 1 3
            1 2 3 4
            """
        )

        assertTrue(engine.hasAvailableMove(board))
    }

    @Test
    fun `detecta quando nao ha movimento disponivel`() {
        val board = boardFrom(
            """
            0 1 2 3
            1 2 3 4
            2 3 4 0
            3 4 0 1
            """
        )

        assertFalse(engine.hasAvailableMove(board))
    }

    @Test
    fun `shuffleWithoutMatches resolve para um tabuleiro jogavel e sem matches`() {
        val board = boardFrom(
            """
            0 1 2 3
            1 2 3 4
            2 3 4 0
            3 4 0 1
            """
        )

        engine.shuffleWithoutMatches(board)

        assertFalse(hasAnyMatch(board))
        assertTrue(engine.hasAvailableMove(board))
    }

    @Test
    fun `ensurePlayableBoard nao mexe em tabuleiro que ja e jogavel`() {
        val board = boardFrom(
            """
            0 1 0 2
            3 0 4 1
            2 4 1 3
            1 2 3 4
            """
        )
        val before = board.snapshot()

        engine.ensurePlayableBoard(board)

        assertEqualsSnapshot(before, board)
    }

    @Test
    fun `shuffleWithoutMatches converge mesmo com poucos tipos de peca`() {
        val tightEngine = Match3Engine(stageConfig(rows = 4, cols = 4, pieceTypes = 2))
        val board = boardFrom(
            """
            0 1 0 1
            1 0 1 0
            0 1 0 1
            1 0 1 0
            """,
            pieceTypes = 2
        )

        tightEngine.shuffleWithoutMatches(board)

        assertFalse(hasAnyMatch(board))
        assertTrue(tightEngine.hasAvailableMove(board))
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                val value = board.get(row, col)
                assertTrue(value in 0 until board.pieceTypes, "Valor invalido apos shuffle: $value")
            }
        }
    }

    private fun assertEqualsSnapshot(expected: List<List<Int>>, board: Match3Board) {
        assertTrue(expected == board.snapshot(), "Tabuleiro nao deveria ter sido alterado")
    }
}
