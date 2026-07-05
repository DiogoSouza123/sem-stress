package com.semstress.mobile.domain

data class PlayerProgress(
    val highestUnlockedStage: Int = 1,
    val currentStage: Int = 1,
    val bestScores: Map<Int, Int> = emptyMap()
) {
    fun isUnlocked(stageId: Int): Boolean = stageId <= highestUnlockedStage

    fun scoreFor(stageId: Int): Int = bestScores[stageId] ?: 0

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
        totalStages: Int
    ): PlayerProgress {
        val previousBest = scoreFor(stageId)
        val updatedBest = if (score > previousBest) {
            bestScores + (stageId to score)
        } else {
            bestScores
        }

        var unlocked = highestUnlockedStage
        if (won) {
            unlocked = maxOf(unlocked, minOf(totalStages, stageId + 1))
        }

        val updatedCurrent = maxOf(1, stageId)
        return copy(
            highestUnlockedStage = unlocked,
            currentStage = updatedCurrent,
            bestScores = updatedBest
        )
    }
}
