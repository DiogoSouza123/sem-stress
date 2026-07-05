package com.semstress.mobile.data

import android.content.Context
import com.semstress.mobile.domain.PlayerProgress
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * RR-06 acceptance: migrating from the legacy `SharedPreferences`-backed progress must preserve
 * the player's data. Uses a real `SharedPreferences` instance (via Robolectric), not a fake, to
 * exercise the actual `SharedPreferencesMigration` path.
 */
@RunWith(RobolectricTestRunner::class)
class ProgressRepositoryMigrationTest {

    @Test
    fun `migra progresso de SharedPreferences reais para o DataStore preservando os valores`() = runTest {
        val context: Context = RuntimeEnvironment.getApplication()
        val legacyPrefs = context.getSharedPreferences(LEGACY_PROGRESS_PREFS_NAME, Context.MODE_PRIVATE)
        legacyPrefs.edit()
            .putInt("highest_unlocked_stage", 4)
            .putInt("current_stage", 3)
            .putInt(legacyScoreKey(1), 1200)
            .putInt(legacyScoreKey(2), 800)
            .putInt(legacyScoreKey(3), 0)
            .commit()

        val repository = ProgressRepository(context)
        val migrated = repository.load(totalStages = 10)

        assertEquals(4, migrated.highestUnlockedStage)
        assertEquals(3, migrated.currentStage)
        assertEquals(1200, migrated.scoreFor(1))
        assertEquals(800, migrated.scoreFor(2))
        assertEquals(0, migrated.scoreFor(3))
    }

    @Test
    fun `progresso salvo apos a migracao e preservado em leituras seguintes`() = runTest {
        // ProgressRepository e um @Singleton em producao (Hilt); duas instancias simultaneas
        // sobre o mesmo arquivo nao e um cenario real (DataStore proibe isso), entao o round-trip
        // save->load na mesma instancia e o que de fato precisa ser garantido aqui.
        val context: Context = RuntimeEnvironment.getApplication()
        val legacyPrefs = context.getSharedPreferences(LEGACY_PROGRESS_PREFS_NAME, Context.MODE_PRIVATE)
        legacyPrefs.edit().putInt("highest_unlocked_stage", 2).commit()

        val repository = ProgressRepository(context)
        val afterMigration = repository.load(totalStages = 10)
        repository.save(
            afterMigration.registerResult(stageId = 2, score = 999, won = true, totalStages = 10, stars = 3)
        )

        val reloaded = repository.load(totalStages = 10)

        assertEquals(999, reloaded.scoreFor(2))
        assertEquals(3, reloaded.highestUnlockedStage)
        assertEquals(3, reloaded.starsFor(2))
    }

    @Test
    fun `progresso salvo antes do campo de estrelas existir carrega com zero estrelas`() = runTest {
        // GP-03: stars_by_stage e um campo proto novo sem equivalente legado em SharedPreferences;
        // instalacoes anteriores a esta mudanca nunca escreveram esse campo, entao o proto3 default
        // (mapa vazio) precisa ser o que o app le, sem exigir migracao especial.
        val context: Context = RuntimeEnvironment.getApplication()
        val repository = ProgressRepository(context)
        repository.save(PlayerProgress(highestUnlockedStage = 2, currentStage = 2, bestScores = mapOf(1 to 500)))

        val reloaded = repository.load(totalStages = 10)

        assertEquals(500, reloaded.scoreFor(1))
        assertEquals(0, reloaded.starsFor(1))
        assertEquals(0, reloaded.totalStars())
    }
}
