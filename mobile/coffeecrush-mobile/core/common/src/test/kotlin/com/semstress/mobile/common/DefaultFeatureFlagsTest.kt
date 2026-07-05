package com.semstress.mobile.common

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultFeatureFlagsTest {

    @Test
    fun `flags fall back to their default when there is no override`() {
        val flags = DefaultFeatureFlags()

        assertFalse(flags.isEnabled(Flag.REWARDED_ADS))
    }

    @Test
    fun `an override takes priority over the default`() {
        val flags = DefaultFeatureFlags()

        flags.setOverride(Flag.REWARDED_ADS, true)

        assertTrue(flags.isEnabled(Flag.REWARDED_ADS))
    }

    @Test
    fun `clearing an override falls back to the default again`() {
        val flags = DefaultFeatureFlags()
        flags.setOverride(Flag.REWARDED_ADS, true)

        flags.setOverride(Flag.REWARDED_ADS, null)

        assertFalse(flags.isEnabled(Flag.REWARDED_ADS))
    }
}
