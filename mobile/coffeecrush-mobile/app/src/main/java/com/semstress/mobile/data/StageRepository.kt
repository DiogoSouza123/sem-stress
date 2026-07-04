package com.semstress.mobile.data

import android.content.Context
import android.util.Log
import com.semstress.mobile.domain.StageCatalog
import com.semstress.mobile.domain.StageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties
import java.util.TreeSet

interface StageCatalogSource {
    suspend fun load(): StageCatalog
}

class StageRepository(private val context: Context) : StageCatalogSource {
    override suspend fun load(): StageCatalog = withContext(Dispatchers.IO) {
        runCatching {
            loadOrThrow()
        }.getOrElse { ex ->
            Log.e("StageRepository", "Falha ao carregar configuracao de fases. Usando fallback.", ex)
            fallbackCatalog()
        }
    }

    private fun loadOrThrow(): StageCatalog {
        val baseProps = loadProperties(
            assetPath = ASSET_BASE_CONFIG,
            overrideFileName = BASE_CONFIG_FILE
        )
        val phasesProps = loadProperties(
            assetPath = ASSET_PHASES_CONFIG,
            overrideFileName = PHASES_CONFIG_FILE
        )

        val base = BaseConfig.from(baseProps)
        val stageIds = resolveStageIds(phasesProps)
        val stages = if (stageIds.isEmpty()) {
            listOf(
                StageConfig(
                    id = 1,
                    name = "Fase 1 - Inicio",
                    description = "Fase padrao",
                    rows = base.rows,
                    cols = base.cols,
                    pieceTypes = base.pieceTypes,
                    minMatchSize = base.minMatchSize,
                    scoreMatch3 = base.scoreMatch3,
                    scoreMatch4 = base.scoreMatch4,
                    scoreMatch5Plus = base.scoreMatch5Plus,
                    cascadeMultiplier = base.cascadeMultiplier,
                    scoreCascade = base.scoreCascade,
                    initialMoves = base.initialMoves,
                    targetScore = base.targetScore,
                    consumeInvalidMove = base.consumeInvalidMove,
                    onlyAdjacentSwap = base.onlyAdjacentSwap,
                    backgroundName = base.backgroundName,
                    musicName = base.musicName,
                    musicVolumePercent = base.musicVolumePercent
                )
            )
        } else {
            stageIds.map { stageId ->
                val stageProps = extractStageProperties(phasesProps, stageId)
                val merged = BaseConfig.from(stageProps, base)
                val stage = StageConfig(
                    id = stageId,
                    name = phasesProps.getProperty("fase.$stageId.nome", "Fase $stageId").trim(),
                    description = phasesProps.getProperty("fase.$stageId.descricao", "").trim(),
                    rows = merged.rows,
                    cols = merged.cols,
                    pieceTypes = merged.pieceTypes,
                    minMatchSize = merged.minMatchSize,
                    scoreMatch3 = merged.scoreMatch3,
                    scoreMatch4 = merged.scoreMatch4,
                    scoreMatch5Plus = merged.scoreMatch5Plus,
                    cascadeMultiplier = merged.cascadeMultiplier,
                    scoreCascade = merged.scoreCascade,
                    initialMoves = merged.initialMoves,
                    targetScore = merged.targetScore,
                    consumeInvalidMove = merged.consumeInvalidMove,
                    onlyAdjacentSwap = merged.onlyAdjacentSwap,
                    backgroundName = merged.backgroundName,
                    musicName = if (merged.musicEnabled) merged.musicName else SILENT_MUSIC_RESOURCE,
                    musicVolumePercent = if (merged.musicEnabled) merged.musicVolumePercent else 0
                )
                validateStage(stage)
                stage
            }
        }

        return StageCatalog(
            stages = stages,
            menuMusicName = if (base.musicEnabled) base.musicName else SILENT_MUSIC_RESOURCE,
            menuMusicVolumePercent = if (base.musicEnabled) base.musicVolumePercent else 0
        )
    }

    private fun fallbackCatalog(): StageCatalog {
        return StageCatalog(
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
    }

    private fun loadProperties(
        assetPath: String,
        overrideFileName: String
    ): Properties {
        val overrideFile = resolveOverrideFile(overrideFileName)
        if (overrideFile != null && overrideFile.exists()) {
            overrideFile.inputStream().use { input ->
                return Properties().apply { load(input) }
            }
        }

        context.assets.open(assetPath).use { input ->
            return Properties().apply { load(input) }
        }
    }

    private fun resolveOverrideFile(fileName: String): File? {
        val internal = File(File(context.filesDir, CONFIG_DIR), fileName)
        if (internal.exists()) {
            return internal
        }

        val externalRoot = context.getExternalFilesDir(null)
        if (externalRoot != null) {
            val external = File(File(externalRoot, CONFIG_DIR), fileName)
            if (external.exists()) {
                return external
            }
        }
        return null
    }

    private fun resolveStageIds(props: Properties): List<Int> {
        val ordered = props.getProperty("fases.ordem", "").trim()
        if (ordered.isNotEmpty()) {
            val ids = mutableListOf<Int>()
            ordered.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { token ->
                    val parsed = token.toIntOrNull()
                    if (parsed != null && parsed > 0 && !ids.contains(parsed)) {
                        ids += parsed
                    }
                }
            if (ids.isNotEmpty()) {
                return ids
            }
        }

        val ids = TreeSet<Int>()
        props.stringPropertyNames().forEach { key ->
            if (!key.startsWith("fase.")) {
                return@forEach
            }
            val rest = key.removePrefix("fase.")
            val index = rest.indexOf('.')
            if (index <= 0) {
                return@forEach
            }
            val id = rest.substring(0, index).toIntOrNull()
            if (id != null && id > 0) {
                ids += id
            }
        }
        return ids.toList()
    }

    private fun extractStageProperties(props: Properties, stageId: Int): Properties {
        val output = Properties()
        val prefix = "fase.$stageId."
        props.stringPropertyNames()
            .filter { it.startsWith(prefix) }
            .forEach { key ->
                val withoutPrefix = key.removePrefix(prefix)
                if (withoutPrefix == "nome" || withoutPrefix == "descricao") {
                    return@forEach
                }
                output.setProperty(withoutPrefix, props.getProperty(key))
            }
        return output
    }

    private fun validateStage(stage: StageConfig) {
        require(stage.rows > 0) { "tabuleiro.linhas deve ser maior que zero (fase ${stage.id})" }
        require(stage.cols > 0) { "tabuleiro.colunas deve ser maior que zero (fase ${stage.id})" }
        require(stage.pieceTypes > 0) { "tabuleiro.tipos_peca deve ser maior que zero (fase ${stage.id})" }
        require(stage.minMatchSize >= 3) { "regras.tamanho_minimo_match deve ser >= 3 (fase ${stage.id})" }
        require(stage.rows >= stage.minMatchSize || stage.cols >= stage.minMatchSize) {
            "Tabuleiro invalido na fase ${stage.id}: ao menos uma dimensao deve ser >= regras.tamanho_minimo_match."
        }
        require(stage.initialMoves > 0) { "jogo.movimentos_iniciais deve ser maior que zero (fase ${stage.id})" }
        require(stage.targetScore >= 0) { "jogo.meta_pontos nao pode ser negativo (fase ${stage.id})" }
        require(stage.musicVolumePercent in 0..100) { "audio.volume_percentual deve estar entre 0 e 100 (fase ${stage.id})" }
    }

    private data class BaseConfig(
        val rows: Int,
        val cols: Int,
        val pieceTypes: Int,
        val minMatchSize: Int,
        val scoreMatch3: Int,
        val scoreMatch4: Int,
        val scoreMatch5Plus: Int,
        val cascadeMultiplier: Int,
        val scoreCascade: Boolean,
        val initialMoves: Int,
        val targetScore: Int,
        val onlyAdjacentSwap: Boolean,
        val consumeInvalidMove: Boolean,
        val backgroundName: String,
        val musicEnabled: Boolean,
        val musicName: String,
        val musicVolumePercent: Int
    ) {
        companion object {
            fun from(props: Properties, fallback: BaseConfig? = null): BaseConfig {
                val base = fallback ?: defaults()
                return BaseConfig(
                    rows = int(props, "tabuleiro.linhas", base.rows),
                    cols = int(props, "tabuleiro.colunas", base.cols),
                    pieceTypes = int(props, "tabuleiro.tipos_peca", base.pieceTypes),
                    minMatchSize = int(props, "regras.tamanho_minimo_match", base.minMatchSize),
                    scoreMatch3 = int(props, "pontuacao.match_3", base.scoreMatch3),
                    scoreMatch4 = int(props, "pontuacao.match_4", base.scoreMatch4),
                    scoreMatch5Plus = int(props, "pontuacao.match_5_ou_mais", base.scoreMatch5Plus),
                    cascadeMultiplier = int(props, "pontuacao.multiplicador_cascata", base.cascadeMultiplier),
                    scoreCascade = bool(props, "pontuacao.pontuar_cascata", base.scoreCascade),
                    initialMoves = int(props, "jogo.movimentos_iniciais", base.initialMoves),
                    targetScore = int(props, "jogo.meta_pontos", base.targetScore),
                    onlyAdjacentSwap = bool(props, "regras.somente_troca_adjacente", base.onlyAdjacentSwap),
                    consumeInvalidMove = bool(props, "regras.consumir_movimento_troca_invalida", base.consumeInvalidMove),
                    backgroundName = normalizedResourceName(
                        text(props, "ui.recurso_background", base.backgroundName),
                        base.backgroundName
                    ),
                    musicEnabled = bool(props, "audio.habilitar_musica_fundo", base.musicEnabled),
                    musicName = normalizedResourceName(text(props, "audio.recurso_musica_fundo", base.musicName), base.musicName),
                    musicVolumePercent = int(props, "audio.volume_percentual", base.musicVolumePercent)
                )
            }

            private fun defaults(): BaseConfig = BaseConfig(
                rows = 6,
                cols = 6,
                pieceTypes = 6,
                minMatchSize = 3,
                scoreMatch3 = 500,
                scoreMatch4 = 1000,
                scoreMatch5Plus = 1500,
                cascadeMultiplier = 1,
                scoreCascade = true,
                initialMoves = 20,
                targetScore = 5000,
                onlyAdjacentSwap = true,
                consumeInvalidMove = false,
                backgroundName = DEFAULT_BACKGROUND_NAME,
                musicEnabled = true,
                musicName = DEFAULT_STAGE_MUSIC,
                musicVolumePercent = DEFAULT_MENU_MUSIC_VOLUME
            )

            private fun int(props: Properties, key: String, fallback: Int): Int {
                return props.getProperty(key)?.trim()?.toIntOrNull() ?: fallback
            }

            private fun bool(props: Properties, key: String, fallback: Boolean): Boolean {
                val raw = props.getProperty(key)?.trim()
                return if (raw.isNullOrEmpty()) fallback else raw.equals("true", ignoreCase = true)
            }

            private fun text(props: Properties, key: String, fallback: String): String {
                val raw = props.getProperty(key)?.trim()
                return if (raw.isNullOrEmpty()) fallback else raw
            }

            private fun normalizedResourceName(raw: String, fallback: String): String {
                val base = raw
                    .trim()
                    .ifEmpty { fallback }
                    .substringAfterLast('/')
                    .substringBeforeLast('.')
                    .lowercase()
                    .replace(Regex("[^a-z0-9_]"), "_")
                    .replace(Regex("_+"), "_")
                    .trim('_')
                if (base.isEmpty()) {
                    return fallback
                }
                return if (base.first().isDigit()) "res_$base" else base
            }
        }
    }

    companion object {
        private const val CONFIG_DIR = "config"
        private const val BASE_CONFIG_FILE = "configuracao-jogo.properties"
        private const val PHASES_CONFIG_FILE = "fases.properties"

        private const val ASSET_BASE_CONFIG = "config/configuracao-jogo.properties"
        private const val ASSET_PHASES_CONFIG = "config/fases.properties"

        private const val DEFAULT_BACKGROUND_NAME = "coffee_bg"
        private const val DEFAULT_MENU_MUSIC = "luke_bergs_waesto_take_off"
        private const val DEFAULT_STAGE_MUSIC = "orchestronika_motivation"
        private const val DEFAULT_MENU_MUSIC_VOLUME = 70
        const val SILENT_MUSIC_RESOURCE = "__sem_audio__"
    }
}
