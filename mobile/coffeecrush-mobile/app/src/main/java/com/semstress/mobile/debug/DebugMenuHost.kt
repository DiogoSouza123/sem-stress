package com.semstress.mobile.debug

import androidx.compose.runtime.Composable
import com.semstress.mobile.common.MutableFeatureFlags

data class DebugMenuState(val visible: Boolean)

data class DebugMenuActions(
    val onAddMoves: (Int) -> Unit,
    val onForceWin: () -> Unit,
    val onReshuffleWithSeed: (Long) -> Unit
)

/**
 * CQ-03: entry point for the debug panel (seed, skip stage, +N moves, flag overrides). The real
 * UI only exists in `src/debug` (see `RealDebugMenuHost`); every other build type binds
 * [NoOpDebugMenuHost] instead, so no debug tooling code or affordance ships outside debug builds.
 */
interface DebugMenuHost {
    val isAvailable: Boolean

    @Composable
    fun Menu(
        state: DebugMenuState,
        actions: DebugMenuActions,
        featureFlags: MutableFeatureFlags,
        onDismiss: () -> Unit
    )
}

object NoOpDebugMenuHost : DebugMenuHost {
    override val isAvailable: Boolean = false

    @Composable
    override fun Menu(
        state: DebugMenuState,
        actions: DebugMenuActions,
        featureFlags: MutableFeatureFlags,
        onDismiss: () -> Unit
    ) = Unit
}
