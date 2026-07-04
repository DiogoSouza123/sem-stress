package com.semstress.mobile.ui.state

import androidx.lifecycle.SavedStateHandle
import com.semstress.mobile.data.FakeProgressStore
import com.semstress.mobile.domain.Position
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.engine.stageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
 * Port of the former `CoffeeCrushControllerTest` (CQ-02) scenarios against [GameViewModel], the
 * replacement introduced by RR-01. As before, boards are generated with an unseeded RNG, so tests
 * locate a valid/invalid move on the real generated board via [findMovePair].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

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
    fun `iniciar a fase preenche o tabuleiro e zera pontos e mensagens`() {
        val stage = stageConfig(rows = 5, cols = 5, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val viewModel = newGameViewModel(stage)

        val game = viewModel.uiState.value
        assertEquals(stage.id, game.stageId)
        assertEquals(stage.rows, game.board.size)
        assertEquals(stage.cols, game.board[0].size)
        assertEquals(stage.initialMoves, game.moves)
        assertEquals(0, game.points)
        assertFalse(game.finished)
    }

    @Test
    fun `primeiro tap seleciona a celula e o segundo tap na mesma desmarca`() {
        val viewModel = newGameViewModel(stageConfig(rows = 5, cols = 5, pieceTypes = 5))

        viewModel.onAction(GameAction.CellTapped(0, 0))
        assertEquals(Position(0, 0), viewModel.uiState.value.selected)

        viewModel.onAction(GameAction.CellTapped(0, 0))
        assertNull(viewModel.uiState.value.selected)
    }

    @Test
    fun `tap fora do tabuleiro e ignorado`() {
        val viewModel = newGameViewModel(stageConfig(rows = 5, cols = 5, pieceTypes = 5))

        viewModel.onAction(GameAction.CellTapped(-1, 0))

        assertNull(viewModel.uiState.value.selected)
    }

    @Test
    fun `dois taps adjacentes com match valido pontuam e consomem um movimento`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val viewModel = newGameViewModel(stage)
        val board = viewModel.uiState.value.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = true))

        viewModel.onAction(GameAction.CellTapped(first.row, first.col))
        viewModel.onAction(GameAction.CellTapped(second.row, second.col))
        testDispatcher.scheduler.advanceUntilIdle()

        val game = viewModel.uiState.value
        assertTrue(game.points > 0)
        assertEquals(stage.initialMoves - 1, game.moves)
        assertFalse(game.animating)
        assertNull(game.selected)
    }

    @Test
    fun `dragSwap com match valido tem o mesmo efeito que dois taps`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val viewModel = newGameViewModel(stage)
        val board = viewModel.uiState.value.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = true))

        viewModel.onAction(GameAction.CellDragSwapped(first.row, first.col, second.row, second.col))
        testDispatcher.scheduler.advanceUntilIdle()

        val game = viewModel.uiState.value
        assertTrue(game.points > 0)
        assertEquals(stage.initialMoves - 1, game.moves)
    }

    @Test
    fun `movimento sem match mostra mensagem de invalido sem consumir movimento`() = runTest(testDispatcher) {
        val stage = stageConfig(
            rows = 6,
            cols = 6,
            pieceTypes = 5,
            initialMoves = 20,
            targetScore = 999_999,
            consumeInvalidMove = false
        )
        val viewModel = newGameViewModel(stage)
        val board = viewModel.uiState.value.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = false))

        viewModel.onAction(GameAction.CellTapped(first.row, first.col))
        viewModel.onAction(GameAction.CellTapped(second.row, second.col))
        testDispatcher.scheduler.advanceUntilIdle()

        val game = viewModel.uiState.value
        assertEquals("Movimento invalido.", game.message)
        assertEquals(stage.initialMoves, game.moves)
        assertEquals(0, game.points)
    }

    @Test
    fun `movimento invalido consome um movimento quando a fase exige`() = runTest(testDispatcher) {
        val stage = stageConfig(
            rows = 6,
            cols = 6,
            pieceTypes = 5,
            initialMoves = 20,
            targetScore = 999_999,
            consumeInvalidMove = true
        )
        val viewModel = newGameViewModel(stage)
        val board = viewModel.uiState.value.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = false))

        viewModel.onAction(GameAction.CellTapped(first.row, first.col))
        viewModel.onAction(GameAction.CellTapped(second.row, second.col))
        testDispatcher.scheduler.advanceUntilIdle()

        val game = viewModel.uiState.value
        assertEquals("Movimento invalido.", game.message)
        assertEquals(stage.initialMoves - 1, game.moves)
    }

    @Test
    fun `atingir a meta finaliza como vitoria e desbloqueia a proxima fase`() = runTest(testDispatcher) {
        val stage = stageConfig(id = 1, rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 1)
        val store = FakeProgressStore()
        val viewModel = newGameViewModel(stage, totalStages = 2, progressRepository = store)
        val board = viewModel.uiState.value.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = true))

        viewModel.onAction(GameAction.CellTapped(first.row, first.col))
        viewModel.onAction(GameAction.CellTapped(second.row, second.col))
        testDispatcher.scheduler.advanceUntilIdle()

        val game = viewModel.uiState.value
        assertTrue(game.finished)
        assertTrue(game.won)
        assertEquals(2, store.saved.highestUnlockedStage)
        assertEquals(game.points, store.saved.scoreFor(1))
    }

    @Test
    fun `esgotar os movimentos sem atingir a meta finaliza a partida como derrota`() = runTest(testDispatcher) {
        val stage = stageConfig(
            id = 1,
            rows = 6,
            cols = 6,
            pieceTypes = 5,
            initialMoves = 1,
            targetScore = 999_999,
            consumeInvalidMove = true
        )
        val store = FakeProgressStore()
        val viewModel = newGameViewModel(stage, totalStages = 1, progressRepository = store)
        val board = viewModel.uiState.value.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = false))

        viewModel.onAction(GameAction.CellTapped(first.row, first.col))
        viewModel.onAction(GameAction.CellTapped(second.row, second.col))
        testDispatcher.scheduler.advanceUntilIdle()

        val game = viewModel.uiState.value
        assertTrue(game.finished)
        assertFalse(game.won)
        assertEquals(1, store.saved.highestUnlockedStage)
    }

    @Test
    fun `backToMenu cancela a corrida em andamento e emite o evento de retorno`() = runTest(testDispatcher) {
        val viewModel = newGameViewModel(stageConfig(rows = 5, cols = 5, pieceTypes = 5))

        var backToMenuRequested = false
        val collectJob = launch { viewModel.backToMenuRequests.collect { backToMenuRequested = true } }
        testDispatcher.scheduler.runCurrent()

        viewModel.onAction(GameAction.BackToMenu)
        testDispatcher.scheduler.runCurrent()

        assertTrue(backToMenuRequested)
        collectJob.cancel()
    }

    @Test
    fun `replay reinicia pontos e movimentos da mesma fase`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val viewModel = newGameViewModel(stage)
        val board = viewModel.uiState.value.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = true))
        viewModel.onAction(GameAction.CellTapped(first.row, first.col))
        viewModel.onAction(GameAction.CellTapped(second.row, second.col))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.points > 0)

        viewModel.onAction(GameAction.Replay)

        val game = viewModel.uiState.value
        assertEquals(0, game.points)
        assertEquals(stage.initialMoves, game.moves)
        assertFalse(game.finished)
    }

    @Test
    fun `quando o tabuleiro fica sem jogadas o motor reembaralha e avisa o jogador`() = runTest(testDispatcher) {
        // Muitos tipos de peca reduzem a chance de coincidencias entre vizinhos, o que aumenta a
        // chance de travar apos um movimento; como o board nao e seedavel aqui, cada tentativa cria
        // uma nova GameViewModel (novo board aleatorio) ate observar o reembaralhamento.
        val stage = stageConfig(
            rows = 5,
            cols = 5,
            pieceTypes = 8,
            minMatchSize = 3,
            initialMoves = 500,
            targetScore = 999_999
        )

        var shuffled = false
        repeat(300) {
            if (shuffled) return@repeat
            val viewModel = newGameViewModel(stage)
            val board = viewModel.uiState.value.board
            val pair = findMovePair(board, stage, wantValid = true) ?: return@repeat
            viewModel.onAction(GameAction.CellTapped(pair.first.row, pair.first.col))
            viewModel.onAction(GameAction.CellTapped(pair.second.row, pair.second.col))
            testDispatcher.scheduler.advanceUntilIdle()
            if (viewModel.uiState.value.message == "Sem movimentos disponiveis. Tabuleiro embaralhado.") {
                shuffled = true
            }
        }

        assertTrue(shuffled, "Nao observou o reembaralhamento em 300 tentativas")
    }

    @Test
    fun `movimento valido passa por animacao com match destacado antes de finalizar`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val viewModel = newGameViewModel(stage)
        val board = viewModel.uiState.value.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = true))

        viewModel.onAction(GameAction.CellTapped(first.row, first.col))
        viewModel.onAction(GameAction.CellTapped(second.row, second.col))

        // runCurrent() executa o inicio da corrotina do movimento ate a primeira delay()
        // (highlight do match), sem avancar o tempo virtual - flagra o estado intermediario.
        testDispatcher.scheduler.runCurrent()
        val duringHighlight = viewModel.uiState.value
        assertTrue(duringHighlight.animating)
        assertTrue(duringHighlight.highlightedMatches.isNotEmpty())

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = viewModel.uiState.value
        assertFalse(finalState.animating)
        assertTrue(finalState.highlightedMatches.isEmpty())
    }

    @Test
    fun `nova instancia com o mesmo SavedStateHandle restaura a sessao`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val handle = SavedStateHandle()
        val original = newGameViewModelWithHandle(stage, handle)
        val board = original.uiState.value.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = true))
        original.onAction(GameAction.CellTapped(first.row, first.col))
        original.onAction(GameAction.CellTapped(second.row, second.col))
        testDispatcher.scheduler.advanceUntilIdle()
        val beforeDeath = original.uiState.value
        assertTrue(beforeDeath.points > 0)

        // Simula a recriacao da ViewModel apos process death: mesmo handle (persistido pelo
        // sistema), instancia nova.
        val restored = newGameViewModelWithHandle(stage, handle)

        val restoredState = restored.uiState.value
        assertEquals(beforeDeath.board, restoredState.board)
        assertEquals(beforeDeath.points, restoredState.points)
        assertEquals(beforeDeath.moves, restoredState.moves)
        assertEquals(beforeDeath.selected, restoredState.selected)
    }

    private fun newGameViewModel(
        stage: StageConfig,
        totalStages: Int = 1,
        progressRepository: FakeProgressStore = FakeProgressStore()
    ): GameViewModel = newGameViewModelWithHandle(stage, SavedStateHandle(), totalStages, progressRepository)

    private fun newGameViewModelWithHandle(
        stage: StageConfig,
        savedStateHandle: SavedStateHandle,
        totalStages: Int = 1,
        progressRepository: FakeProgressStore = FakeProgressStore()
    ): GameViewModel = GameViewModel(
        stage = stage,
        totalStages = totalStages,
        progressRepository = progressRepository,
        savedStateHandle = savedStateHandle,
        ioDispatcher = testDispatcher
    )
}
