package com.semstress.mobile.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semstress.mobile.data.ProgressStore
import com.semstress.mobile.data.StageCatalogSource
import com.semstress.mobile.di.IoDispatcher
import com.semstress.mobile.domain.PlayerProgress
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.ui.sprites.SpriteAtlas
import com.semstress.mobile.ui.sprites.SpriteAtlasSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuUiState(
    val isLoading: Boolean = true,
    val stages: List<StageConfig> = emptyList(),
    val progress: PlayerProgress = PlayerProgress(),
    val selectedStageId: Int = 1,
    val menuMusicName: String = "",
    val menuMusicVolumePercent: Int = 0,
    val spriteAtlas: SpriteAtlas? = null
)

sealed interface MenuAction {
    data class SelectStage(val stageId: Int) : MenuAction
    data object ReturnToMenu : MenuAction
}

/**
 * Owns the stage catalog and player progress. Replaces the menu-related half of the former
 * `CoffeeCrushController`. Since RR-04, which screen is showing (menu vs. a specific game) is
 * owned by the Navigation Compose back stack instead of this ViewModel; `SavedStateHandle` is no
 * longer needed here because that back stack already survives process death on its own.
 *
 * The stage catalog, player progress and sprite atlas (RR-21) are loaded on [ioDispatcher]
 * (RR-02): neither [StageCatalogSource.load] nor [ProgressStore.load] are safe to call from the
 * main thread, so [MenuUiState.isLoading] stays `true` until all three finish. Loading the sprite
 * atlas here means it is ready before the splash screen hands off to the menu/game UI.
 */
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val stageCatalogSource: StageCatalogSource,
    private val progressRepository: ProgressStore,
    private val spriteAtlasSource: SpriteAtlasSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuUiState())
    val uiState: StateFlow<MenuUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            val catalog = stageCatalogSource.load()
            val progress = progressRepository.load(catalog.stages.size)
            val spriteAtlas = spriteAtlasSource.load()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                stages = catalog.stages,
                progress = progress,
                selectedStageId = progress.currentStage.coerceIn(1, catalog.stages.size),
                menuMusicName = catalog.menuMusicName,
                menuMusicVolumePercent = catalog.menuMusicVolumePercent,
                spriteAtlas = spriteAtlas
            )
        }
    }

    fun onAction(action: MenuAction) {
        when (action) {
            is MenuAction.SelectStage -> selectStage(action.stageId)
            MenuAction.ReturnToMenu -> returnToMenu()
        }
    }

    private fun selectStage(stageId: Int) {
        val progress = _uiState.value.progress
        if (!progress.isUnlocked(stageId)) {
            return
        }
        _uiState.value = _uiState.value.copy(selectedStageId = stageId)
    }

    private fun returnToMenu() {
        viewModelScope.launch(ioDispatcher) {
            val totalStages = _uiState.value.stages.size
            val progress = progressRepository.load(totalStages)
            _uiState.value = _uiState.value.copy(progress = progress)
        }
    }
}
