package com.semstress.mobile.ui.screens

/** Bundles [StageMenuScreen]'s mute flags so the composable stays under the parameter limit. */
data class StageMenuScreenSound(
    val isMusicMuted: Boolean,
    val isSfxMuted: Boolean
)
