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
    fun `Moedor sem gesto de swap queima ao longo da propria corrida horizontal`() {
        val board = boardFrom(
            """
            4 4 4 4 2
            0 1 2 3 4
            1 2 3 4 0
            2 3 4 0 1
            3 4 0 1 2
            """
        )

        val outcome = engine.resolveBoardAnimated(board)

        val grinderCount = board.snapshot().flatten().count { it == Match3Engine.SPECIAL_GRINDER }
        assertEquals(0, grinderCount, "O Moedor detona ao nascer - nao deveria permanecer no tabuleiro")
        // Passo 1: o match explode e o Moedor recem-criado e exibido na celula de spawn.
        val matchStep = outcome.rounds[0]
        assertTrue(
            matchStep.specialSpawns.any { it.pieceValue == Match3Engine.SPECIAL_GRINDER },
            "O passo do match deveria exibir o Moedor recem-criado"
        )
        // Passo 2: o Moedor detona ao longo da corrida - uma unica linha, nunca cruz.
        val blastStep = outcome.rounds[1]
        assertTrue(
            blastStep.matchedPositions.contains(Position(0, 4)),
            "A explosao deveria queimar o resto da linha da corrida"
        )
        assertFalse(
            blastStep.matchedPositions.contains(Position(1, 3)),
            "A explosao nao deveria descer pela coluna"
        )
    }

    @Test
    fun `Moedor sem gesto de swap queima ao longo da propria corrida vertical`() {
        val board = boardFrom(
            """
            4 0 1 2 3
            4 1 2 3 0
            4 2 3 0 1
            4 3 0 1 2
            0 4 1 2 3
            """
        )

        val outcome = engine.resolveBoardAnimated(board)

        val grinderCount = board.snapshot().flatten().count { it == Match3Engine.SPECIAL_GRINDER }
        assertEquals(0, grinderCount, "O Moedor detona ao nascer - nao deveria permanecer no tabuleiro")
        // Passo 2: o Moedor detona ao longo da corrida - uma unica coluna, nunca cruz.
        val blastStep = outcome.rounds[1]
        assertTrue(
            blastStep.matchedPositions.contains(Position(4, 0)),
            "A explosao deveria queimar o resto da coluna da corrida"
        )
        assertFalse(
            blastStep.matchedPositions.contains(Position(3, 1)),
            "A explosao nao deveria abrir pela linha"
        )
    }

    @Test
    fun `ativar Moedor moi a coluna inteira e pontua`() {
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
        assertEquals(4, outcome.affectedPieces.size)
        assertEquals(outcome.affectedPieces.size * 500, outcome.points)
        for (row in 0 until board.rows) {
            assertNotEquals(Match3Engine.SPECIAL_GRINDER, board.get(row, grinderPosition.col))
        }
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
    fun `L-shape de match-5 detona a Prensa Francesa na hora amassando os vizinhos`() {
        val board = boardFrom(
            """
            4 4 4 2 3
            4 0 1 2 3
            4 1 2 3 0
            0 2 3 0 1
            1 3 0 1 2
            """
        )

        val outcome = engine.resolveBoard(board)

        val frenchPressCount = board.snapshot().flatten().count { it == Match3Engine.SPECIAL_FRENCH_PRESS }
        assertEquals(0, frenchPressCount, "A Prensa Francesa detona ao nascer - nao deveria permanecer no tabuleiro")
        // Dois bracos de 3 (2 * 500) + o vizinho (1,1) amassado pela detonacao (500).
        assertTrue(outcome.points >= 1500, "Esperava os pontos do L-shape mais a detonacao dos vizinhos")
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
    fun `ativar Prensa Francesa amassa os 8 vizinhos e pontua`() {
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
        assertEquals(8, outcome.affectedPieces.size)
        assertEquals(outcome.affectedPieces.size * 500, outcome.points)
        assertNotEquals(Match3Engine.SPECIAL_FRENCH_PRESS, board.get(pressPosition.row, pressPosition.col))
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

    @Test
    fun `alinhamento criado pela ativacao de um especial explode em cascata`() {
        // French press at (2,2) clears rows 1-3 x cols 1-3. Gravity then drops row 0's "2 2 3"
        // (cols 1-3) onto row 3, forming "2 2 2" with the untouched (3,0) - a match the activation
        // MUST resolve instead of leaving aligned pieces sitting on the board.
        val board = boardFrom(
            """
            1 2 2 3 4
            2 3 4 0 1
            3 4 0 1 2
            2 0 1 2 3
            0 1 2 3 4
            """,
            pieceTypes = 5
        )
        val pressPosition = Position(2, 2)
        board.set(pressPosition.row, pressPosition.col, Match3Engine.SPECIAL_FRENCH_PRESS)

        val outcome = engine.activateSpecialPiece(board, pressPosition)

        assertTrue(outcome.activated)
        assertTrue(outcome.cascadeRounds.isNotEmpty(), "Esperava a cascata do alinhamento criado pela ativacao")
        assertTrue(outcome.cascadePoints >= 500, "A cascata pos-ativacao deveria pontuar")
        assertFalse(hasAnyMatch(board), "Nenhum alinhamento pode sobrar no tabuleiro apos a ativacao")
    }

    @Test
    fun `Moedor criado por um swap queima ao longo da corrida do match`() {
        // Swapping (1,2) up into (0,2) completes the horizontal match-4 "4 4 4 4" on row 0. The
        // Moedor detonates along the run: the whole row 0 burns as one continuous line - it never
        // opens a perpendicular column (the "cross" bug).
        val board = boardFrom(
            """
            4 4 0 4 2
            1 2 4 3 1
            2 3 0 1 2
            3 0 1 2 3
            0 1 2 3 0
            """,
            pieceTypes = 5
        )

        val outcome = engine.tryMoveAnimated(board, Position(1, 2), Position(0, 2))

        assertTrue(outcome.valid)
        val grinderCount = board.snapshot().flatten().count { it == Match3Engine.SPECIAL_GRINDER }
        assertEquals(0, grinderCount, "O Moedor detona ao nascer - nao deveria permanecer no tabuleiro")
        // Passo 1 exibe o Moedor na celula onde a peca do jogador pousou.
        val matchStep = outcome.rounds[0]
        assertEquals(
            listOf(Position(0, 2)),
            matchStep.specialSpawns.filter { it.pieceValue == Match3Engine.SPECIAL_GRINDER }.map { it.position },
            "O passo do match deveria exibir o Moedor na celula do swap"
        )
        // Passo 2 detona ao longo da corrida.
        val blastStep = outcome.rounds[1]
        assertTrue(
            blastStep.matchedPositions.contains(Position(0, 4)),
            "A explosao deveria queimar o resto da linha da corrida"
        )
        assertFalse(
            blastStep.matchedPositions.contains(Position(2, 2)),
            "A explosao nao deveria descer pela coluna da celula de origem"
        )
        assertFalse(
            blastStep.matchedPositions.contains(Position(2, 3)),
            "A explosao nao deveria descer pela coluna do fim da corrida"
        )
    }
}
