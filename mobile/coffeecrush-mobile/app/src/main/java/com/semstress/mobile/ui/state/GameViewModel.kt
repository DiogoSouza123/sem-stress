package com.semstress.mobile.ui.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semstress.mobile.BuildConfig
import com.semstress.mobile.data.ProgressStore
import com.semstress.mobile.di.IoDispatcher
import com.semstress.mobile.domain.DAILY_CHALLENGE_STAGE_ID
import com.semstress.mobile.domain.Position
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.domain.calculateStars
import com.semstress.mobile.engine.AnimatedMoveOutcome
import com.semstress.mobile.engine.AnimationRound
import com.semstress.mobile.engine.BoardEvent
import com.semstress.mobile.engine.Match3Board
import com.semstress.mobile.engine.Match3Engine
import com.semstress.mobile.engine.Match3EngineFactory
import com.semstress.mobile.engine.SpecialActivationOutcome
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
    val won: Boolean = false,
    val starsEarned: Int = 0,
    val invalidSwap: Pair<Position, Position>? = null,
    val invalidMoveNonce: Int = 0,
    val initialMoves: Int = 0,
    val collectPieceType: Int? = null,
    val collectTarget: Int = 0,
    val collectProgress: Int = 0,
    val hintMove: Pair<Position, Position>? = null,
    val isZenMode: Boolean = false,
    val aroma: Int = 0,
    val aromaCapacity: Int = AROMA_CAPACITY
)

sealed interface GameAction {
    data class CellTapped(val row: Int, val col: Int) : GameAction
    data class CellDragSwapped(val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int) : GameAction
    data object Replay : GameAction
    data object BackToMenu : GameAction

    /** GP-04: consumes a full Aroma meter to reveal a valid move ("Degustacao") for a few seconds. */
    data object ActivateBaristaSkill : GameAction

    /** CQ-03: debug-panel-only actions; the panel that emits these only exists in debug builds. */
    data class DebugAddMoves(val amount: Int) : GameAction
    data object DebugForceWin : GameAction
    data class DebugReshuffleWithSeed(val seed: Long) : GameAction
}

private const val MATCH_HIGHLIGHT_MS = 140L
private const val EXPLOSION_MS = 220L
private const val FALL_FRAME_MS = 65L
private const val HINT_DELAY_MS = 8000L

/** GP-04: how much Aroma each matched piece releases, and how full the meter needs to be to act. */
private const val AROMA_PER_MATCHED_PIECE = 1
const val AROMA_CAPACITY = 30
private const val BARISTA_SKILL_REVEAL_MS = 5000L

private data class GameSession(
    val board: Match3Board,
    val engine: Match3Engine,
    var selected: Position? = null,
    var points: Int = 0,
    var moves: Int,
    var finished: Boolean = false,
    var won: Boolean = false,
    var starsEarned: Int = 0,
    var animating: Boolean = false,
    var highlightedMatches: Set<Position> = emptySet(),
    var explodingMatches: Set<Position> = emptySet(),
    var message: String? = null,
    var invalidSwap: Pair<Position, Position>? = null,
    var invalidMoveNonce: Int = 0,
    var collectedCount: Int = 0,
    var hintMove: Pair<Position, Position>? = null,
    var aroma: Int = 0
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
    won = won,
    starsEarned = starsEarned,
    invalidSwap = invalidSwap,
    invalidMoveNonce = invalidMoveNonce,
    initialMoves = stage.initialMoves,
    collectPieceType = stage.collectObjective?.pieceType,
    collectTarget = stage.collectObjective?.count ?: 0,
    collectProgress = collectedCount,
    hintMove = hintMove,
    isZenMode = stage.isZenMode,
    aroma = aroma
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
    round.specialSpawns.forEach { spawn -> board.set(spawn.position.row, spawn.position.col, spawn.pieceValue) }
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
        is BoardEvent.Reshuffled -> set(event.position.row, event.position.col, event.piece)
    }
}

/**
 * GP-01: plays out a [SpecialActivationOutcome] (highlight -> explode -> fall -> score), shared by
 * the player-tapped path ([GameViewModel.performSpecialActivation]) and Xicara Vazia's automatic
 * countdown expiry (see the `tickEmptyCups` call in [GameViewModel.applyValidMove]).
 */
private suspend fun GameSession.animateSpecialOutcome(
    outcome: SpecialActivationOutcome,
    stage: StageConfig,
    onChanged: () -> Unit
) {
    val affected = (outcome.affectedPieces.map { it.first } + listOfNotNull(outcome.triggerPosition)).toSet()
    animating = true
    message = null
    highlightedMatches = affected
    onChanged()
    delay(MATCH_HIGHLIGHT_MS)

    affected.forEach { pos -> board.set(pos.row, pos.col, Match3Engine.EMPTY) }
    explodingMatches = affected
    onChanged()
    delay(EXPLOSION_MS)

    highlightedMatches = emptySet()
    explodingMatches = emptySet()
    outcome.fallSteps.forEach { step ->
        step.forEach { event -> board.apply(event) }
        onChanged()
        delay(FALL_FRAME_MS)
    }

    // Alignments created by the activation resolve like any regular move's cascades, with the
    // same aroma/objective crediting (read before applyRound clears the matched cells).
    for (round in outcome.cascadeRounds) {
        stage.collectObjective?.let { objective ->
            collectedCount += round.matchedPositions.count { position ->
                board.get(position.row, position.col) == objective.pieceType
            }
        }
        aroma = (aroma + round.matchedPositions.size * AROMA_PER_MATCHED_PIECE).coerceAtMost(AROMA_CAPACITY)
        applyRound(round) { onChanged() }
    }

    points += outcome.points + outcome.cascadePoints
    stage.collectObjective?.let { objective ->
        collectedCount += outcome.affectedPieces.count { (_, value) -> value == objective.pieceType }
    }
    aroma = (aroma + outcome.affectedPieces.size * AROMA_PER_MATCHED_PIECE).coerceAtMost(AROMA_CAPACITY)
}

/**
 * Owns a single play session for [stage]. Replaces the game-related half of the former
 * `CoffeeCrushController`: state lives in [viewModelScope] (cancelled automatically on
 * `onCleared`) and the durable parts of the session are mirrored into [SavedStateHandle] so a
 * process death restores the same board/points/moves instead of starting over.
 */
@HiltViewModel(assistedFactory = GameViewModel.Factory::class)
class GameViewModel @AssistedInject constructor(
    @Assisted spec: GameSessionSpec,
    private val progressRepository: ProgressStore,
    private val engineFactory: Match3EngineFactory,
    private val savedStateHandle: SavedStateHandle,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val stage: StageConfig = spec.stage
    private val totalStages: Int = spec.totalStages
    private val seed: Long? = spec.seed

    private var session: GameSession = restoreSession(savedStateHandle, stage, engineFactory)
        ?: createFreshSession(stage, engineFactory, seed)

    private val _uiState = MutableStateFlow(session.toUiState(stage))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _backToMenuRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val backToMenuRequests: SharedFlow<Unit> = _backToMenuRequests.asSharedFlow()

    private var moveJob: Job? = null
    private var hintJob: Job? = null

    init {
        emit()
        scheduleHint(session)
    }

    fun onAction(action: GameAction) {
        when (action) {
            is GameAction.CellTapped -> onCellTap(action.row, action.col)
            is GameAction.CellDragSwapped ->
                onCellDragSwap(action.fromRow, action.fromCol, action.toRow, action.toCol)
            GameAction.Replay -> {
                moveJob?.cancel()
                hintJob?.cancel()
                session = createFreshSession(stage, engineFactory, seed)
                emit()
                scheduleHint(session)
            }
            GameAction.ActivateBaristaSkill -> {
                val currentSession = session
                if (currentSession.aroma >= AROMA_CAPACITY && !currentSession.finished) {
                    currentSession.aroma = 0
                    currentSession.hintMove = currentSession.engine.findAvailableMove(currentSession.board)
                    hintJob?.cancel()
                    hintJob = viewModelScope.launch {
                        delay(BARISTA_SKILL_REVEAL_MS)
                        if (session === currentSession) {
                            currentSession.hintMove = null
                            emit()
                        }
                    }
                    emit()
                }
            }
            GameAction.BackToMenu -> {
                moveJob?.cancel()
                hintJob?.cancel()
                _backToMenuRequests.tryEmit(Unit)
            }
            is GameAction.DebugAddMoves, GameAction.DebugForceWin, is GameAction.DebugReshuffleWithSeed ->
                if (BuildConfig.DEBUG) {
                    handleDebugAction(action)
                }
        }
    }

    /** CQ-03: debug-panel-only actions, split out to keep [onAction]'s complexity in check. */
    private fun handleDebugAction(action: GameAction) {
        when (action) {
            is GameAction.DebugAddMoves -> {
                session.moves += action.amount
                emit()
            }
            GameAction.DebugForceWin -> if (moveJob?.isActive != true) {
                val currentSession = session
                currentSession.points = stage.targetScore
                currentSession.finished = true
                currentSession.won = true
                currentSession.starsEarned = calculateStars(
                    won = true,
                    score = currentSession.points,
                    targetScore = stage.targetScore,
                    movesRemaining = currentSession.moves,
                    initialMoves = stage.initialMoves
                )
                emit()
                viewModelScope.launch {
                    val spec = GameSessionSpec(stage, totalStages, seed)
                    persistProgressIfFinished(currentSession, spec, progressRepository, ioDispatcher)
                }
            }
            is GameAction.DebugReshuffleWithSeed -> {
                moveJob?.cancel()
                hintJob?.cancel()
                session = createFreshSession(stage, engineFactory, action.seed)
                emit()
                scheduleHint(session)
            }
            else -> Unit
        }
    }

    private fun onCellTap(row: Int, col: Int) {
        val currentSession = session
        val blocked = currentSession.finished || currentSession.animating || moveJob?.isActive == true
        val outOfBounds = row !in 0 until stage.rows || col !in 0 until stage.cols
        if (blocked || outOfBounds) {
            return
        }
        scheduleHint(currentSession)

        val clicked = Position(row, col)
        if (Match3Engine.isSpecialPiece(currentSession.board.get(row, col))) {
            currentSession.selected = null
            emit()
            launchAnimatedAction { performSpecialActivation(currentSession, clicked) }
            return
        }

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
                launchAnimatedAction { performMoveAnimated(currentSession, selected, clicked) }
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
        scheduleHint(currentSession)

        currentSession.selected = null
        emit()
        launchAnimatedAction { performMoveAnimated(currentSession, first, second) }
    }

    /** GP-08: (re)starts the ~8s inactivity countdown and clears any hint already shown, on every player action. */
    private fun scheduleHint(currentSession: GameSession) {
        hintJob?.cancel()
        currentSession.hintMove = null
        hintJob = viewModelScope.launch {
            delay(HINT_DELAY_MS)
            if (session === currentSession && !currentSession.finished && !currentSession.animating) {
                currentSession.hintMove = currentSession.engine.findAvailableMove(currentSession.board)
                emit()
            }
        }
    }

    private fun launchAnimatedAction(action: suspend () -> Unit) {
        if (moveJob?.isActive == true) {
            return
        }
        moveJob = viewModelScope.launch {
            action()
            emit()
        }
    }

    private suspend fun performMoveAnimated(currentSession: GameSession, first: Position, second: Position) {
        val workingBoard = currentSession.board.copyOf()
        val outcome = currentSession.engine.tryMoveAnimated(workingBoard, first, second)
        val completed = if (outcome.valid) {
            applyValidMove(currentSession, first, second, outcome)
        } else {
            if (stage.consumeInvalidMove && !stage.isZenMode) {
                currentSession.moves -= 1
            }
            currentSession.message = INVALID_MOVE_MESSAGE_KEY
            currentSession.invalidSwap = first to second
            currentSession.invalidMoveNonce += 1
            true
        }
        if (!completed) {
            return
        }

        currentSession.animating = false
        currentSession.highlightedMatches = emptySet()
        currentSession.explodingMatches = emptySet()
        finalizeMove(currentSession, stage) { scheduleHint(currentSession) }
        persistProgressIfFinished(
            currentSession,
            GameSessionSpec(stage, totalStages, seed),
            progressRepository,
            ioDispatcher
        )
    }

    /** Returns false if [session] moved on mid-animation (e.g. a replay), meaning [currentSession] is now stale. */
    private suspend fun applyValidMove(
        currentSession: GameSession,
        first: Position,
        second: Position,
        outcome: AnimatedMoveOutcome
    ): Boolean {
        currentSession.animating = true
        currentSession.message = null
        currentSession.board.swap(first, second)
        emit()

        val objective = stage.collectObjective
        val matchedThisMove = mutableListOf<Position>()
        var stillCurrent = true
        for (round in outcome.rounds) {
            if (session !== currentSession) {
                stillCurrent = false
                break
            }
            matchedThisMove += round.matchedPositions
            if (objective != null) {
                val collected = round.matchedPositions.count { position ->
                    currentSession.board.get(position.row, position.col) == objective.pieceType
                }
                currentSession.collectedCount += collected
            }
            currentSession.aroma = (currentSession.aroma + round.matchedPositions.size * AROMA_PER_MATCHED_PIECE)
                .coerceAtMost(AROMA_CAPACITY)
            currentSession.applyRound(round) { emit() }
        }

        if (stillCurrent) {
            currentSession.points += outcome.points
            if (!stage.isZenMode) {
                currentSession.moves -= 1
            }
            for (explosion in currentSession.engine.tickEmptyCups(currentSession.board, matchedThisMove)) {
                if (session !== currentSession) {
                    stillCurrent = false
                    break
                }
                currentSession.animateSpecialOutcome(explosion, stage) { emit() }
            }
        }
        if (!stillCurrent) {
            return false
        }

        if (!currentSession.engine.hasAvailableMove(currentSession.board)) {
            currentSession.engine.shuffleWithoutMatches(currentSession.board)
            currentSession.message = SHUFFLED_MESSAGE_KEY
        } else {
            currentSession.message = if (outcome.cascades > 1) "$COMBO_MESSAGE_KEY_PREFIX${outcome.cascades}" else null
        }
        return true
    }

    /**
     * GP-01: activates whichever special piece sits at [position] (Moedor mills its 8 neighbors,
     * Prensa Francesa compresses its column, Xicara Vazia detonates early) instead of swapping;
     * does not spend a move.
     */
    private suspend fun performSpecialActivation(currentSession: GameSession, position: Position) {
        val workingBoard = currentSession.board.copyOf()
        val outcome = currentSession.engine.activateSpecialPiece(workingBoard, position)
        if (!outcome.activated) {
            return
        }

        currentSession.animateSpecialOutcome(outcome, stage) { emit() }

        currentSession.animating = false
        finalizeMove(currentSession, stage) { scheduleHint(currentSession) }
        persistProgressIfFinished(
            currentSession,
            GameSessionSpec(stage, totalStages, seed),
            progressRepository,
            ioDispatcher
        )
    }

    /** GP-05: replaying without an explicit seed reuses the session's own (e.g. the daily challenge's). */
    private fun emit() {
        _uiState.value = session.toUiState(stage)

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
        savedStateHandle[KEY_STARS] = currentSession.starsEarned
        savedStateHandle[KEY_COLLECTED] = currentSession.collectedCount
        savedStateHandle[KEY_MESSAGE] = currentSession.message
        savedStateHandle[KEY_SELECTED_ROW] = currentSession.selected?.row ?: NO_SELECTION
        savedStateHandle[KEY_SELECTED_COL] = currentSession.selected?.col ?: NO_SELECTION
    }

    companion object {
        /** RR-22: exposed so [com.semstress.mobile.ui.screens.GameScreen] can trigger SFX/haptics on it. */
        const val INVALID_MOVE_MESSAGE_KEY = "invalid_move"

        /** UX-12: [GameUiState.message] keys - display text lives in strings.xml, resolved by the UI layer. */
        const val SHUFFLED_MESSAGE_KEY = "shuffled"
        const val COMBO_MESSAGE_KEY_PREFIX = "combo:"
    }

    @AssistedFactory
    interface Factory {
        fun create(spec: GameSessionSpec): GameViewModel
    }
}

/** GP-05: bundles the assisted-injection args [GameViewModel] needs, to stay under the parameter limit. */
data class GameSessionSpec(
    val stage: StageConfig,
    val totalStages: Int,
    val seed: Long? = null
)

private const val NO_SELECTION = -1

private const val KEY_BOARD = "game_board"
private const val KEY_POINTS = "game_points"
private const val KEY_MOVES = "game_moves"
private const val KEY_FINISHED = "game_finished"
private const val KEY_WON = "game_won"
private const val KEY_STARS = "game_stars"
private const val KEY_COLLECTED = "game_collected"
private const val KEY_MESSAGE = "game_message"
private const val KEY_SELECTED_ROW = "game_selected_row"
private const val KEY_SELECTED_COL = "game_selected_col"

/** Marks [currentSession] finished/won once its objectives (score + optional collect) are met, or out of
 *  moves; calculates stars on finish, otherwise invokes [onIncomplete] (e.g. to reschedule the GP-08 hint). */
private fun finalizeMove(currentSession: GameSession, stage: StageConfig, onIncomplete: () -> Unit) {
    if (stage.isZenMode) {
        onIncomplete()
        return
    }
    val objectivesMet = currentSession.points >= stage.targetScore &&
        (stage.collectObjective?.isComplete(currentSession.collectedCount) ?: true)
    if (objectivesMet) {
        currentSession.finished = true
        currentSession.won = true
    } else if (currentSession.moves <= 0) {
        currentSession.finished = true
        currentSession.won = false
    }
    if (currentSession.finished) {
        currentSession.starsEarned = calculateStars(
            won = currentSession.won,
            score = currentSession.points,
            targetScore = stage.targetScore,
            movesRemaining = currentSession.moves,
            initialMoves = stage.initialMoves
        )
    } else {
        onIncomplete()
    }
}

private fun createFreshSession(
    stage: StageConfig,
    engineFactory: Match3EngineFactory,
    seed: Long? = null
): GameSession {
    val board = Match3Board(stage.rows, stage.cols, stage.pieceTypes, seed)
    val engine = engineFactory.create(stage)
    board.fillRandom()
    engine.ensurePlayableBoard(board)
    engine.resolveBoard(board)
    engine.ensurePlayableBoard(board)
    return GameSession(board = board, engine = engine, moves = stage.initialMoves)
}

private fun restoreSession(
    savedStateHandle: SavedStateHandle,
    stage: StageConfig,
    engineFactory: Match3EngineFactory
): GameSession? {
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
        starsEarned = savedStateHandle.get<Int>(KEY_STARS) ?: 0,
        collectedCount = savedStateHandle.get<Int>(KEY_COLLECTED) ?: 0,
        message = savedStateHandle.get<String>(KEY_MESSAGE)
    )
}

private suspend fun persistProgressIfFinished(
    currentSession: GameSession,
    spec: GameSessionSpec,
    progressRepository: ProgressStore,
    ioDispatcher: CoroutineDispatcher
) {
    if (!currentSession.finished) {
        return
    }
    withContext(ioDispatcher) {
        val loaded = progressRepository.load(spec.totalStages)
        val today = spec.seed ?: java.time.LocalDate.now().toEpochDay()
        val progress = if (spec.stage.id == DAILY_CHALLENGE_STAGE_ID && spec.seed != null) {
            loaded.registerDailyAttempt(today = spec.seed, score = currentSession.points)
        } else {
            loaded.registerResult(
                stageId = spec.stage.id,
                score = currentSession.points,
                won = currentSession.won,
                totalStages = spec.totalStages,
                stars = currentSession.starsEarned
            )
        }
        progressRepository.save(progress.registerPlay(today))
    }
}
