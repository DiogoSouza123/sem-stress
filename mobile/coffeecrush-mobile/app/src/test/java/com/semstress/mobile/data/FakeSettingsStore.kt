package com.semstress.mobile.data

class FakeSettingsStore(private var musicMuted: Boolean = false) : SettingsStore {
    var saveCount: Int = 0
        private set

    override fun isMusicMuted(): Boolean = musicMuted

    override fun setMusicMuted(muted: Boolean) {
        musicMuted = muted
        saveCount++
    }
}
