package com.semstress.mobile.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StageCatalogJsonParserTest {

    @Test
    fun `parseia catalogo valido com fases herdando e sobrescrevendo a base`() {
        val json = """
            {
              "schemaVersion": 1,
              "menu": { "musicName": "menu_track", "musicVolumePercent": 60 },
              "base": {
                "rows": 6, "cols": 6, "pieceTypes": 6, "initialMoves": 20, "targetScore": 5000,
                "musicName": "stage_track", "musicVolumePercent": 70
              },
              "stages": [
                { "id": 1, "name": "Fase 1", "description": "Intro" },
                { "id": 2, "name": "Fase 2", "description": "Maior", "rows": 8, "targetScore": 9000 }
              ]
            }
        """.trimIndent()

        val catalog = StageCatalogJsonParser.parse(json)

        assertEquals("menu_track", catalog.menuMusicName)
        assertEquals(60, catalog.menuMusicVolumePercent)
        assertEquals(2, catalog.stages.size)

        val stage1 = catalog.stages[0]
        assertEquals(6, stage1.rows)
        assertEquals(5000, stage1.targetScore)
        assertEquals("stage_track", stage1.musicName)

        val stage2 = catalog.stages[1]
        assertEquals(8, stage2.rows)
        assertEquals(6, stage2.cols)
        assertEquals(9000, stage2.targetScore)
    }

    @Test
    fun `usa defaults da base quando campo do arquivo e omitido`() {
        val json = """{ "schemaVersion": 1, "stages": [ { "id": 1, "name": "Fase 1" } ] }"""

        val catalog = StageCatalogJsonParser.parse(json)

        val stage = catalog.stages.single()
        assertEquals(6, stage.rows)
        assertEquals(6, stage.cols)
        assertEquals(6, stage.pieceTypes)
        assertEquals(20, stage.initialMoves)
        assertEquals(5000, stage.targetScore)
        assertEquals("luke_bergs_waesto_take_off", catalog.menuMusicName)
    }

    @Test
    fun `retorna fase unica de fallback quando a lista de fases esta vazia`() {
        val json = """{ "schemaVersion": 1, "stages": [] }"""

        val catalog = StageCatalogJsonParser.parse(json)

        assertEquals(1, catalog.stages.size)
        assertEquals(1, catalog.stages.single().id)
    }

    @Test
    fun `musica desabilitada produz recurso silencioso e volume zero`() {
        val json = """
            { "schemaVersion": 1, "stages": [ { "id": 1, "name": "Fase 1", "musicEnabled": false } ] }
        """.trimIndent()

        val stage = StageCatalogJsonParser.parse(json).stages.single()

        assertEquals(StageCatalogJsonParser.SILENT_MUSIC_RESOURCE, stage.musicName)
        assertEquals(0, stage.musicVolumePercent)
    }

    @Test
    fun `rejeita schemaVersion nao suportada com mensagem clara`() {
        val json = """{ "schemaVersion": 99, "stages": [] }"""

        val exception = assertThrows(StageConfigParsingException::class.java) {
            StageCatalogJsonParser.parse(json)
        }

        assertTrue(exception.message!!.contains("schemaVersion"))
    }

    @Test
    fun `rejeita json malformado com mensagem clara`() {
        val exception = assertThrows(StageConfigParsingException::class.java) {
            StageCatalogJsonParser.parse("{ nao e json valido")
        }

        assertTrue(exception.message!!.contains("Invalid stages JSON"))
    }

    @Test
    fun `rejeita fase sem campos obrigatorios com mensagem clara`() {
        val json = """{ "schemaVersion": 1, "stages": [ { "name": "Fase sem id" } ] }"""

        assertThrows(StageConfigParsingException::class.java) {
            StageCatalogJsonParser.parse(json)
        }
    }

    @Test
    fun `rejeita tabuleiro invalido com mensagem que identifica a fase`() {
        val json = """
            { "schemaVersion": 1, "stages": [ { "id": 3, "name": "Fase 3", "rows": 0 } ] }
        """.trimIndent()

        val exception = assertThrows(StageConfigParsingException::class.java) {
            StageCatalogJsonParser.parse(json)
        }

        assertTrue(exception.message!!.contains("Stage 3"))
    }

    @Test
    fun `parseia collectObjective quando presente na fase`() {
        val json = """
            { "schemaVersion": 1, "stages": [
                { "id": 1, "name": "Fase 1", "pieceTypes": 5, "collectPieceType": 2, "collectCount": 15 }
            ] }
        """.trimIndent()

        val stage = StageCatalogJsonParser.parse(json).stages.single()

        val objective = requireNotNull(stage.collectObjective)
        assertEquals(2, objective.pieceType)
        assertEquals(15, objective.count)
    }

    @Test
    fun `nao cria collectObjective quando os campos nao estao presentes`() {
        val json = """{ "schemaVersion": 1, "stages": [ { "id": 1, "name": "Fase 1" } ] }"""

        val stage = StageCatalogJsonParser.parse(json).stages.single()

        assertEquals(null, stage.collectObjective)
    }

    @Test
    fun `rejeita collectPieceType fora do intervalo de tipos de peca`() {
        val json = """
            { "schemaVersion": 1, "stages": [
                { "id": 1, "name": "Fase 1", "pieceTypes": 5, "collectPieceType": 9, "collectCount": 10 }
            ] }
        """.trimIndent()

        val exception = assertThrows(StageConfigParsingException::class.java) {
            StageCatalogJsonParser.parse(json)
        }

        assertTrue(exception.message!!.contains("collectPieceType"))
    }

    @Test
    fun `ignora chaves desconhecidas no json para permitir evolucao do schema`() {
        val json = """
            { "schemaVersion": 1, "campoFuturo": true, "stages": [ { "id": 1, "name": "Fase 1", "campoNovo": 123 } ] }
        """.trimIndent()

        val catalog = StageCatalogJsonParser.parse(json)

        assertEquals(1, catalog.stages.size)
    }
}
