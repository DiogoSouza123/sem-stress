package com.semstress.mobile.data

import android.content.Context
import android.util.Log
import com.semstress.mobile.domain.StageCatalog
import com.semstress.mobile.domain.StageConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

interface StageCatalogSource {
    suspend fun load(): StageCatalog
}

/**
 * Reads the RR-16 JSON stage catalog. On-device overrides (internal `files/config`, then
 * `externalFilesDir/config`) take priority over the bundled asset, same override precedence the
 * old `.properties`-based repository used, so devices with an override already installed keep
 * working after upgrading to this format.
 */
class StageRepository @Inject constructor(@ApplicationContext private val context: Context) : StageCatalogSource {

    override suspend fun load(): StageCatalog = withContext(Dispatchers.IO) {
        runCatching {
            StageCatalogJsonParser.parse(readConfigText())
        }.getOrElse { ex ->
            Log.e("StageRepository", "Failed to load stage configuration. Using fallback.", ex)
            fallbackCatalog()
        }
    }

    private fun readConfigText(): String {
        val overrideFile = resolveOverrideFile()
        if (overrideFile != null) {
            return overrideFile.readText()
        }
        return context.assets.open(ASSET_STAGES_CONFIG).use { input -> input.readBytes().decodeToString() }
    }

    private fun resolveOverrideFile(): File? {
        val internal = File(File(context.filesDir, CONFIG_DIR), STAGES_CONFIG_FILE)
        val external = context.getExternalFilesDir(null)?.let { File(File(it, CONFIG_DIR), STAGES_CONFIG_FILE) }
        return listOfNotNull(internal, external).firstOrNull { it.exists() }
    }

    private fun fallbackCatalog(): StageCatalog = StageCatalog(
        stages = listOf(
            StageConfig(
                id = 1,
                name = "Fase 1 - Inicio",
                description = "Fallback",
                rows = 6,
                cols = 6,
                pieceTypes = 5,
                initialMoves = 20,
                targetScore = 5000,
                musicName = DEFAULT_STAGE_MUSIC,
                musicVolumePercent = 70
            )
        ),
        menuMusicName = DEFAULT_MENU_MUSIC,
        menuMusicVolumePercent = DEFAULT_MENU_MUSIC_VOLUME
    )

    companion object {
        private const val CONFIG_DIR = "config"
        private const val STAGES_CONFIG_FILE = "stages.json"
        private const val ASSET_STAGES_CONFIG = "config/stages.json"

        private const val DEFAULT_MENU_MUSIC = "luke_bergs_waesto_take_off"
        private const val DEFAULT_STAGE_MUSIC = "orchestronika_motivation"
        private const val DEFAULT_MENU_MUSIC_VOLUME = 70
        const val SILENT_MUSIC_RESOURCE = StageCatalogJsonParser.SILENT_MUSIC_RESOURCE
    }
}
