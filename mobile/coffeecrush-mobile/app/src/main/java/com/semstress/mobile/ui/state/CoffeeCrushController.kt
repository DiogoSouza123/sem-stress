package com.semstress.mobile.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.semstress.mobile.data.ProgressRepository
import com.semstress.mobile.domain.PlayerProgress
import com.semstress.mobile.domain.Position
import com.semstress.mobile.domain.StageCatalog
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.engine.Match3Board
import com.semstress.mobile.engine.Match3Engine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AppScreen {
    MENU,
    GAME
}

data class AppUiState(
    val screen: AppScreen = AppScreen.MENU,
    val stages: List<StageConfig> = emptyList(),
    val progress: PlayerProgress = PlayerProgress(),
    val selectedStageId: Int = 1,
    val menuMusicName: String = "luke_bergs_waesto_take_off",
    val menuMusicVolumePercent: Int = 70,
    val musicMuted: Boolean = false,
    val game: GameUiState? = null
)

data class GameUiState(
    val stageId: Int,
    val stageName: String,
    val stageDescription: String,
    val board: List<List<Int>>,
    val selected: Position? = null,
    val points: Int = 0,
    val moves: Int = 0,
    val target: Int = 0,
    val animating: Boolean = false,
    val highlightedMatches: Set<Position> = emptySet(),
    val explodingMatches: Set<Position> = emptySet(),
    val message: String? = null,
    val finished: Boolean = false,
    val won: Boolean = false
)

class CoffeeCrushController(
    private val stageCatalog: StageCatalog,
    private val progressRepository: ProgressRepository
) {
    private val stages: List<StageConfig> = stageCatalog.stages

    private data class GameSession(
        val stage: StageConfig,
        val board: Match3Board,
        val engine: Match3Engine,
        var selected: Position? = null,
        var points: Int = 0,
        var moves: Int = stage.initialMoves,
        var finished: Boolean = false,
        var won: Boolean = false,
        var animating: Boolean = false,
        var highlightedMatches: Set<Position> = emptySet(),
        var explodingMatches: Set<Position> = emptySet(),
        var message: String? = null
    )

    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var moveJob: Job? = null

    private var progress: PlayerProgress = progressRepository.load(stages.size)
    private var session: GameSession? = null

    var uiState by mutableStateOf(
        AppUiState(
            screen = AppScreen.MENU,
            stages = stages,
            progress = progress,
            selectedStageId = progress.currentStage.coerceIn(1, stages.size),
            menuMusicName = stageCatalog.menuMusicName,
            menuMusicVolumePercent = stageCatalog.menuMusicVolumePercent
        )
    )
        private set

    fun selectStage(stageId: Int) {
        if (!progress.isUnlocked(stageId)) {
            return
        }
        uiState = uiState.copy(selectedStageId = stageId)
    }

    fun startSelectedStage() {
        val stage = stages.firstOrNull { it.id == uiState.selectedStageId } ?: return
        startStage(stage)
    }

    fun replayCurrentStage() {
        val currentGame = uiState.game ?: return
        val stage = stages.firstOrNull { it.id == currentGame.stageId } ?: return
        startStage(stage)
    }

    fun backToMenu() {
        moveJob?.cancel()
        session = null
        uiState = uiState.copy(
            screen = AppScreen.MENU,
            progress = progress,
            selectedStageId = uiState.selectedStageId,
            game = null
        )
    }

    fun toggleMusic() {
        uiState = uiState.copy(musicMuted = !uiState.musicMuted)
    }

    fun onCellTap(row: Int, col: Int) {
        val currentSession = session ?: return
        if (currentSession.finished || currentSession.animating || moveJob?.isActive == true) {
            return
        }
        if (row !in 0 until currentSession.stage.rows || col !in 0 until currentSession.stage.cols) {
            return
        }

        val clicked = Position(row, col)
        val selected = currentSession.selected

        if (selected == null) {
            currentSession.selected = clicked
            currentSession.message = null
            syncGameState()
            return
        }

        if (selected == clicked) {
            currentSession.selected = null
            currentSession.message = null
            syncGameState()
            return
        }

        currentSession.selected = null
        syncGameState()
        launchMove(currentSession, selected, clicked)
    }

    fun onCellDragSwap(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val currentSession = session ?: return
        if (currentSession.finished || currentSession.animating || moveJob?.isActive == true) {
            return
        }
        if (fromRow !in 0 until currentSession.stage.rows || fromCol !in 0 until currentSession.stage.cols) {
            return
        }
        if (toRow !in 0 until currentSession.stage.rows || toCol !in 0 until currentSession.stage.cols) {
            return
        }

        val first = Position(fromRow, fromCol)
        val second = Position(toRow, toCol)
        if (first == second) {
            return
        }

        currentSession.selected = null
        syncGameState()
        launchMove(currentSession, first, second)
    }

    private fun launchMove(currentSession: GameSession, first: Position, second: Position) {
        if (moveJob?.isActive == true) {
            return
        }
        moveJob = controllerScope.launch {
            performMoveAnimated(currentSession, first, second)
            syncGameState()
        }
    }

    private suspend fun performMoveAnimated(currentSession: GameSession, first: Position, second: Position) {
        val outcome = currentSession.engine.tryMoveAnimated(currentSession.board, first, second)
        if (outcome.valid) {
            currentSession.animating = true
            currentSession.message = null
            syncGameState()

            for (round in outcome.rounds) {
                if (session !== currentSession) {
                    return
                }
                currentSession.board.overwrite(round.stateBeforeClear)
                currentSession.highlightedMatches = round.matchedPositions.toSet()
                currentSession.explodingMatches = emptySet()
                syncGameState()
                delay(MATCH_HIGHLIGHT_MS)

                currentSession.board.overwrite(round.stateAfterClear)
                currentSession.explodingMatches = round.matchedPositions.toSet()
                syncGameState()
                delay(EXPLOSION_MS)

                currentSession.highlightedMatches = emptySet()
                currentSession.explodingMatches = emptySet()
                if (round.fallFrames.isEmpty()) {
                    syncGameState()
                } else {
                    round.fallFrames.forEach { frame ->
                        currentSession.board.overwrite(frame)
                        syncGameState()
                        delay(FALL_FRAME_MS)
                    }
                }
            }

            currentSession.points += outcome.points
            currentSession.moves -= 1

            if (!currentSession.engine.hasAvailableMove(currentSession.board)) {
                currentSession.engine.shuffleWithoutMatches(currentSession.board)
                currentSession.message = "Sem movimentos disponiveis. Tabuleiro embaralhado."
            } else {
                currentSession.message = if (outcome.cascades > 1) {
                    "Combo x${outcome.cascades}!"
                } else {
                    null
                }
            }
        } else {
            if (currentSession.stage.consumeInvalidMove) {
                currentSession.moves -= 1
            }
            currentSession.message = "Movimento invalido."
        }

        currentSession.animating = false
        currentSession.highlightedMatches = emptySet()
        currentSession.explodingMatches = emptySet()
        finalizeRoundIfNeeded(currentSession)
    }

    private fun startStage(stage: StageConfig) {
        moveJob?.cancel()
        val board = Match3Board(stage.rows, stage.cols, stage.pieceTypes)
        val engine = Match3Engine(stage)
        board.fillRandom()
        engine.ensurePlayableBoard(board)
        engine.resolveBoard(board)
        engine.ensurePlayableBoard(board)

        val stageProgress = progress.copy(currentStage = stage.id)
        progress = stageProgress
        progressRepository.save(progress)

        session = GameSession(
            stage = stage,
            board = board,
            engine = engine,
            moves = stage.initialMoves
        )
        syncGameState(screen = AppScreen.GAME)
    }

    private fun finalizeRoundIfNeeded(currentSession: GameSession) {
        if (currentSession.points >= currentSession.stage.targetScore) {
            currentSession.finished = true
            currentSession.won = true
        } else if (currentSession.moves <= 0) {
            currentSession.finished = true
            currentSession.won = false
        }

        if (currentSession.finished) {
            progress = progress.registerResult(
                stageId = currentSession.stage.id,
                score = currentSession.points,
                won = currentSession.won,
                totalStages = stages.size
            )
            progressRepository.save(progress)
        }
    }

    private fun syncGameState(screen: AppScreen = uiState.screen) {
        val currentSession = session
        uiState = uiState.copy(
            screen = screen,
            progress = progress,
            game = if (currentSession == null) {
                null
            } else {
                GameUiState(
                    stageId = currentSession.stage.id,
                    stageName = currentSession.stage.name,
                    stageDescription = currentSession.stage.description,
                    board = currentSession.board.snapshot(),
                    selected = currentSession.selected,
                    points = currentSession.points,
                    moves = currentSession.moves,
                    target = currentSession.stage.targetScore,
                    animating = currentSession.animating,
                    highlightedMatches = currentSession.highlightedMatches,
                    explodingMatches = currentSession.explodingMatches,
                    message = currentSession.message,
                    finished = currentSession.finished,
                    won = currentSession.won
                )
            }
        )
    }

    private companion object {
        const val MATCH_HIGHLIGHT_MS = 140L
        const val EXPLOSION_MS = 220L
        const val FALL_FRAME_MS = 65L
    }
}
