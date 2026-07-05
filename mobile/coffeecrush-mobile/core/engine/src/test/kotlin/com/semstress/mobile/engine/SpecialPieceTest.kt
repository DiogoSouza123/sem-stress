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
        assertEquals(8, outcome.affectedPieces.size)
        assertEquals(outcome.affectedPieces.size * 500, outcome.points)
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
        assertTrue(outcome.affectedPieces.isEmpty())
        assertEquals(before, board.snapshot())
    }

    @Test
    fun `L-shape de match-5 cria uma Prensa Francesa`() {
        val board = boardFrom(
            """
            4 4 4 2 3
            4 0 1 2 3
            4 1 2 3 0
            0 2 3 0 1
            1 3 0 1 2
            """
        )

        engine.resolveBoard(board)

        // A peca especial cai como qualquer outra ate assentar - a coluna 0 tinha 2 celulas vazias
        // abaixo da intersecao (0,0), entao ela deveria assentar na linha 2 apos a gravidade.
        val frenchPressCount = board.snapshot().flatten().count { it == Match3Engine.SPECIAL_FRENCH_PRESS }
        assertEquals(1, frenchPressCount, "Esperava exatamente uma Prensa Francesa criada pelo L-shape")
        assertEquals(Match3Engine.SPECIAL_FRENCH_PRESS, board.get(2, 0))
    }

    @Test
    fun `um bloco solido nao gera Prensa Francesa (nao e um L-T limpo)`() {
        val board = boardFrom(
            """
            1 1 1 3 4
            1 1 1 4 0
            1 1 1 0 1
            2 3 4 0 1
            3 4 0 1 2
            """
        )

        engine.resolveBoard(board)

        val frenchPressCount = board.snapshot().flatten().count { it == Match3Engine.SPECIAL_FRENCH_PRESS }
        assertEquals(0, frenchPressCount, "Um bloco 3x3 solido nao deveria virar Prensa Francesa")
    }

    @Test
    fun `ativar Prensa Francesa limpa a coluna inteira e pontua`() {
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
        val pressPosition = Position(2, 2)
        board.set(pressPosition.row, pressPosition.col, Match3Engine.SPECIAL_FRENCH_PRESS)

        val outcome = engine.activateSpecialPiece(board, pressPosition)

        assertTrue(outcome.activated)
        assertEquals(4, outcome.affectedPieces.size)
        assertEquals(outcome.affectedPieces.size * 500, outcome.points)
        for (row in 0 until board.rows) {
            assertNotEquals(Match3Engine.SPECIAL_FRENCH_PRESS, board.get(row, pressPosition.col))
        }
    }

    @Test
    fun `match-5 em linha reta cria uma Xicara Vazia com o countdown inicial`() {
        val board = boardFrom(
            """
            4 4 4 4 4
            0 1 2 3 4
            1 2 3 4 0
            2 3 4 0 1
            3 4 0 1 2
            """
        )

        engine.resolveBoard(board)

        val value = board.get(0, 4)
        assertTrue(EmptyCupState.matches(value), "Esperava uma Xicara Vazia na ultima celula da corrida")
        assertEquals(EmptyCupState.INITIAL_TURNS, EmptyCupState.turnsRemaining(value))
        assertEquals(0, EmptyCupState.absorbed(value))
    }

    @Test
    fun `tickEmptyCups absorve matches adjacentes e decrementa o countdown`() {
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
        val cupPosition = Position(2, 2)
        board.set(cupPosition.row, cupPosition.col, EmptyCupState.encode(EmptyCupState.INITIAL_TURNS, absorbed = 0))
        val matchedThisMove = listOf(Position(2, 1), Position(1, 2))

        val explosions = engine.tickEmptyCups(board, matchedThisMove)

        assertTrue(explosions.isEmpty(), "Ainda restam turnos, nao deveria explodir")
        val value = board.get(cupPosition.row, cupPosition.col)
        assertEquals(EmptyCupState.INITIAL_TURNS - 1, EmptyCupState.turnsRemaining(value))
        assertEquals(2, EmptyCupState.absorbed(value))
    }

    @Test
    fun `tickEmptyCups explode automaticamente quando o countdown chega a zero`() {
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
        val cupPosition = Position(2, 2)
        board.set(cupPosition.row, cupPosition.col, EmptyCupState.encode(1, absorbed = 4))

        val explosions = engine.tickEmptyCups(board, matchedThisMove = emptyList())

        assertEquals(1, explosions.size)
        val explosion = explosions.single()
        assertTrue(explosion.activated)
        assertEquals((explosion.affectedPieces.size + 4) * 500, explosion.points)
        assertFalse(EmptyCupState.matches(board.get(cupPosition.row, cupPosition.col)))
    }

    @Test
    fun `tocar em uma Xicara Vazia detona cedo com pontuacao proporcional ao que absorveu`() {
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
        val cupPosition = Position(2, 2)
        board.set(cupPosition.row, cupPosition.col, EmptyCupState.encode(2, absorbed = 3))

        val outcome = engine.activateSpecialPiece(board, cupPosition)

        assertTrue(outcome.activated)
        assertEquals((outcome.affectedPieces.size + 3) * 500, outcome.points)
        assertFalse(EmptyCupState.matches(board.get(cupPosition.row, cupPosition.col)))
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
