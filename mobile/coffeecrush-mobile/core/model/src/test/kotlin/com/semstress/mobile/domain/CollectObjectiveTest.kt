package com.semstress.mobile.domain

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CollectObjectiveTest {

    @Test
    fun `nao esta completo enquanto o coletado for menor que o alvo`() {
        val objective = CollectObjective(pieceType = 2, count = 10)
        assertFalse(objective.isComplete(collected = 9))
    }

    @Test
    fun `esta completo quando o coletado atinge o alvo`() {
        val objective = CollectObjective(pieceType = 2, count = 10)
        assertTrue(objective.isComplete(collected = 10))
    }

    @Test
    fun `esta completo quando o coletado excede o alvo`() {
        val objective = CollectObjective(pieceType = 2, count = 10)
        assertTrue(objective.isComplete(collected = 15))
    }
}
