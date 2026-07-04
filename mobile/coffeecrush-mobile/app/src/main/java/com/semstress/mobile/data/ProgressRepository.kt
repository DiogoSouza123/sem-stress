package com.semstress.mobile.data

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import com.semstress.mobile.data.proto.PlayerProgressProtoOuterClass.PlayerProgressProto
import com.semstress.mobile.domain.PlayerProgress
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ProgressStore {
    suspend fun load(totalStages: Int): PlayerProgress
    suspend fun save(progress: PlayerProgress)
}

internal const val LEGACY_PROGRESS_PREFS_NAME = "coffee_crush_mobile_progress"
private const val KEY_HIGHEST_UNLOCKED = "highest_unlocked_stage"
private const val KEY_CURRENT_STAGE = "current_stage"
private const val MAX_LEGACY_STAGE_SCAN = 200

internal fun legacyScoreKey(stageId: Int): String = "stage_${stageId}_best_score"

/**
 * Player progress backed by Proto DataStore (RR-06). Reads used to be a synchronous
 * `SharedPreferences` call on whichever thread asked for it; this store is suspend-only so
 * callers must already be on a coroutine (both ViewModels call it from [ioDispatcher]-launched
 * coroutines since RR-02).
 *
 * On first read, [legacySharedPreferencesMigration] copies over any progress saved by the old
 * `coffee_crush_mobile_progress` SharedPreferences file, so installs predating this migration
 * don't lose progress.
 */
@Singleton
class ProgressRepository @Inject constructor(
    @ApplicationContext context: Context,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ProgressStore {

    private val dataStore: DataStore<PlayerProgressProto> = DataStoreFactory.create(
        serializer = PlayerProgressSerializer,
        migrations = listOf(legacySharedPreferencesMigration(context)),
        scope = scope,
        produceFile = { File(context.filesDir, "datastore/$DATASTORE_FILE_NAME") }
    )

    override suspend fun load(totalStages: Int): PlayerProgress {
        val proto = dataStore.data.first()
        val highestUnlocked = if (proto.highestUnlockedStage > 0) proto.highestUnlockedStage else 1
        val currentStage = if (proto.currentStage > 0) proto.currentStage else 1
        return PlayerProgress(
            highestUnlockedStage = highestUnlocked.coerceIn(1, totalStages),
            currentStage = currentStage.coerceIn(1, totalStages),
            bestScores = proto.bestScoresMap.filterValues { it > 0 }
        )
    }

    override suspend fun save(progress: PlayerProgress) {
        dataStore.updateData { current ->
            current.toBuilder()
                .setHighestUnlockedStage(progress.highestUnlockedStage)
                .setCurrentStage(progress.currentStage)
                .clearBestScores()
                .putAllBestScores(progress.bestScores)
                .build()
        }
    }

    companion object {
        private const val DATASTORE_FILE_NAME = "player_progress.pb"
    }
}

internal fun legacySharedPreferencesMigration(context: Context): DataMigration<PlayerProgressProto> =
    SharedPreferencesMigration(
        context = context,
        sharedPreferencesName = LEGACY_PROGRESS_PREFS_NAME
    ) { prefs: SharedPreferencesView, currentData: PlayerProgressProto ->
        val builder = currentData.toBuilder()
        if (prefs.contains(KEY_HIGHEST_UNLOCKED)) {
            builder.highestUnlockedStage = prefs.getInt(KEY_HIGHEST_UNLOCKED, 1)
        }
        if (prefs.contains(KEY_CURRENT_STAGE)) {
            builder.currentStage = prefs.getInt(KEY_CURRENT_STAGE, 1)
        }
        for (stageId in 1..MAX_LEGACY_STAGE_SCAN) {
            val key = legacyScoreKey(stageId)
            if (prefs.contains(key)) {
                val score = prefs.getInt(key, 0)
                if (score > 0) {
                    builder.putBestScores(stageId, score)
                }
            }
        }
        builder.build()
    }
