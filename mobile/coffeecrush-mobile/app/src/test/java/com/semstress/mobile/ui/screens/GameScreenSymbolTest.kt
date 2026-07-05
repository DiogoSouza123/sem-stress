package com.semstress.mobile.ui.screens

import com.semstress.mobile.engine.EmptyCupState
import com.semstress.mobile.engine.Match3Engine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * GP-01: guards the fallback glyph shown for each piece type/special when there is no sprite (or
 * symbol mode is on).
 */
class GameScreenSymbolTest {

    @Test
    fun `cada peca especial tem um glifo proprio, distinto das pecas normais`() {
        assertEquals("⚙️", fallbackPieceSymbol(Match3Engine.SPECIAL_GRINDER))
        assertEquals("⬇️", fallbackPieceSymbol(Match3Engine.SPECIAL_FRENCH_PRESS))
        assertEquals("🫗", fallbackPieceSymbol(EmptyCupState.encode(turnsRemaining = 3, absorbed = 0)))
        assertEquals("🫗", fallbackPieceSymbol(EmptyCupState.encode(turnsRemaining = 1, absorbed = 9)))
    }

    @Test
    fun `pecas normais usam o glifo do seu tipo, ciclando a cada 6 valores`() {
        assertEquals("☕", fallbackPieceSymbol(0))
        assertEquals("🫘", fallbackPieceSymbol(1))
        assertEquals("🥛", fallbackPieceSymbol(2))
        assertEquals("🍪", fallbackPieceSymbol(3))
        assertEquals("🟤", fallbackPieceSymbol(4))
        assertEquals("⭐", fallbackPieceSymbol(5))
        assertEquals(fallbackPieceSymbol(0), fallbackPieceSymbol(6))
    }
}
