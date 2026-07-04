package com.semstress.mobile.ui.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.semstress.mobile.data.ProgressStore
import com.semstress.mobile.domain.PlayerProgress
import com.semstress.mobile.domain.StageCatalog
import com.semstress.mobile.domain.StageConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Points at the [GameViewModel] instance currently backing the game screen, if any. */
data class ActiveGameRef(val stageId: Int, val playToken: Int)

data class MenuUiState(
    val stages: List<StageConfig> = emptyList(),
    val progress: PlayerProgress = PlayerProgress(),
    val selectedStageId: Int = 1,
    val musicMuted: Boolean = false,
    val activeGame: ActiveGameRef? = null
)

sealed interface MenuAction {
    data class SelectStage(val stageId: Int) : MenuAction
    data object PlaySelectedStage : MenuAction
    data object ToggleMusic : MenuAction
    data object ReturnToMenu : MenuAction
}

/**
 * Owns the stage catalog, player progress and menu navigation pointer. Replaces the
 * menu-related half of the former `CoffeeCrushController`. [MenuUiState.activeGame] is the only
 * navigation state kept here (there is no Navigation Compose back stack yet, see RR-04); it is
 * mirrored into [SavedStateHandle] via [playTokenState]/[activeGameState] so which screen is
 * showing survives rotation/process death same as everything else in this ViewModel.
 */
class MenuViewModel(
    private val stageCatalog: StageCatalog,
    private val progressRepository: ProgressStore,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val stages: List<StageConfig> = stageCatalog.stages

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    fun onAction(action: MenuAction) {
        when (action) {
            is MenuAction.SelectStage -> selectStage(action.stageId)
            MenuAction.PlaySelectedStage -> playSelectedStage()
            MenuAction.ToggleMusic -> toggleMusic()
            MenuAction.ReturnToMenu -> returnToMenu()
        }
    }

    private fun selectStage(stageId: Int) {
        val progress = _uiState.value.progress
        if (!progress.isUnlocked(stageId)) {
            return
        }
        updateState { it.copy(selectedStageId = stageId) }
    }

    private fun playSelectedStage() {
        val stageId = _uiState.value.selectedStageId
        val token = nextPlayToken()
        updateState { it.copy(activeGame = ActiveGameRef(stageId, token)) }
    }

    private fun toggleMusic() {
        updateState { it.copy(musicMuted = !it.musicMuted) }
    }

    private fun returnToMenu() {
        updateState {
            it.copy(
                progress = progressRepository.load(stages.size),
                activeGame = null
            )
        }
    }

    private fun nextPlayToken(): Int {
        val next = (savedStateHandle.get<Int>(KEY_PLAY_TOKEN) ?: 0) + 1
        savedStateHandle[KEY_PLAY_TOKEN] = next
        return next
    }

    private fun createInitialState(): MenuUiState {
        val progress = progressRepository.load(stages.size)
        return MenuUiState(
            stages = stages,
            progress = progress,
            selectedStageId = progress.currentStage.coerceIn(1, stages.size),
            activeGame = savedStateHandle.get<Int>(KEY_ACTIVE_STAGE_ID)?.let { stageId ->
                ActiveGameRef(
                    stageId = stageId,
                    playToken = savedStateHandle.get<Int>(KEY_PLAY_TOKEN) ?: 0
                )
            }
        )
    }

    private fun updateState(transform: (MenuUiState) -> MenuUiState) {
        _uiState.value = transform(_uiState.value).also { persistActiveGame(it.activeGame) }
    }

    private fun persistActiveGame(activeGame: ActiveGameRef?) {
        savedStateHandle[KEY_ACTIVE_STAGE_ID] = activeGame?.stageId
    }

    companion object {
        private const val KEY_ACTIVE_STAGE_ID = "menu_active_stage_id"
        private const val KEY_PLAY_TOKEN = "menu_play_token"

        fun factory(
            stageCatalog: StageCatalog,
            progressRepository: ProgressStore
        ) = viewModelFactory {
            initializer {
                MenuViewModel(
                    stageCatalog = stageCatalog,
                    progressRepository = progressRepository,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
