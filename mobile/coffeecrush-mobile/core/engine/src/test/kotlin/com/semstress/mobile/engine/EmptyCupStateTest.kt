package com.semstress.mobile.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmptyCupStateTest {

    @Test
    fun `encode e decode preservam turnos restantes e pecas absorvidas`() {
        val value = EmptyCupState.encode(turnsRemaining = 2, absorbed = 7)

        assertTrue(EmptyCupState.matches(value))
        assertEquals(2, EmptyCupState.turnsRemaining(value))
        assertEquals(7, EmptyCupState.absorbed(value))
    }

    @Test
    fun `valores normais e outros especiais nao sao confundidos com Xicara Vazia`() {
        assertFalse(EmptyCupState.matches(0))
        assertFalse(EmptyCupState.matches(4))
        assertFalse(EmptyCupState.matches(Match3Engine.EMPTY))
        assertFalse(EmptyCupState.matches(Match3Engine.SPECIAL_GRINDER))
        assertFalse(EmptyCupState.matches(Match3Engine.SPECIAL_FRENCH_PRESS))
    }

    @Test
    fun `absorvido e limitado ao maximo suportado pela codificacao`() {
        val value = EmptyCupState.encode(turnsRemaining = 3, absorbed = 999)

        assertEquals(EmptyCupState.MAX_ABSORBED, EmptyCupState.absorbed(value))
    }

    @Test
    fun `turnos restantes sao limitados entre 0 e o valor inicial`() {
        val negative = EmptyCupState.encode(turnsRemaining = -5, absorbed = 0)
        val tooMany = EmptyCupState.encode(turnsRemaining = 999, absorbed = 0)

        assertEquals(0, EmptyCupState.turnsRemaining(negative))
        assertEquals(EmptyCupState.INITIAL_TURNS, EmptyCupState.turnsRemaining(tooMany))
    }
}
