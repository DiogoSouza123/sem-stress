package com.semstress.mobile.ui.screens

import com.semstress.mobile.common.MutableFeatureFlags
import com.semstress.mobile.debug.DebugMenuActions
import com.semstress.mobile.debug.DebugMenuHost

data class GameScreenDebugTools(
    val host: DebugMenuHost,
    val featureFlags: MutableFeatureFlags,
    val actions: DebugMenuActions
)
