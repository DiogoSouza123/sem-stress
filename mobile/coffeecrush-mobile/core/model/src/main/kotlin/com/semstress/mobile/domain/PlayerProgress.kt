package com.semstress.mobile.domain

data class PlayerProgress(
    val highestUnlockedStage: Int = 1,
    val currentStage: Int = 1,
    val bestScores: Map<Int, Int> = emptyMap(),
    val starsByStage: Map<Int, Int> = emptyMap(),
    val dailyChallengeEpochDay: Long = 0L,
    val dailyAttemptsUsedToday: Int = 0,
    val dailyBestScore: Int = 0
) {
    fun isUnlocked(stageId: Int): Boolean = stageId <= highestUnlockedStage

    /** GP-05: attempts reset once [today] (an epoch day) differs from the last day the challenge was played. */
    fun dailyAttemptsRemaining(today: Long, maxAttempts: Int = DAILY_CHALLENGE_MAX_ATTEMPTS): Int {
        val usedToday = if (today == dailyChallengeEpochDay) dailyAttemptsUsedToday else 0
        return (maxAttempts - usedToday).coerceAtLeast(0)
    }

    fun registerDailyAttempt(
        today: Long,
        score: Int,
        maxAttempts: Int = DAILY_CHALLENGE_MAX_ATTEMPTS
    ): PlayerProgress {
        val usedToday = if (today == dailyChallengeEpochDay) dailyAttemptsUsedToday else 0
        return copy(
            dailyChallengeEpochDay = today,
            dailyAttemptsUsedToday = (usedToday + 1).coerceAtMost(maxAttempts),
            dailyBestScore = maxOf(dailyBestScore, score)
        )
    }

    fun scoreFor(stageId: Int): Int = bestScores[stageId] ?: 0

    fun starsFor(stageId: Int): Int = starsByStage[stageId] ?: 0

    fun totalStars(): Int = starsByStage.values.sum()

    fun completedStagesCount(): Int = bestScores.values.count { it > 0 }

    fun totalScore(): Int = bestScores.values.filter { it > 0 }.sum()

    fun averageScore(): Int {
        val completed = completedStagesCount()
        if (completed == 0) {
            return 0
        }
        return totalScore() / completed
    }

    fun registerResult(
        stageId: Int,
        score: Int,
        won: Boolean,
        totalStages: Int,
        stars: Int = 0
    ): PlayerProgress {
        val previousBest = scoreFor(stageId)
        val updatedBest = if (score > previousBest) {
            bestScores + (stageId to score)
        } else {
            bestScores
        }

        val previousStars = starsFor(stageId)
        val updatedStars = if (stars > previousStars) {
            starsByStage + (stageId to stars)
        } else {
            starsByStage
        }

        var unlocked = highestUnlockedStage
        if (won) {
            unlocked = maxOf(unlocked, minOf(totalStages, stageId + 1))
        }

        val updatedCurrent = maxOf(1, stageId)
        return copy(
            highestUnlockedStage = unlocked,
            currentStage = updatedCurrent,
            bestScores = updatedBest,
            starsByStage = updatedStars
        )
    }
}
