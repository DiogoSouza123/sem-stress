package com.semstress.mobile.ui.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semstress.mobile.data.ProgressStore
import com.semstress.mobile.di.IoDispatcher
import com.semstress.mobile.domain.Position
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.engine.AnimationRound
import com.semstress.mobile.engine.BoardEvent
import com.semstress.mobile.engine.Match3Board
import com.semstress.mobile.engine.Match3Engine
import com.semstress.mobile.engine.Match3EngineFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

sealed interface GameAction {
    data class CellTapped(val row: Int, val col: Int) : GameAction
    data class CellDragSwapped(val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int) : GameAction
    data object Replay : GameAction
    data object BackToMenu : GameAction
}

private const val MATCH_HIGHLIGHT_MS = 140L
private const val EXPLOSION_MS = 220L
private const val FALL_FRAME_MS = 65L

private data class GameSession(
    val board: Match3Board,
    val engine: Match3Engine,
    var selected: Position? = null,
    var points: Int = 0,
    var moves: Int,
    var finished: Boolean = false,
    var won: Boolean = false,
    var animating: Boolean = false,
    var highlightedMatches: Set<Position> = emptySet(),
    var explodingMatches: Set<Position> = emptySet(),
    var message: String? = null
)

private fun GameSession.toUiState(stage: StageConfig): GameUiState = GameUiState(
    stageId = stage.id,
    stageName = stage.name,
    stageDescription = stage.description,
    board = board.snapshot(),
    selected = selected,
    points = points,
    moves = moves,
    target = stage.targetScore,
    animating = animating,
    highlightedMatches = highlightedMatches,
    explodingMatches = explodingMatches,
    message = message,
    finished = finished,
    won = won
)

/**
 * Plays out a single [AnimationRound] (highlight -> explode -> fall) onto [GameSession.board],
 * calling [onChanged] after each frame. The engine already resolved this round on a separate
 * working copy (see [performMoveAnimated]); here the same round is replayed frame-by-frame onto
 * the board the UI is actually showing, starting from wherever the previous round (or the initial
 * swap) left it -- the same invariant the old snapshot-based version relied on.
 */
private suspend fun GameSession.applyRound(round: AnimationRound, onChanged: () -> Unit) {
    highlightedMatches = round.matchedPositions.toSet()
    explodingMatches = emptySet()
    onChanged()
    delay(MATCH_HIGHLIGHT_MS)

    round.matchedPositions.forEach { board.set(it.row, it.col, Match3Engine.EMPTY) }
    explodingMatches = round.matchedPositions.toSet()
    onChanged()
    delay(EXPLOSION_MS)

    highlightedMatches = emptySet()
    explodingMatches = emptySet()
    if (round.fallSteps.isEmpty()) {
        onChanged()
    } else {
        round.fallSteps.forEach { step ->
            step.forEach { event -> board.apply(event) }
            onChanged()
            delay(FALL_FRAME_MS)
        }
    }
}

private fun Match3Board.apply(event: BoardEvent) {
    when (event) {
        is BoardEvent.Moved -> {
            set(event.to.row, event.to.col, event.piece)
            set(event.from.row, event.from.col, Match3Engine.EMPTY)
        }
        is BoardEvent.Spawned -> set(event.position.row, event.position.col, event.piece)
    }
}

/**
 * Owns a single play session for [stage]. Replaces the game-related half of the former
 * `CoffeeCrushController`: state lives in [viewModelScope] (cancelled automatically on
 * `onCleared`) and the durable parts of the session are mirrored into [SavedStateHandle] so a
 * process death restores the same board/points/moves instead of starting over.
 */
@HiltViewModel(assistedFactory = GameViewModel.Factory::class)
class GameViewModel @AssistedInject constructor(
    @Assisted private val stage: StageConfig,
    @Assisted private val totalStages: Int,
    private val progressRepository: ProgressStore,
    private val engineFactory: Match3EngineFactory,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private var session: GameSession = restoreSession() ?: createFreshSession()

    private val _uiState = MutableStateFlow(session.toUiState(stage))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _backToMenuRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val backToMenuRequests: SharedFlow<Unit> = _backToMenuRequests.asSharedFlow()

    private var moveJob: Job? = null

    init {
        persistSession()
    }

    fun onAction(action: GameAction) {
        when (action) {
            is GameAction.CellTapped -> onCellTap(action.row, action.col)
            is GameAction.CellDragSwapped ->
                onCellDragSwap(action.fromRow, action.fromCol, action.toRow, action.toCol)
            GameAction.Replay -> restart()
            GameAction.BackToMenu -> {
                moveJob?.cancel()
                _backToMenuRequests.tryEmit(Unit)
            }
        }
    }

    private fun onCellTap(row: Int, col: Int) {
        val currentSession = session
        val blocked = currentSession.finished || currentSession.animating || moveJob?.isActive == true
        val outOfBounds = row !in 0 until stage.rows || col !in 0 until stage.cols
        if (blocked || outOfBounds) {
            return
        }

        val clicked = Position(row, col)
        val selected = currentSession.selected
        when {
            selected == null -> {
                currentSession.selected = clicked
                currentSession.message = null
                emit()
            }
            selected == clicked -> {
                currentSession.selected = null
                currentSession.message = null
                emit()
            }
            else -> {
                currentSession.selected = null
                emit()
                launchMove(currentSession, selected, clicked)
            }
        }
    }

    private fun onCellDragSwap(fromRow: Int, fromCol: Int, toRow: Int, toCol: Int) {
        val currentSession = session
        val blocked = currentSession.finished || currentSession.animating || moveJob?.isActive == true
        val firstOutOfBounds = fromRow !in 0 until stage.rows || fromCol !in 0 until stage.cols
        val secondOutOfBounds = toRow !in 0 until stage.rows || toCol !in 0 until stage.cols
        val first = Position(fromRow, fromCol)
        val second = Position(toRow, toCol)
        if (blocked || firstOutOfBounds || secondOutOfBounds) {
            return
        }
        if (first == second) {
            return
        }

        currentSession.selected = null
        emit()
        launchMove(currentSession, first, second)
    }

    private fun launchMove(currentSession: GameSession, first: Position, second: Position) {
        if (moveJob?.isActive == true) {
            return
        }
        moveJob = viewModelScope.launch {
            performMoveAnimated(currentSession, first, second)
            emit()
        }
    }

    private suspend fun performMoveAnimated(currentSession: GameSession, first: Position, second: Position) {
        val workingBoard = currentSession.board.copyOf()
        val outcome = currentSession.engine.tryMoveAnimated(workingBoard, first, second)
        if (outcome.valid) {
            currentSession.animating = true
            currentSession.message = null
            currentSession.board.swap(first, second)
            emit()

            for (round in outcome.rounds) {
                if (session !== currentSession) {
                    return
                }
                currentSession.applyRound(round) { emit() }
            }

            currentSession.points += outcome.points
            currentSession.moves -= 1

            if (!currentSession.engine.hasAvailableMove(currentSession.board)) {
                currentSession.engine.shuffleWithoutMatches(currentSession.board)
                currentSession.message = "Sem movimentos disponiveis. Tabuleiro embaralhado."
            } else {
                currentSession.message = if (outcome.cascades > 1) "Combo x${outcome.cascades}!" else null
            }
        } else {
            if (stage.consumeInvalidMove) {
                currentSession.moves -= 1
            }
            currentSession.message = INVALID_MOVE_MESSAGE
        }

        currentSession.animating = false
        currentSession.highlightedMatches = emptySet()
        currentSession.explodingMatches = emptySet()

        if (currentSession.points >= stage.targetScore) {
            currentSession.finished = true
            currentSession.won = true
        } else if (currentSession.moves <= 0) {
            currentSession.finished = true
            currentSession.won = false
        }
        if (currentSession.finished) {
            withContext(ioDispatcher) {
                val progress = progressRepository.load(totalStages).registerResult(
                    stageId = stage.id,
                    score = currentSession.points,
                    won = currentSession.won,
                    totalStages = totalStages
                )
                progressRepository.save(progress)
            }
        }
    }

    private fun restart() {
        moveJob?.cancel()
        session = createFreshSession()
        emit()
    }

    private fun createFreshSession(): GameSession {
        val board = Match3Board(stage.rows, stage.cols, stage.pieceTypes)
        val engine = engineFactory.create(stage)
        board.fillRandom()
        engine.ensurePlayableBoard(board)
        engine.resolveBoard(board)
        engine.ensurePlayableBoard(board)
        return GameSession(board = board, engine = engine, moves = stage.initialMoves)
    }

    private fun emit() {
        _uiState.value = session.toUiState(stage)
        persistSession()
    }

    private fun persistSession() {
        val currentSession = session
        val flatBoard = IntArray(stage.rows * stage.cols)
        val snapshot = currentSession.board.snapshot()
        var index = 0
        for (row in snapshot) {
            for (value in row) {
                flatBoard[index] = value
                index++
            }
        }
        savedStateHandle[KEY_BOARD] = flatBoard
        savedStateHandle[KEY_POINTS] = currentSession.points
        savedStateHandle[KEY_MOVES] = currentSession.moves
        savedStateHandle[KEY_FINISHED] = currentSession.finished
        savedStateHandle[KEY_WON] = currentSession.won
        savedStateHandle[KEY_MESSAGE] = currentSession.message
        savedStateHandle[KEY_SELECTED_ROW] = currentSession.selected?.row ?: NO_SELECTION
        savedStateHandle[KEY_SELECTED_COL] = currentSession.selected?.col ?: NO_SELECTION
    }

    private fun restoreSession(): GameSession? {
        val flatBoard = savedStateHandle.get<IntArray>(KEY_BOARD) ?: return null
        val board = Match3Board(stage.rows, stage.cols, stage.pieceTypes)
        val nested = (0 until stage.rows).map { row ->
            (0 until stage.cols).map { col -> flatBoard[row * stage.cols + col] }
        }
        board.overwrite(nested)

        val selectedRow = savedStateHandle.get<Int>(KEY_SELECTED_ROW) ?: NO_SELECTION
        val selectedCol = savedStateHandle.get<Int>(KEY_SELECTED_COL) ?: NO_SELECTION
        val selected = if (selectedRow == NO_SELECTION || selectedCol == NO_SELECTION) {
            null
        } else {
            Position(selectedRow, selectedCol)
        }

        return GameSession(
            board = board,
            engine = engineFactory.create(stage),
            selected = selected,
            points = savedStateHandle.get<Int>(KEY_POINTS) ?: 0,
            moves = savedStateHandle.get<Int>(KEY_MOVES) ?: stage.initialMoves,
            finished = savedStateHandle.get<Boolean>(KEY_FINISHED) ?: false,
            won = savedStateHandle.get<Boolean>(KEY_WON) ?: false,
            message = savedStateHandle.get<String>(KEY_MESSAGE)
        )
    }

    companion object {
        /** RR-22: exposed so [com.semstress.mobile.ui.screens.GameScreen] can trigger SFX/haptics on it. */
        const val INVALID_MOVE_MESSAGE = "Movimento invalido."

        private const val NO_SELECTION = -1

        private const val KEY_BOARD = "game_board"
        private const val KEY_POINTS = "game_points"
        private const val KEY_MOVES = "game_moves"
        private const val KEY_FINISHED = "game_finished"
        private const val KEY_WON = "game_won"
        private const val KEY_MESSAGE = "game_message"
        private const val KEY_SELECTED_ROW = "game_selected_row"
        private const val KEY_SELECTED_COL = "game_selected_col"
    }

    @AssistedFactory
    interface Factory {
        fun create(stage: StageConfig, totalStages: Int): GameViewModel
    }
}
