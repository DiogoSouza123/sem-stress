package com.semstress.mobile.ui.screens

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private const val NO_MOTION_SCALE = 0f

/**
 * UX-11: mirrors the system "remove animations" accessibility setting (`Settings.Global
 * .ANIMATOR_DURATION_SCALE == 0`), so decorative animations (shake, pulse) can be skipped for
 * players who requested reduced motion, without needing an in-app duplicate toggle.
 */
@Composable
fun rememberReducedMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        scale == NO_MOTION_SCALE
    }
}
