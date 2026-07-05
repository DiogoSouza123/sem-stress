package com.semstress.mobile.data

import com.semstress.mobile.domain.CollectObjective
import com.semstress.mobile.domain.StageCatalog
import com.semstress.mobile.domain.StageConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class StageConfigParsingException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
data class StageCatalogFileDto(
    val schemaVersion: Int,
    val menu: MenuAudioDto = MenuAudioDto(),
    val base: StageDefaultsDto = StageDefaultsDto(),
    val stages: List<StageDto> = emptyList()
)

@Serializable
data class MenuAudioDto(
    val musicName: String = "luke_bergs_waesto_take_off",
    val musicVolumePercent: Int = 70
)

@Serializable
data class StageDefaultsDto(
    val rows: Int = 6,
    val cols: Int = 6,
    val pieceTypes: Int = 6,
    val minMatchSize: Int = 3,
    val scoreMatch3: Int = 500,
    val scoreMatch4: Int = 1000,
    val scoreMatch5Plus: Int = 1500,
    val cascadeMultiplier: Int = 1,
    val scoreCascade: Boolean = true,
    val initialMoves: Int = 20,
    val targetScore: Int = 5000,
    val onlyAdjacentSwap: Boolean = true,
    val consumeInvalidMove: Boolean = false,
    val backgroundName: String = "coffee_bg",
    val musicEnabled: Boolean = true,
    val musicName: String = "orchestronika_motivation",
    val musicVolumePercent: Int = 70
)

@Serializable
data class StageDto(
    val id: Int,
    val name: String,
    val description: String = "",
    val rows: Int? = null,
    val cols: Int? = null,
    val pieceTypes: Int? = null,
    val minMatchSize: Int? = null,
    val scoreMatch3: Int? = null,
    val scoreMatch4: Int? = null,
    val scoreMatch5Plus: Int? = null,
    val cascadeMultiplier: Int? = null,
    val scoreCascade: Boolean? = null,
    val initialMoves: Int? = null,
    val targetScore: Int? = null,
    val onlyAdjacentSwap: Boolean? = null,
    val consumeInvalidMove: Boolean? = null,
    val backgroundName: String? = null,
    val musicEnabled: Boolean? = null,
    val musicName: String? = null,
    val musicVolumePercent: Int? = null,
    val collectPieceType: Int? = null,
    val collectCount: Int? = null,
    val region: String? = null
)

/**
 * Pure parser (no Android dependency) for the RR-16 stage catalog JSON format. Kept free of
 * `Context`/assets so parsing rules and validation messages can be unit tested directly, with
 * [StageRepository] only responsible for resolving which file (asset or on-device override) to
 * feed in.
 */
object StageCatalogJsonParser {
    private const val SUPPORTED_SCHEMA_VERSION = 1
    private const val MIN_MATCH_SIZE_FLOOR = 3
    private const val MAX_MUSIC_VOLUME_PERCENT = 100
    const val SILENT_MUSIC_RESOURCE = "__sem_audio__"

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): StageCatalog {
        val file = decodeOrThrow(text)

        if (file.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            throw StageConfigParsingException(
                "schemaVersion ${file.schemaVersion} not supported (expected $SUPPORTED_SCHEMA_VERSION)."
            )
        }

        val stages = if (file.stages.isEmpty()) {
            listOf(fallbackStage(file.base))
        } else {
            file.stages.map { dto -> toStageConfig(dto, file.base).also(::validateStage) }
        }

        return StageCatalog(
            stages = stages,
            menuMusicName = file.menu.musicName,
            menuMusicVolumePercent = file.menu.musicVolumePercent
        )
    }

    private fun decodeOrThrow(text: String): StageCatalogFileDto = try {
        json.decodeFromString(StageCatalogFileDto.serializer(), text)
    } catch (exception: IllegalArgumentException) {
        throw StageConfigParsingException("Invalid stages JSON: ${exception.message}", exception)
    }

    private fun fallbackStage(base: StageDefaultsDto): StageConfig = StageConfig(
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
        musicName = if (base.musicEnabled) base.musicName else SILENT_MUSIC_RESOURCE,
        musicVolumePercent = if (base.musicEnabled) base.musicVolumePercent else 0
    )

    private fun <T> orDefault(value: T?, default: T): T = value ?: default

    private fun toStageConfig(dto: StageDto, base: StageDefaultsDto): StageConfig {
        val music = resolveMusic(dto, base)
        return StageConfig(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            rows = orDefault(dto.rows, base.rows),
            cols = orDefault(dto.cols, base.cols),
            pieceTypes = orDefault(dto.pieceTypes, base.pieceTypes),
            minMatchSize = orDefault(dto.minMatchSize, base.minMatchSize),
            scoreMatch3 = orDefault(dto.scoreMatch3, base.scoreMatch3),
            scoreMatch4 = orDefault(dto.scoreMatch4, base.scoreMatch4),
            scoreMatch5Plus = orDefault(dto.scoreMatch5Plus, base.scoreMatch5Plus),
            cascadeMultiplier = orDefault(dto.cascadeMultiplier, base.cascadeMultiplier),
            scoreCascade = orDefault(dto.scoreCascade, base.scoreCascade),
            initialMoves = orDefault(dto.initialMoves, base.initialMoves),
            targetScore = orDefault(dto.targetScore, base.targetScore),
            consumeInvalidMove = orDefault(dto.consumeInvalidMove, base.consumeInvalidMove),
            onlyAdjacentSwap = orDefault(dto.onlyAdjacentSwap, base.onlyAdjacentSwap),
            backgroundName = orDefault(dto.backgroundName, base.backgroundName),
            musicName = music.first,
            musicVolumePercent = music.second,
            collectObjective = collectObjectiveFrom(dto),
            region = dto.region
        )
    }

    /** GP-02: `collect` is stage-specific only (a global default piece type wouldn't make sense). */
    private fun collectObjectiveFrom(dto: StageDto): CollectObjective? {
        val pieceType = dto.collectPieceType
        val count = dto.collectCount
        return if (pieceType != null && count != null) CollectObjective(pieceType, count) else null
    }

    private fun resolveMusic(dto: StageDto, base: StageDefaultsDto): Pair<String, Int> {
        val musicEnabled = orDefault(dto.musicEnabled, base.musicEnabled)
        if (!musicEnabled) {
            return SILENT_MUSIC_RESOURCE to 0
        }
        return orDefault(dto.musicName, base.musicName) to orDefault(dto.musicVolumePercent, base.musicVolumePercent)
    }

    private fun validateStage(stage: StageConfig) {
        requireStage(stage.rows > 0, stage.id) { "rows must be greater than zero" }
        requireStage(stage.cols > 0, stage.id) { "cols must be greater than zero" }
        requireStage(stage.pieceTypes > 0, stage.id) { "pieceTypes must be greater than zero" }
        requireStage(stage.minMatchSize >= MIN_MATCH_SIZE_FLOOR, stage.id) {
            "minMatchSize must be >= $MIN_MATCH_SIZE_FLOOR"
        }
        requireStage(stage.rows >= stage.minMatchSize || stage.cols >= stage.minMatchSize, stage.id) {
            "at least one board dimension must be >= minMatchSize"
        }
        requireStage(stage.initialMoves > 0, stage.id) { "initialMoves must be greater than zero" }
        requireStage(stage.targetScore >= 0, stage.id) { "targetScore cannot be negative" }
        requireStage(stage.musicVolumePercent in 0..MAX_MUSIC_VOLUME_PERCENT, stage.id) {
            "musicVolumePercent must be between 0 and $MAX_MUSIC_VOLUME_PERCENT"
        }
        stage.collectObjective?.let { objective ->
            requireStage(objective.pieceType in 0 until stage.pieceTypes, stage.id) {
                "collectPieceType must be a valid piece type (0..${stage.pieceTypes - 1})"
            }
            requireStage(objective.count > 0, stage.id) { "collectCount must be greater than zero" }
        }
    }

    private inline fun requireStage(condition: Boolean, stageId: Int, lazyMessage: () -> String) {
        if (!condition) {
            throw StageConfigParsingException("Stage $stageId invalid: ${lazyMessage()}")
        }
    }
}
