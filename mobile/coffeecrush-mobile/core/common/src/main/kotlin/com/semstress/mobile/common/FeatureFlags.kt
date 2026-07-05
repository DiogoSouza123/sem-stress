package com.semstress.mobile.common

import kotlinx.coroutines.flow.Flow

/**
 * MZ-01: technical and monetization toggles shared across the app. Monetization flags default to
 * `false` and stay off until MZ-02+ wire real surfaces behind them (monetization.md §1.4); until
 * then this infra is used by technical rollouts (e.g. CQ-03's debug panel overrides).
 */
enum class Flag(val default: Boolean) {
    MONETIZATION_MASTER(false),
    REWARDED_ADS(false),
    IAP_STORE(false),
    SEASON_PASS(false)
}

interface FeatureFlags {
    fun isEnabled(flag: Flag): Boolean

    /** Emits whenever a flag's effective value may have changed, so observers can re-read [isEnabled]. */
    val updates: Flow<Unit>
}

/** Adds the ability to override a flag at runtime, used by the debug panel (CQ-03). */
interface MutableFeatureFlags : FeatureFlags {
    /** Passing `null` clears the override, falling back to [Flag.default] (or a remote value once one exists). */
    fun setOverride(flag: Flag, enabled: Boolean?)
}
