package com.semstress.mobile.ui.state

import com.semstress.mobile.data.FakeProgressStore
import com.semstress.mobile.data.FakeStageCatalogSource
import com.semstress.mobile.domain.StageCatalog
import com.semstress.mobile.engine.stageConfig
import com.semstress.mobile.ui.sprites.SpriteAtlasSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Port of the menu-related `CoffeeCrushControllerTest` (CQ-02) scenarios against [MenuViewModel],
 * the replacement introduced by RR-01. Since RR-02, the catalog/progress load on [ioDispatcher]
 * (here the shared [testDispatcher]), so [MenuUiState.isLoading] starts `true` and tests must
 * advance the scheduler before reading the loaded state. Since RR-04, which screen is showing is
 * owned by the Navigation Compose back stack, not this ViewModel, so there is no more
 * `activeGame`/`playToken`/`SavedStateHandle` here to test.
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
    fun `selectStage aceita fase desbloqueada`() = runTest(testDispatcher) {
        val stage1 = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val stage2 = stageConfig(id = 2, rows = 5, cols = 5, pieceTypes = 5)
        val viewModel = newMenuViewModel(
            catalogOf(stage1, stage2),
            progressRepository = FakeProgressStore(progressUnlockingUpTo(2))
        )
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(MenuAction.SelectStage(2))

        assertEquals(2, viewModel.uiState.value.selectedStageId)
    }

    @Test
    fun `returnToMenu recarrega o progresso`() = runTest(testDispatcher) {
        val stage = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val store = FakeProgressStore()
        val viewModel = newMenuViewModel(catalogOf(stage), progressRepository = store)
        testDispatcher.scheduler.advanceUntilIdle()

        store.save(store.saved.registerResult(stageId = 1, score = 500, won = true, totalStages = 1))
        viewModel.onAction(MenuAction.ReturnToMenu)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(500, viewModel.uiState.value.progress.scoreFor(1))
    }

    private fun newMenuViewModel(
        catalog: StageCatalog,
        progressRepository: FakeProgressStore = FakeProgressStore()
    ): MenuViewModel = MenuViewModel(
        stageCatalogSource = FakeStageCatalogSource(catalog),
        progressRepository = progressRepository,
        spriteAtlasSource = FakeSpriteAtlasSource,
        ioDispatcher = testDispatcher
    )

    private object FakeSpriteAtlasSource : SpriteAtlasSource {
        override suspend fun load() = null
    }
}
