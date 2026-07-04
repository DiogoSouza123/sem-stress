package com.semstress.mobile.data

import android.content.Context
import com.semstress.mobile.domain.PlayerProgress

interface ProgressStore {
    fun load(totalStages: Int): PlayerProgress
    fun save(progress: PlayerProgress)
}

class ProgressRepository(context: Context) : ProgressStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(totalStages: Int): PlayerProgress {
        val highestUnlocked = prefs.getInt(KEY_HIGHEST_UNLOCKED, 1).coerceIn(1, totalStages)
        val currentStage = prefs.getInt(KEY_CURRENT_STAGE, 1).coerceIn(1, totalStages)

        val bestScores = mutableMapOf<Int, Int>()
        for (stageId in 1..totalStages) {
            val score = prefs.getInt(scoreKey(stageId), 0)
            if (score > 0) {
                bestScores[stageId] = score
            }
        }

        return PlayerProgress(
            highestUnlockedStage = highestUnlocked,
            currentStage = currentStage,
            bestScores = bestScores
        )
    }

    override fun save(progress: PlayerProgress) {
        val editor = prefs.edit()
        editor.putInt(KEY_HIGHEST_UNLOCKED, progress.highestUnlockedStage)
        editor.putInt(KEY_CURRENT_STAGE, progress.currentStage)

        progress.bestScores.forEach { (stageId, score) ->
            editor.putInt(scoreKey(stageId), score)
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "coffee_crush_mobile_progress"
        private const val KEY_HIGHEST_UNLOCKED = "highest_unlocked_stage"
        private const val KEY_CURRENT_STAGE = "current_stage"

        private fun scoreKey(stageId: Int): String = "stage_${stageId}_best_score"
    }
}
