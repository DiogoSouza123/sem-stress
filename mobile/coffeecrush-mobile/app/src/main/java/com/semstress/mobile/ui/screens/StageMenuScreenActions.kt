package com.semstress.mobile.ui.screens

/** Bundles [StageMenuScreen]'s callbacks so the composable stays under the parameter limit. */
data class StageMenuScreenActions(
    val onSelectStage: (Int) -> Unit,
    val onPlaySelectedStage: () -> Unit,
    val onPlayZenMode: () -> Unit,
    val onPlayDailyChallenge: () -> Unit,
    val onToggleMusic: () -> Unit,
    val onToggleSfx: () -> Unit,
    val onOpenSettings: () -> Unit
)
