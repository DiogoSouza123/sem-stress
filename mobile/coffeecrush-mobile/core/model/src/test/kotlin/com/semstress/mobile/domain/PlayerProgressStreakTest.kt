package com.semstress.mobile.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val DAY_ONE = 19_000L
private const val DAY_TWO = 19_001L
private const val DAY_FOUR = 19_003L

class PlayerProgressStreakTest {

    @Test
    fun `primeira vez jogando comeca um streak de 1 dia`() {
        val progress = PlayerProgress().registerPlay(DAY_ONE)
        assertEquals(1, progress.currentStreakDays)
        assertEquals(DAY_ONE, progress.lastPlayedEpochDay)
    }

    @Test
    fun `jogar no dia seguinte estende o streak`() {
        val progress = PlayerProgress().registerPlay(DAY_ONE).registerPlay(DAY_TWO)
        assertEquals(2, progress.currentStreakDays)
    }

    @Test
    fun `jogar de novo no mesmo dia nao conta duas vezes`() {
        val progress = PlayerProgress().registerPlay(DAY_ONE).registerPlay(DAY_ONE)
        assertEquals(1, progress.currentStreakDays)
    }

    @Test
    fun `pular um dia reinicia o streak em 1`() {
        val progress = PlayerProgress().registerPlay(DAY_ONE).registerPlay(DAY_FOUR)
        assertEquals(1, progress.currentStreakDays)
    }
}
