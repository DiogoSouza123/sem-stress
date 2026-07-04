package com.semstress.mobile.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ScoringTest {

    /**
     * Single vertical match in column 0 only; the refill (seed = 2) chains into a
     * second cascade round, giving a deterministic 2-round scoring scenario.
     */
    private fun cascadeBoard() = boardFrom(
        pattern = """
            0 1 0
            0 0 1
            0 1 1
            1 0 0
        """,
        pieceTypes = 2,
        seed = 2L
    )

    @Test
    fun `pontua apenas a primeira rodada quando cascata esta desabilitada`() {
        val config = stageConfig(pieceTypes = 2, scoreMatch3 = 100, cascadeMultiplier = 2, scoreCascade = false)
        val engine = Match3Engine(config)

        val outcome = engine.resolveBoard(cascadeBoard())

        assertEquals(2, outcome.cascades, "Esperava 2 rodadas de cascata no cenario de teste")
        assertEquals(100, outcome.points)
    }

    @Test
    fun `pontua mais quando cascata esta habilitada`() {
        val configSemCascata = stageConfig(
            pieceTypes = 2,
            scoreMatch3 = 100,
            cascadeMultiplier = 2,
            scoreCascade = false
        )
        val configComCascata = stageConfig(
            pieceTypes = 2,
            scoreMatch3 = 100,
            cascadeMultiplier = 2,
            scoreCascade = true
        )

        val pontosSemCascata = Match3Engine(configSemCascata).resolveBoard(cascadeBoard()).points
        val pontosComCascata = Match3Engine(configComCascata).resolveBoard(cascadeBoard()).points

        assertEquals(100, pontosSemCascata)
        assertEquals(300, pontosComCascata)
        assertTrue(pontosComCascata > pontosSemCascata)
    }

    @Test
    fun `multiplicador de cascata aplica progressao linear por rodada`() {
        val config = stageConfig(pieceTypes = 2, scoreMatch3 = 100, cascadeMultiplier = 3, scoreCascade = true)

        val outcome = Match3Engine(config).resolveBoard(cascadeBoard())

        // Rodada 1: 100 * 1. Rodada 2: 100 * (1 + (2-1)*(3-1)) = 100 * 3.
        assertEquals(400, outcome.points)
    }
}
