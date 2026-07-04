package com.semstress.mobile.ui.screens

import com.semstress.mobile.audio.SfxPlayer

/** Bundles [GameScreen]'s audio dependencies so the composable stays under the parameter limit. */
data class GameScreenSound(
    val isMusicMuted: Boolean,
    val isSfxMuted: Boolean,
    val sfxPlayer: SfxPlayer
)
