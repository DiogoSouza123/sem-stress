package com.semstress.mobile.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

private const val DAY_ONE = 19_000L
private const val DAY_TWO = 19_001L

class DailyChallengeTest {

    @Test
    fun `comeca com todas as tentativas disponiveis`() {
        val progress = PlayerProgress()
        assertEquals(DAILY_CHALLENGE_MAX_ATTEMPTS, progress.dailyAttemptsRemaining(DAY_ONE))
    }

    @Test
    fun `cada tentativa registrada reduz as tentativas restantes no mesmo dia`() {
        var progress = PlayerProgress()
        progress = progress.registerDailyAttempt(DAY_ONE, score = 1000)
        assertEquals(DAILY_CHALLENGE_MAX_ATTEMPTS - 1, progress.dailyAttemptsRemaining(DAY_ONE))

        progress = progress.registerDailyAttempt(DAY_ONE, score = 500)
        assertEquals(DAILY_CHALLENGE_MAX_ATTEMPTS - 2, progress.dailyAttemptsRemaining(DAY_ONE))
    }

    @Test
    fun `tentativas nao passam de zero mesmo registrando alem do limite`() {
        var progress = PlayerProgress()
        repeat(DAILY_CHALLENGE_MAX_ATTEMPTS + 2) {
            progress = progress.registerDailyAttempt(DAY_ONE, score = 100)
        }
        assertEquals(0, progress.dailyAttemptsRemaining(DAY_ONE))
    }

    @Test
    fun `tentativas resetam num novo dia`() {
        var progress = PlayerProgress()
        progress = progress.registerDailyAttempt(DAY_ONE, score = 1000)
        assertEquals(DAILY_CHALLENGE_MAX_ATTEMPTS - 1, progress.dailyAttemptsRemaining(DAY_ONE))

        assertEquals(DAILY_CHALLENGE_MAX_ATTEMPTS, progress.dailyAttemptsRemaining(DAY_TWO))
    }

    @Test
    fun `guarda a melhor pontuacao diaria entre tentativas`() {
        var progress = PlayerProgress()
        progress = progress.registerDailyAttempt(DAY_ONE, score = 1000)
        progress = progress.registerDailyAttempt(DAY_ONE, score = 500)
        assertEquals(1000, progress.dailyBestScore)
    }

    @Test
    fun `dailyChallengeSeed e igual ao epoch day informado`() {
        assertEquals(DAY_ONE, dailyChallengeSeed(DAY_ONE))
    }
}
