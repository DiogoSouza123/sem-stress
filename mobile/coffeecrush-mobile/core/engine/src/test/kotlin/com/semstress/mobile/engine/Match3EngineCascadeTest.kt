package com.semstress.mobile.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Match3EngineCascadeTest {

    private val engine = Match3Engine(stageConfig(rows = 5, cols = 5, pieceTypes = 5))

    @Test
    fun `pontua match horizontal de 3`() {
        val board = boardFrom(
            """
            2 2 2 3 4
            0 1 2 3 4
            1 2 3 4 0
            2 3 4 0 1
            3 4 0 1 2
            """
        )

        val outcome = engine.resolveBoard(board)

        assertEquals(500, outcome.points)
        assertFalse(hasAnyMatch(board))
    }

    @Test
    fun `pontua match vertical de 3`() {
        val board = boardFrom(
            """
            3 0 1 2 4
            3 1 2 4 0
            3 2 4 0 1
            4 4 0 1 2
            0 0 1 2 3
            """
        )

        val outcome = engine.resolveBoard(board)

        assertEquals(500, outcome.points)
        assertFalse(hasAnyMatch(board))
    }

    @Test
    fun `pontua match horizontal de 4 com pontuacao maior que match de 3`() {
        val board = boardFrom(
            """
            4 4 4 4 2
            0 1 2 3 4
            1 2 3 4 0
            2 3 4 0 1
            3 4 0 1 2
            """
        )

        val outcome = engine.resolveBoard(board)

        assertEquals(1000, outcome.points)
        assertFalse(hasAnyMatch(board))
    }

    @Test
    fun `resolve cascata em bloco sem deixar celulas invalidas`() {
        val board = boardFrom(
            """
            1 1 1 3 4
            1 1 1 4 0
            1 1 1 0 1
            2 3 4 0 1
            3 4 0 1 2
            """
        )

        val outcome = engine.resolveBoard(board)

        assertTrue(outcome.points >= 1500, "Esperava >= 1500 pontos, foi ${outcome.points}")
        for (row in 0 until board.rows) {
            for (col in 0 until board.cols) {
                val value = board.get(row, col)
                assertTrue(value in 0 until board.pieceTypes, "Valor invalido apos cascade: $value")
            }
        }
    }

    @Test
    fun `resolveBoardAnimated captura os eventos de queda de cada rodada`() {
        val board = boardFrom(
            """
            1 1 1 3 4
            0 1 2 3 4
            1 2 3 4 0
            2 3 4 0 1
            3 4 0 1 2
            """
        )

        val outcome = engine.resolveBoardAnimated(board)

        assertTrue(outcome.rounds.isNotEmpty())
        assertTrue(outcome.rounds.first().fallSteps.isNotEmpty())
        assertEquals(outcome.points, outcome.rounds.sumOf { it.roundPoints })
    }

    @Test
    fun `replay dos eventos de resolveBoardAnimated reproduz o board final exatamente`() {
        val original = boardFrom(
            """
            1 1 1 3 4
            0 1 2 3 4
            1 2 3 4 0
            2 3 4 0 1
            3 4 0 1 2
            """
        )
        val replay = original.copyOf()

        val outcome = engine.resolveBoardAnimated(original)

        outcome.rounds.forEach { round ->
            round.matchedPositions.forEach { replay.set(it.row, it.col, Match3Engine.EMPTY) }
            round.specialSpawns.forEach { spawn ->
                replay.set(spawn.position.row, spawn.position.col, spawn.pieceValue)
            }
            round.fallSteps.forEach { step -> step.forEach { event -> replay.apply(event) } }
        }

        assertEquals(original.snapshot(), replay.snapshot())
    }

    @Test
    fun `replay reproduz o board final quando a rodada cria uma peca especial`() {
        val original = boardFrom(
            """
            4 4 4 4 2
            0 1 2 3 4
            1 2 3 4 0
            2 3 4 0 1
            3 4 0 1 2
            """
        )
        val replay = original.copyOf()

        val outcome = engine.resolveBoardAnimated(original)
        assertTrue(
            outcome.rounds.any { it.specialSpawns.isNotEmpty() },
            "Esperava que o match-4 gerasse uma peca especial capturada em specialSpawns"
        )

        outcome.rounds.forEach { round ->
            round.matchedPositions.forEach { replay.set(it.row, it.col, Match3Engine.EMPTY) }
            round.specialSpawns.forEach { spawn ->
                replay.set(spawn.position.row, spawn.position.col, spawn.pieceValue)
            }
            round.fallSteps.forEach { step -> step.forEach { event -> replay.apply(event) } }
        }

        assertEquals(original.snapshot(), replay.snapshot())
    }
}

private fun Match3Board.apply(event: BoardEvent) {
    when (event) {
        is BoardEvent.Moved -> {
            set(event.to.row, event.to.col, event.piece)
            set(event.from.row, event.from.col, Match3Engine.EMPTY)
        }
        is BoardEvent.Spawned -> set(event.position.row, event.position.col, event.piece)
        is BoardEvent.Reshuffled -> set(event.position.row, event.position.col, event.piece)
    }
}
