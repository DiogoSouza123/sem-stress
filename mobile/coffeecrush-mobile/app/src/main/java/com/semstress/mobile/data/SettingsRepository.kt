package com.semstress.mobile.data

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface SettingsStore {
    fun isMusicMuted(): Boolean
    fun setMusicMuted(muted: Boolean)
}

class SettingsRepository @Inject constructor(@ApplicationContext context: Context) : SettingsStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun isMusicMuted(): Boolean = prefs.getBoolean(KEY_MUSIC_MUTED, false)

    override fun setMusicMuted(muted: Boolean) {
        prefs.edit { putBoolean(KEY_MUSIC_MUTED, muted) }
    }

    companion object {
        private const val PREFS_NAME = "coffee_crush_mobile_settings"
        private const val KEY_MUSIC_MUTED = "music_muted"
    }
}
