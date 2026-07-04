package com.semstress.mobile.ui.state

import com.semstress.mobile.data.FakeSettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

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
    fun `carrega o valor persistido do mudo ao iniciar`() = runTest(testDispatcher) {
        val store = FakeSettingsStore(musicMuted = true)
        val viewModel = SettingsViewModel(store, testDispatcher)

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.musicMuted)
    }

    @Test
    fun `toggleMusic alterna e persiste a flag de mudo`() = runTest(testDispatcher) {
        val store = FakeSettingsStore()
        val viewModel = SettingsViewModel(store, testDispatcher)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.musicMuted)

        viewModel.toggleMusic()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.musicMuted)
        assertTrue(store.isMusicMuted())
        assertEquals(1, store.saveCount)

        viewModel.toggleMusic()
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(viewModel.uiState.value.musicMuted)
        assertFalse(store.isMusicMuted())
        assertEquals(2, store.saveCount)
    }
}
