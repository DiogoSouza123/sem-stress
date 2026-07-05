package com.semstress.mobile.data

class FakeSettingsStore(
    private var musicMuted: Boolean = false,
    private var sfxMuted: Boolean = false,
    private var symbolModeEnabled: Boolean = false
) : SettingsStore {
    var saveCount: Int = 0
        private set

    override fun isMusicMuted(): Boolean = musicMuted

    override fun setMusicMuted(muted: Boolean) {
        musicMuted = muted
        saveCount++
    }

    override fun isSfxMuted(): Boolean = sfxMuted

    override fun setSfxMuted(muted: Boolean) {
        sfxMuted = muted
        saveCount++
    }

    override fun isSymbolModeEnabled(): Boolean = symbolModeEnabled

    override fun setSymbolModeEnabled(enabled: Boolean) {
        symbolModeEnabled = enabled
        saveCount++
    }
}
