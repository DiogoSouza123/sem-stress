package com.semstress.mobile.domain

data class PlayerProgress(
    val highestUnlockedStage: Int = 1,
    val currentStage: Int = 1,
    val bestScores: Map<Int, Int> = emptyMap(),
    val starsByStage: Map<Int, Int> = emptyMap()
) {
    fun isUnlocked(stageId: Int): Boolean = stageId <= highestUnlockedStage

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
