package com.semstress.mobile.ui.state

import androidx.lifecycle.SavedStateHandle
import com.semstress.mobile.data.FakeProgressStore
import com.semstress.mobile.data.FakeStageCatalogSource
import com.semstress.mobile.domain.StageCatalog
import com.semstress.mobile.engine.stageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Port of the menu-related `CoffeeCrushControllerTest` (CQ-02) scenarios against [MenuViewModel],
 * the replacement introduced by RR-01. Since RR-02, the catalog/progress load on [ioDispatcher]
 * (here the shared [testDispatcher]), so [MenuUiState.isLoading] starts `true` and tests must
 * advance the scheduler before reading the loaded state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial comeca no menu com as fases do catalogo`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 5, cols = 5, pieceTypes = 5)
        val viewModel = newMenuViewModel(catalogOf(stage))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.activeGame)
        assertEquals(listOf(stage), viewModel.uiState.value.stages)
    }

    @Test
    fun `permanece em loading ate o catalogo e o progresso carregarem`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 5, cols = 5, pieceTypes = 5)
        val viewModel = newMenuViewModel(catalogOf(stage))

        assertTrue(viewModel.uiState.value.isLoading)

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `selectStage ignora fases bloqueadas`() = runTest(testDispatcher) {
        val stage1 = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val stage2 = stageConfig(id = 2, rows = 5, cols = 5, pieceTypes = 5)
        val viewModel = newMenuViewModel(
            catalogOf(stage1, stage2),
            progressRepository = FakeProgressStore(progressUnlockingUpTo(1))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(MenuAction.SelectStage(2))

        assertEquals(1, viewModel.uiState.value.selectedStageId)
    }

    @Test
    fun `playSelectedStage aponta a fase selecionada como jogo ativo`() = runTest(testDispatcher) {
        val stage = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val viewModel = newMenuViewModel(catalogOf(stage))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(MenuAction.PlaySelectedStage)

        val activeGame = viewModel.uiState.value.activeGame
        assertEquals(1, activeGame?.stageId)
    }

    @Test
    fun `playSelectedStage gera um novo playToken a cada chamada`() = runTest(testDispatcher) {
        val stage = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val viewModel = newMenuViewModel(catalogOf(stage))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(MenuAction.PlaySelectedStage)
        val firstToken = viewModel.uiState.value.activeGame?.playToken

        viewModel.onAction(MenuAction.ReturnToMenu)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onAction(MenuAction.PlaySelectedStage)
        val secondToken = viewModel.uiState.value.activeGame?.playToken

        assertTrue(secondToken != null && secondToken != firstToken)
    }

    @Test
    fun `returnToMenu limpa o jogo ativo e recarrega o progresso`() = runTest(testDispatcher) {
        val stage = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val store = FakeProgressStore()
        val viewModel = newMenuViewModel(catalogOf(stage), progressRepository = store)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onAction(MenuAction.PlaySelectedStage)

        store.save(store.saved.registerResult(stageId = 1, score = 500, won = true, totalStages = 1))
        viewModel.onAction(MenuAction.ReturnToMenu)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.activeGame)
        assertEquals(500, viewModel.uiState.value.progress.scoreFor(1))
    }

    @Test
    fun `uma nova instancia com o mesmo SavedStateHandle restaura o jogo ativo apos process death`() =
        runTest(testDispatcher) {
            val stage = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
            val handle = SavedStateHandle()
            val original = newMenuViewModel(catalogOf(stage), savedStateHandle = handle)
            testDispatcher.scheduler.advanceUntilIdle()
            original.onAction(MenuAction.PlaySelectedStage)
            val beforeDeath = original.uiState.value.activeGame

            val restored = newMenuViewModel(catalogOf(stage), savedStateHandle = handle)

            assertEquals(beforeDeath, restored.uiState.value.activeGame)
        }

    private fun newMenuViewModel(
        catalog: StageCatalog,
        progressRepository: FakeProgressStore = FakeProgressStore(),
        savedStateHandle: SavedStateHandle = SavedStateHandle()
    ): MenuViewModel = MenuViewModel(
        stageCatalogSource = FakeStageCatalogSource(catalog),
        progressRepository = progressRepository,
        savedStateHandle = savedStateHandle,
        ioDispatcher = testDispatcher
    )
}
