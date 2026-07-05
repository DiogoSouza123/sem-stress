package com.semstress.mobile.engine

import com.semstress.mobile.domain.Position
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SpecialPieceTest {

    private val engine = Match3Engine(stageConfig(rows = 5, cols = 5, pieceTypes = 5))

    @Test
    fun `match-4 deixa um Moedor no lugar de limpar todas as pecas`() {
        val board = boardFrom(
            """
            4 4 4 4 2
            0 1 2 3 4
            1 2 3 4 0
            2 3 4 0 1
            3 4 0 1 2
            """
        )

        engine.resolveBoard(board)

        val grinderCount = board.snapshot().flatten().count { it == Match3Engine.SPECIAL_GRINDER }
        assertEquals(1, grinderCount, "Esperava exatamente um Moedor criado pelo match-4")
    }

    @Test
    fun `activar Moedor mói os 8 vizinhos e pontua`() {
        val board = boardFrom(
            """
            1 2 3 4 0
            2 3 4 0 1
            3 4 0 1 2
            4 0 1 2 3
            0 1 2 3 4
            """,
            pieceTypes = 5
        )
        val grinderPosition = Position(2, 2)
        board.set(grinderPosition.row, grinderPosition.col, Match3Engine.SPECIAL_GRINDER)

        val outcome = engine.activateSpecialPiece(board, grinderPosition)

        assertTrue(outcome.activated)
        assertEquals(8, outcome.milledPieces.size)
        assertEquals(outcome.milledPieces.size * 500, outcome.points)
        assertNotEquals(Match3Engine.SPECIAL_GRINDER, board.get(grinderPosition.row, grinderPosition.col))
    }

    @Test
    fun `ativar celula que nao e Moedor nao faz nada`() {
        val board = boardFrom(
            """
            1 2 3
            2 3 1
            3 1 2
            """
        )
        val before = board.snapshot()

        val outcome = engine.activateSpecialPiece(board, Position(1, 1))

        assertFalse(outcome.activated)
        assertEquals(0, outcome.points)
        assertTrue(outcome.milledPieces.isEmpty())
        assertEquals(before, board.snapshot())
    }

    @Test
    fun `Moedor nao participa de matches nem impede a resolucao do board`() {
        val board = boardFrom(
            """
            1 2 3 4 0
            2 3 4 0 1
            3 4 0 1 2
            4 0 1 2 3
            0 1 2 3 4
            """,
            pieceTypes = 5
        )
        board.set(2, 2, Match3Engine.SPECIAL_GRINDER)

        val outcome = engine.resolveBoard(board)

        assertEquals(0, outcome.points)
        assertEquals(Match3Engine.SPECIAL_GRINDER, board.get(2, 2))
    }
}
