package com.semstress.mobile.engine

/**
 * GP-01: Xicara Vazia ("Empty Cup") needs two pieces of state - turns remaining and pieces
 * absorbed so far - that must travel with the piece as gravity moves it around the board. Since
 * [Match3Board] only stores a single `Int` per cell, both are packed into one negative value
 * (mirroring [Match3Engine.SPECIAL_GRINDER]'s single-sentinel approach) instead of adding a
 * separate per-position side-store that gravity/drop code would need to know how to move too.
 */
object EmptyCupState {
    private const val BASE = -1000
    private const val TURN_MULTIPLIER = 100

    const val INITIAL_TURNS = 3
    const val MAX_ABSORBED = TURN_MULTIPLIER - 1
    private const val MAX_OFFSET = INITIAL_TURNS * TURN_MULTIPLIER + MAX_ABSORBED

    fun encode(turnsRemaining: Int, absorbed: Int): Int {
        val cappedAbsorbed = absorbed.coerceIn(0, MAX_ABSORBED)
        val cappedTurns = turnsRemaining.coerceIn(0, INITIAL_TURNS)
        return BASE - (cappedTurns * TURN_MULTIPLIER + cappedAbsorbed)
    }

    fun matches(value: Int): Boolean {
        val offset = BASE - value
        return offset in 0..MAX_OFFSET
    }

    fun turnsRemaining(value: Int): Int = (BASE - value) / TURN_MULTIPLIER

    fun absorbed(value: Int): Int = (BASE - value) % TURN_MULTIPLIER
}
