package com.semstress.mobile.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.semstress.mobile.data.SettingsStore
import com.semstress.mobile.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(val musicMuted: Boolean = false)

/**
 * Owns audio/settings state shared across screens (RR-03): both the menu and the game screen
 * read/toggle the same mute flag through this single ViewModel instead of routing it through
 * [MenuUiState]. Backed by [SettingsStore] (`SharedPreferences`), read on [ioDispatcher].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
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
}
