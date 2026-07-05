package com.semstress.mobile.engine

import com.semstress.mobile.domain.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Match3BoardTest {

    @Test
    fun `swap troca as pecas entre duas posicoes`() {
        val board = boardFrom(
            """
            0 1 2
            1 2 0
            2 0 1
            """
        )
        val originalFirst = board.get(0, 0)
        val originalSecond = board.get(0, 1)

        board.swap(Position(0, 0), Position(0, 1))

        assertEquals(originalSecond, board.get(0, 0))
        assertEquals(originalFirst, board.get(0, 1))
    }

    @Test
    fun `fillRandom preenche valores dentro do limite de tipos`() {
        val board = Match3Board(rows = 6, cols = 6, pieceTypes = 5, seed = 7L)

        board.fillRandom()

        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                val value = board.get(row, col)
                assertTrue(value in 0 until 5, "Valor fora da faixa: $value")
            }
        }
    }

    @Test
    fun `set e get persistem o valor na celula`() {
        val board = boardFrom(
            """
            0 1 2
            1 2 0
            """
        )

        board.set(1, 2, 2)

        assertEquals(2, board.get(1, 2))
    }

    @Test
    fun `snapshot e overwrite fazem round-trip do estado`() {
        val board = boardFrom(
            """
            0 1 2 3
            3 2 1 0
            """
        )
        val snapshot = board.snapshot()

        board.set(0, 0, 3)
        board.overwrite(snapshot)

        assertEquals(snapshot, board.snapshot())
    }

    @Test
    fun `isValid reconhece posicoes dentro e fora do tabuleiro`() {
        val board = Match3Board(rows = 4, cols = 3, pieceTypes = 5, seed = 1L)

        assertTrue(board.isValid(Position(0, 0)))
        assertTrue(board.isValid(Position(3, 2)))
        assertTrue(!board.isValid(Position(4, 0)))
        assertTrue(!board.isValid(Position(0, 3)))
        assertTrue(!board.isValid(Position(-1, 0)))
    }

    @Test
    fun `mesmo seed produz sempre a mesma sequencia de pecas`() {
        val boardA = Match3Board(rows = 5, cols = 5, pieceTypes = 6, seed = 99L)
        val boardB = Match3Board(rows = 5, cols = 5, pieceTypes = 6, seed = 99L)

        boardA.fillRandom()
        boardB.fillRandom()

        assertEquals(boardA.snapshot(), boardB.snapshot())
    }
}
