package com.semstress.mobile.common

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MZ-01's flag chain, collapsed into a single class since there is no remote layer yet
 * (monetization.md §1.4 mentions `RemoteConfigFlags` as future work behind Firebase): an override
 * set here (only ever written by the debug panel, CQ-03) wins over [Flag.default].
 */
@Singleton
class DefaultFeatureFlags @Inject constructor() : MutableFeatureFlags {
    private val overrides = ConcurrentHashMap<Flag, Boolean>()
    private val _updates = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    override val updates: Flow<Unit> = _updates.asSharedFlow()

    override fun isEnabled(flag: Flag): Boolean = overrides[flag] ?: flag.default

    override fun setOverride(flag: Flag, enabled: Boolean?) {
        if (enabled == null) {
            overrides.remove(flag)
        } else {
            overrides[flag] = enabled
        }
        _updates.tryEmit(Unit)
    }
}
