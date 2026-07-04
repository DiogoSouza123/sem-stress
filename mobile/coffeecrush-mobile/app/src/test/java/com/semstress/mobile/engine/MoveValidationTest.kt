package com.semstress.mobile.engine

import com.semstress.mobile.domain.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoveValidationTest {

    private val engine = Match3Engine(stageConfig())

    @Test
    fun `rejeita troca nao adjacente sem alterar o tabuleiro`() {
        val board = boardFrom(
            """
            0 1 2 3 4
            1 2 3 4 0
            2 3 4 0 1
            """
        )
        val before = board.snapshot()

        val result = engine.tryMove(board, Position(0, 0), Position(2, 2))

        assertFalse(result.valid)
        assertEquals(before, board.snapshot())
    }

    @Test
    fun `rejeita troca adjacente que nao gera match sem alterar o tabuleiro`() {
        val board = boardFrom(
            """
            0 1 2 3 4
            1 2 3 4 0
            2 3 4 0 1
            """
        )
        val before = board.snapshot()

        val result = engine.tryMove(board, Position(0, 0), Position(0, 1))

        assertFalse(result.valid)
        assertEquals(before, board.snapshot())
    }

    @Test
    fun `aceita troca adjacente que gera match`() {
        val board = boardFrom(
            """
            1 2 1 3 4
            3 1 4 2 0
            """
        )

        val result = engine.tryMove(board, Position(0, 1), Position(1, 1))

        assertTrue(result.valid)
        assertTrue(result.points >= 500, "Pontuacao esperada >= 500, foi ${result.points}")
    }

    @Test
    fun `rejeita posicoes fora do tabuleiro`() {
        val board = boardFrom(
            """
            0 1 2
            1 2 0
            """
        )

        val result = engine.tryMove(board, Position(0, 0), Position(5, 5))

        assertFalse(result.valid)
        assertEquals(0, result.points)
    }
}
