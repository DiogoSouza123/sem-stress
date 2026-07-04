package com.semstress.mobile.ui.state

import com.semstress.mobile.data.FakeProgressStore
import com.semstress.mobile.domain.Position
import com.semstress.mobile.domain.StageConfig
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
 * Characterization tests for the current [CoffeeCrushController]: they document the behavior
 * that must be preserved when it is migrated to a ViewModel (RR-01). The controller builds its
 * board with an unseeded RNG, so tests locate a valid/invalid move on the real generated board
 * via [findMovePair] instead of relying on a fixed layout.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoffeeCrushControllerTest {

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
    fun `estado inicial comeca no menu com as fases do catalogo`() {
        val stage = stageConfig(rows = 5, cols = 5, pieceTypes = 5)
        val controller = CoffeeCrushController(catalogOf(stage), FakeProgressStore())

        assertEquals(AppScreen.MENU, controller.uiState.screen)
        assertEquals(listOf(stage), controller.uiState.stages)
        assertNull(controller.uiState.game)
    }

    @Test
    fun `selectStage ignora fases bloqueadas`() {
        val stage1 = stageConfig(id = 1, rows = 5, cols = 5, pieceTypes = 5)
        val stage2 = stageConfig(id = 2, rows = 5, cols = 5, pieceTypes = 5)
        val controller = CoffeeCrushController(
            catalogOf(stage1, stage2),
            FakeProgressStore(progressUnlockingUpTo(1))
        )

        controller.selectStage(2)

        assertEquals(1, controller.uiState.selectedStageId)
    }

    @Test
    fun `startSelectedStage inicia a fase e muda para a tela de jogo`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 5, cols = 5, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val controller = CoffeeCrushController(catalogOf(stage), FakeProgressStore())

        controller.startSelectedStage()
        testDispatcher.scheduler.advanceUntilIdle()

        val game = controller.uiState.game
        assertEquals(AppScreen.GAME, controller.uiState.screen)
        requireNotNull(game)
        assertEquals(stage.id, game.stageId)
        assertEquals(stage.rows, game.board.size)
        assertEquals(stage.cols, game.board[0].size)
        assertEquals(stage.initialMoves, game.moves)
        assertEquals(0, game.points)
        assertFalse(game.finished)
    }

    @Test
    fun `primeiro tap seleciona a celula e o segundo tap na mesma desmarca`() = runTest(testDispatcher) {
        val controller = startedController(rows = 5, cols = 5)

        controller.onCellTap(0, 0)
        assertEquals(Position(0, 0), controller.uiState.game?.selected)

        controller.onCellTap(0, 0)
        assertNull(controller.uiState.game?.selected)
    }

    @Test
    fun `tap fora do tabuleiro e ignorado`() = runTest(testDispatcher) {
        val controller = startedController(rows = 5, cols = 5)

        controller.onCellTap(-1, 0)

        assertNull(controller.uiState.game?.selected)
    }

    @Test
    fun `dois taps adjacentes com match valido pontuam e consomem um movimento`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val controller = startedController(stage)
        val board = controller.uiState.game!!.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = true))

        controller.onCellTap(first.row, first.col)
        controller.onCellTap(second.row, second.col)
        testDispatcher.scheduler.advanceUntilIdle()

        val game = requireNotNull(controller.uiState.game)
        assertTrue(game.points > 0)
        assertEquals(stage.initialMoves - 1, game.moves)
        assertFalse(game.animating)
        assertNull(game.selected)
    }

    @Test
    fun `dragSwap com match valido tem o mesmo efeito que dois taps`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val controller = startedController(stage)
        val board = controller.uiState.game!!.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = true))

        controller.onCellDragSwap(first.row, first.col, second.row, second.col)
        testDispatcher.scheduler.advanceUntilIdle()

        val game = requireNotNull(controller.uiState.game)
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
        val controller = startedController(stage)
        val board = controller.uiState.game!!.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = false))

        controller.onCellTap(first.row, first.col)
        controller.onCellTap(second.row, second.col)
        testDispatcher.scheduler.advanceUntilIdle()

        val game = requireNotNull(controller.uiState.game)
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
        val controller = startedController(stage)
        val board = controller.uiState.game!!.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = false))

        controller.onCellTap(first.row, first.col)
        controller.onCellTap(second.row, second.col)
        testDispatcher.scheduler.advanceUntilIdle()

        val game = requireNotNull(controller.uiState.game)
        assertEquals("Movimento invalido.", game.message)
        assertEquals(stage.initialMoves - 1, game.moves)
    }

    @Test
    fun `atingir a meta finaliza como vitoria e desbloqueia a proxima fase`() = runTest(testDispatcher) {
        val stage1 = stageConfig(id = 1, rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 1)
        val stage2 = stageConfig(id = 2, rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val store = FakeProgressStore()
        val controller = CoffeeCrushController(catalogOf(stage1, stage2), store)
        controller.startSelectedStage()
        testDispatcher.scheduler.advanceUntilIdle()

        val board = controller.uiState.game!!.board
        val (first, second) = requireNotNull(findMovePair(board, stage1, wantValid = true))
        controller.onCellTap(first.row, first.col)
        controller.onCellTap(second.row, second.col)
        testDispatcher.scheduler.advanceUntilIdle()

        val game = requireNotNull(controller.uiState.game)
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
        val controller = CoffeeCrushController(catalogOf(stage), store)
        controller.startSelectedStage()
        testDispatcher.scheduler.advanceUntilIdle()

        val board = controller.uiState.game!!.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = false))
        controller.onCellTap(first.row, first.col)
        controller.onCellTap(second.row, second.col)
        testDispatcher.scheduler.advanceUntilIdle()

        val game = requireNotNull(controller.uiState.game)
        assertTrue(game.finished)
        assertFalse(game.won)
        assertEquals(1, store.saved.highestUnlockedStage)
    }

    @Test
    fun `backToMenu encerra a sessao e limpa o estado de jogo`() = runTest(testDispatcher) {
        val controller = startedController(rows = 5, cols = 5)

        controller.backToMenu()

        assertEquals(AppScreen.MENU, controller.uiState.screen)
        assertNull(controller.uiState.game)
    }

    @Test
    fun `replayCurrentStage reinicia pontos e movimentos da mesma fase`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val controller = startedController(stage)
        val board = controller.uiState.game!!.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = true))
        controller.onCellTap(first.row, first.col)
        controller.onCellTap(second.row, second.col)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(controller.uiState.game!!.points > 0)

        controller.replayCurrentStage()
        testDispatcher.scheduler.advanceUntilIdle()

        val game = requireNotNull(controller.uiState.game)
        assertEquals(0, game.points)
        assertEquals(stage.initialMoves, game.moves)
        assertFalse(game.finished)
    }

    @Test
    fun `toggleMusic alterna a flag de mudo`() {
        val controller = CoffeeCrushController(
            catalogOf(stageConfig(rows = 5, cols = 5, pieceTypes = 5)),
            FakeProgressStore()
        )
        assertFalse(controller.uiState.musicMuted)

        controller.toggleMusic()
        assertTrue(controller.uiState.musicMuted)

        controller.toggleMusic()
        assertFalse(controller.uiState.musicMuted)
    }

    @Test
    fun `quando o tabuleiro fica sem jogadas o motor reembaralha e avisa o jogador`() = runTest(testDispatcher) {
        // Muitos tipos de peca reduzem a chance de coincidencias entre vizinhos, o que aumenta
        // a chance de travar apos um movimento; como o controller nao expoe seed, cada tentativa
        // recomeca a fase (novo board aleatorio) ate observar o reembaralhamento pelo menos uma vez.
        val stage = stageConfig(
            rows = 5,
            cols = 5,
            pieceTypes = 8,
            minMatchSize = 3,
            initialMoves = 500,
            targetScore = 999_999
        )
        val store = FakeProgressStore()
        val controller = CoffeeCrushController(catalogOf(stage), store)
        controller.startSelectedStage()
        testDispatcher.scheduler.advanceUntilIdle()

        var shuffled = false
        repeat(300) {
            if (shuffled) return@repeat
            controller.replayCurrentStage()
            testDispatcher.scheduler.advanceUntilIdle()

            val game = requireNotNull(controller.uiState.game)
            val pair = findMovePair(game.board, stage, wantValid = true) ?: return@repeat
            controller.onCellTap(pair.first.row, pair.first.col)
            controller.onCellTap(pair.second.row, pair.second.col)
            testDispatcher.scheduler.advanceUntilIdle()
            if (controller.uiState.game?.message == "Sem movimentos disponiveis. Tabuleiro embaralhado.") {
                shuffled = true
            }
        }

        assertTrue(shuffled, "Nao observou o reembaralhamento em 300 tentativas")
    }

    @Test
    fun `movimento valido passa por animacao com match destacado antes de finalizar`() = runTest(testDispatcher) {
        val stage = stageConfig(rows = 6, cols = 6, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        val controller = startedController(stage)
        val board = controller.uiState.game!!.board
        val (first, second) = requireNotNull(findMovePair(board, stage, wantValid = true))

        controller.onCellTap(first.row, first.col)
        controller.onCellTap(second.row, second.col)

        // runCurrent() executa o inicio da corrotina do movimento ate a primeira delay()
        // (highlight do match), sem avancar o tempo virtual - flagra o estado intermediario.
        testDispatcher.scheduler.runCurrent()
        val duringHighlight = requireNotNull(controller.uiState.game)
        assertTrue(duringHighlight.animating)
        assertTrue(duringHighlight.highlightedMatches.isNotEmpty())

        testDispatcher.scheduler.advanceUntilIdle()
        val finalState = requireNotNull(controller.uiState.game)
        assertFalse(finalState.animating)
        assertTrue(finalState.highlightedMatches.isEmpty())
    }

    private fun startedController(rows: Int, cols: Int): CoffeeCrushController {
        val stage = stageConfig(rows = rows, cols = cols, pieceTypes = 5, initialMoves = 20, targetScore = 999_999)
        return startedController(stage)
    }

    private fun startedController(stage: StageConfig): CoffeeCrushController {
        val controller = CoffeeCrushController(catalogOf(stage), FakeProgressStore())
        controller.startSelectedStage()
        testDispatcher.scheduler.advanceUntilIdle()
        return controller
    }
}
