package com.semstress.mobile.ui.state

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.semstress.mobile.data.ProgressStore
import com.semstress.mobile.data.StageCatalogSource
import com.semstress.mobile.domain.PlayerProgress
import com.semstress.mobile.domain.StageConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Points at the [GameViewModel] instance currently backing the game screen, if any. */
data class ActiveGameRef(val stageId: Int, val playToken: Int)

data class MenuUiState(
    val isLoading: Boolean = true,
    val stages: List<StageConfig> = emptyList(),
    val progress: PlayerProgress = PlayerProgress(),
    val selectedStageId: Int = 1,
    val menuMusicName: String = "",
    val menuMusicVolumePercent: Int = 0,
    val activeGame: ActiveGameRef? = null
)

sealed interface MenuAction {
    data class SelectStage(val stageId: Int) : MenuAction
    data object PlaySelectedStage : MenuAction
    data object ReturnToMenu : MenuAction
}

/**
 * Owns the stage catalog, player progress and menu navigation pointer. Replaces the
 * menu-related half of the former `CoffeeCrushController`. [MenuUiState.activeGame] is the only
 * navigation state kept here (there is no Navigation Compose back stack yet, see RR-04); it is
 * mirrored into [SavedStateHandle] via [persistActiveGame] so which screen is showing survives
 * rotation/process death same as everything else in this ViewModel.
 *
 * The stage catalog and player progress are loaded on [ioDispatcher] (RR-02): neither
 * [StageCatalogSource.load] nor [ProgressStore.load] are safe to call from the main thread, so
 * [MenuUiState.isLoading] stays `true` until both finish.
 */
class MenuViewModel(
    private val stageCatalogSource: StageCatalogSource,
    private val progressRepository: ProgressStore,
    private val savedStateHandle: SavedStateHandle,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            val catalog = stageCatalogSource.load()
            val progress = progressRepository.load(catalog.stages.size)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                stages = catalog.stages,
                progress = progress,
                selectedStageId = progress.currentStage.coerceIn(1, catalog.stages.size),
                menuMusicName = catalog.menuMusicName,
                menuMusicVolumePercent = catalog.menuMusicVolumePercent
            )
        }
    }

    fun onAction(action: MenuAction) {
        when (action) {
            is MenuAction.SelectStage -> selectStage(action.stageId)
            MenuAction.PlaySelectedStage -> playSelectedStage()
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

    private fun returnToMenu() {
        viewModelScope.launch(ioDispatcher) {
            val totalStages = _uiState.value.stages.size
            val progress = progressRepository.load(totalStages)
            updateState { it.copy(progress = progress, activeGame = null) }
        }
    }

    private fun nextPlayToken(): Int {
        val next = (savedStateHandle.get<Int>(KEY_PLAY_TOKEN) ?: 0) + 1
        savedStateHandle[KEY_PLAY_TOKEN] = next
        return next
    }

    private fun createInitialState(): MenuUiState = MenuUiState(
        activeGame = savedStateHandle.get<Int>(KEY_ACTIVE_STAGE_ID)?.let { stageId ->
            ActiveGameRef(
                stageId = stageId,
                playToken = savedStateHandle.get<Int>(KEY_PLAY_TOKEN) ?: 0
            )
        }
    )

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
            stageCatalogSource: StageCatalogSource,
            progressRepository: ProgressStore
        ) = viewModelFactory {
            initializer {
                MenuViewModel(
                    stageCatalogSource = stageCatalogSource,
                    progressRepository = progressRepository,
                    savedStateHandle = createSavedStateHandle()
                )
            }
        }
    }
}
