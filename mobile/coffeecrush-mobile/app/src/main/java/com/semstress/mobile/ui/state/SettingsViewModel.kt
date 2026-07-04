package com.semstress.mobile.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.semstress.mobile.data.SettingsStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(val musicMuted: Boolean = false)

/**
 * Owns audio/settings state shared across screens (RR-03): both the menu and the game screen
 * read/toggle the same mute flag through this single ViewModel instead of routing it through
 * [MenuUiState]. Backed by [SettingsStore] (`SharedPreferences`), read on [ioDispatcher].
 */
class SettingsViewModel(
    private val settingsRepository: SettingsStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch(ioDispatcher) {
            _uiState.value = SettingsUiState(musicMuted = settingsRepository.isMusicMuted())
        }
    }

    fun toggleMusic() {
        val muted = !_uiState.value.musicMuted
        _uiState.value = SettingsUiState(musicMuted = muted)
        viewModelScope.launch(ioDispatcher) {
            settingsRepository.setMusicMuted(muted)
        }
    }

    companion object {
        fun factory(settingsRepository: SettingsStore) = viewModelFactory {
            initializer { SettingsViewModel(settingsRepository) }
        }
    }
}
