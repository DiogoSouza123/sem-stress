package com.semstress.mobile.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StarRatingTest {

    @Test
    fun `retorna zero estrelas quando a fase nao foi vencida`() {
        val stars = calculateStars(
            won = false,
            score = 999_999,
            targetScore = 1000,
            movesRemaining = 20,
            initialMoves = 20
        )
        assertEquals(0, stars)
    }

    @Test
    fun `retorna uma estrela ao apenas atingir a meta`() {
        val stars = calculateStars(won = true, score = 1000, targetScore = 1000, movesRemaining = 0, initialMoves = 20)
        assertEquals(1, stars)
    }

    @Test
    fun `retorna duas estrelas ao superar o limiar de 1,5x a meta`() {
        val stars = calculateStars(won = true, score = 1600, targetScore = 1000, movesRemaining = 0, initialMoves = 20)
        assertEquals(2, stars)
    }

    @Test
    fun `retorna tres estrelas ao superar o limiar de 2x a meta`() {
        val stars = calculateStars(won = true, score = 2000, targetScore = 1000, movesRemaining = 0, initialMoves = 20)
        assertEquals(3, stars)
    }

    @Test
    fun `retorna tres estrelas em vitoria eficiente mesmo sem o limiar alto de pontos`() {
        val stars = calculateStars(won = true, score = 1600, targetScore = 1000, movesRemaining = 10, initialMoves = 20)
        assertEquals(3, stars)
    }

    @Test
    fun `meta zero conta como uma estrela ao vencer`() {
        val stars = calculateStars(won = true, score = 500, targetScore = 0, movesRemaining = 0, initialMoves = 20)
        assertEquals(1, stars)
    }
}
