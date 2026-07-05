package com.semstress.mobile.engine

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VaporTest {

    /**
     * Column 0 is engineered so that clearing cascades exactly 3 levels using only pre-existing
     * values (no reliance on which random piece refills the top): round 1 clears the two "gap"
     * triples (value 4 and value 1); round 2 finds the three value-0 cells now adjacent (they were
     * split by the value-1 gap); round 3 finds the three value-3 cells now adjacent (they were
     * split by the value-0 group, which only just cleared). Columns 1-3 are inert filler (never
     * matches, never falls) - just there so the top-2-rows Vapor reshuffles has more than 2 cells
     * to rearrange, since a 1-column board could shuffle two equal values into a "no-op".
     */
    private val threeLevelCascadeBoard = """
        3 2 5 2
        4 5 2 5
        4 2 5 2
        4 5 2 5
        3 2 5 2
        0 5 2 5
        0 2 5 2
        1 5 2 5
        1 2 5 2
        1 5 2 5
        0 2 5 2
        3 5 2 5
    """

    @Test
    fun `cascata de 3 niveis aciona o Vapor e embaralha as 2 linhas do topo`() {
        val engine = Match3Engine(stageConfig(rows = 12, cols = 4, pieceTypes = 6, minMatchSize = 3))
        val board = boardFrom(threeLevelCascadeBoard)

        val outcome = engine.resolveBoardAnimated(board)

        assertTrue(outcome.cascades >= 3, "Esperava pelo menos 3 niveis de cascata, foi ${outcome.cascades}")
        val reshuffleEvents = outcome.rounds.flatMap { it.fallSteps }.flatten()
            .filterIsInstance<BoardEvent.Reshuffled>()
        assertTrue(reshuffleEvents.isNotEmpty(), "Esperava que o Vapor gerasse eventos Reshuffled ao atingir 3 niveis")
    }

    @Test
    fun `cascata de menos de 3 niveis nao aciona o Vapor`() {
        val engine = Match3Engine(stageConfig(rows = 6, cols = 1, pieceTypes = 5, minMatchSize = 3))
        // Mesma tecnica de "merge apos o gap", mas com um unico gap - so 2 niveis de cascata.
        val board = boardFrom(
            """
            0
            0
            1
            1
            1
            0
            """
        )

        val outcome = engine.resolveBoardAnimated(board)

        assertTrue(outcome.cascades < 3, "Nao esperava 3+ niveis de cascata neste tabuleiro, foi ${outcome.cascades}")
        val reshuffleEvents = outcome.rounds.flatMap { it.fallSteps }.flatten()
            .filterIsInstance<BoardEvent.Reshuffled>()
        assertTrue(reshuffleEvents.isEmpty(), "Nao esperava eventos Reshuffled sem atingir o limiar do Vapor")
    }
}
