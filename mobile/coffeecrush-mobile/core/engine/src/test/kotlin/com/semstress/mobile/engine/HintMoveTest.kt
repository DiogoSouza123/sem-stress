package com.semstress.mobile.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HintMoveTest {

    private val engine = Match3Engine(stageConfig(rows = 4, cols = 4, pieceTypes = 5))

    @Test
    fun `encontra um par que gera match quando existe jogada disponivel`() {
        val board = boardFrom(
            """
            0 1 0 2
            3 0 4 1
            2 4 1 3
            1 2 3 4
            """
        )

        val hint = engine.findAvailableMove(board)

        assertTrue(hint != null, "Esperava um par de posicoes, mas nao encontrou nenhum")
        val (first, second) = requireNotNull(hint)
        val outcome = engine.tryMove(board, first, second)
        assertTrue(outcome.valid, "A jogada sugerida ($first -> $second) deveria ser valida")
    }

    @Test
    fun `retorna nulo quando nao ha jogada disponivel`() {
        val board = boardFrom(
            """
            0 1 2 3
            1 2 3 4
            2 3 4 0
            3 4 0 1
            """
        )

        val hint = engine.findAvailableMove(board)

        assertNull(hint)
    }

    @Test
    fun `hint retorna posicoes adjacentes`() {
        val board = boardFrom(
            """
            0 1 0 2
            3 0 4 1
            2 4 1 3
            1 2 3 4
            """
        )

        val hint = requireNotNull(engine.findAvailableMove(board))
        val rowDiff = kotlin.math.abs(hint.first.row - hint.second.row)
        val colDiff = kotlin.math.abs(hint.first.col - hint.second.col)
        assertEquals(1, rowDiff + colDiff)
    }
}
