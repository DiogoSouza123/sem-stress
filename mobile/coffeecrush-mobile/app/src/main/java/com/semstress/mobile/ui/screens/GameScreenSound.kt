package com.semstress.mobile.ui.screens

import com.semstress.mobile.audio.SfxPlayer

/** Bundles [GameScreen]'s audio (and, for UX-11, symbol-mode) dependencies under the parameter limit. */
data class GameScreenSound(
    val isMusicMuted: Boolean,
    val isSfxMuted: Boolean,
    val sfxPlayer: SfxPlayer,
    val isSymbolModeEnabled: Boolean = false
)
