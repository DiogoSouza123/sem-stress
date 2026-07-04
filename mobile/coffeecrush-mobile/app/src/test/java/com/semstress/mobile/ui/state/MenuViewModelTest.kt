package com.semstress.mobile.ui.state

import androidx.lifecycle.SavedStateHandle
import com.semstress.mobile.data.FakeProgressStore
import com.semstress.mobile.domain.StageCatalog
import com.semstress.mobile.engine.stageConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Port of the menu-related `CoffeeCrushControllerTest` (CQ-02) scenarios against [MenuViewModel],
 * the replacement introduced by RR-01.
 */
class MenuViewModelTest {

    @Test
    fun `estado inicial comeca no menu com as fases do catalogo`() {
        val stage = stageConfig(rows = 5, cols = 5, pieceTypes = 5)
        val viewModel = newMenuViewModel(catalogOf(stage))

        assertNull(viewModel.uiState.value.activeGame)
        assertEquals(listOf(stage), viewModel.uiState.value.stages)
    }

    @Test
    fun `selectStage ignora fases bloqueadas`() {
        val stage1 = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val stage2 = stageConfig(id = 2, rows = 5, cols = 5, pieceTypes = 5)
        val viewModel = newMenuViewModel(
            catalogOf(stage1, stage2),
            progressRepository = FakeProgressStore(progressUnlockingUpTo(1))
        )

        viewModel.onAction(MenuAction.SelectStage(2))

        assertEquals(1, viewModel.uiState.value.selectedStageId)
    }

    @Test
    fun `playSelectedStage aponta a fase selecionada como jogo ativo`() {
        val stage = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val viewModel = newMenuViewModel(catalogOf(stage))

        viewModel.onAction(MenuAction.PlaySelectedStage)

        val activeGame = viewModel.uiState.value.activeGame
        assertEquals(1, activeGame?.stageId)
    }

    @Test
    fun `playSelectedStage gera um novo playToken a cada chamada`() {
        val stage = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val viewModel = newMenuViewModel(catalogOf(stage))

        viewModel.onAction(MenuAction.PlaySelectedStage)
        val firstToken = viewModel.uiState.value.activeGame?.playToken

        viewModel.onAction(MenuAction.ReturnToMenu)
        viewModel.onAction(MenuAction.PlaySelectedStage)
        val secondToken = viewModel.uiState.value.activeGame?.playToken

        assertTrue(secondToken != null && secondToken != firstToken)
    }

    @Test
    fun `returnToMenu limpa o jogo ativo e recarrega o progresso`() {
        val stage = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val store = FakeProgressStore()
        val viewModel = newMenuViewModel(catalogOf(stage), progressRepository = store)
        viewModel.onAction(MenuAction.PlaySelectedStage)

        store.save(store.saved.registerResult(stageId = 1, score = 500, won = true, totalStages = 1))
        viewModel.onAction(MenuAction.ReturnToMenu)

        assertNull(viewModel.uiState.value.activeGame)
        assertEquals(500, viewModel.uiState.value.progress.scoreFor(1))
    }

    @Test
    fun `toggleMusic alterna a flag de mudo`() {
        val viewModel = newMenuViewModel(catalogOf(stageConfig(rows = 5, cols = 5, pieceTypes = 5)))
        assertFalse(viewModel.uiState.value.musicMuted)

        viewModel.onAction(MenuAction.ToggleMusic)
        assertTrue(viewModel.uiState.value.musicMuted)

        viewModel.onAction(MenuAction.ToggleMusic)
        assertFalse(viewModel.uiState.value.musicMuted)
    }

    @Test
    fun `uma nova instancia com o mesmo SavedStateHandle restaura o jogo ativo apos process death`() {
        val stage = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val handle = SavedStateHandle()
        val original = MenuViewModel(catalogOf(stage), FakeProgressStore(), handle)
        original.onAction(MenuAction.PlaySelectedStage)
        val beforeDeath = original.uiState.value.activeGame

        val restored = MenuViewModel(catalogOf(stage), FakeProgressStore(), handle)

        assertEquals(beforeDeath, restored.uiState.value.activeGame)
    }

    private fun newMenuViewModel(
        catalog: StageCatalog,
        progressRepository: FakeProgressStore = FakeProgressStore()
    ): MenuViewModel = MenuViewModel(
        stageCatalog = catalog,
        progressRepository = progressRepository,
        savedStateHandle = SavedStateHandle()
    )
}
