package com.semstress.mobile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * UX-02: a rounded, "food-friendly" display family for titles/HUD numbers, kept distinct from the
 * body family. Uses [DeviceFontFamilyName] to look up the on-device "sans-serif-rounded" system
 * font (shipped on most Android 8+ devices, e.g. Pixel/Samsung) instead of bundling font assets;
 * it degrades gracefully to the platform default wherever that family isn't present.
 */
private val DisplayFontFamily = FontFamily(Font(DeviceFontFamilyName("sans-serif-rounded"), FontWeight.Bold))
private val BodyFontFamily = FontFamily.SansSerif

/**
 * Bundled game-display font (Lilita One, SIL Open Font License) for map/HUD labels drawn over
 * artwork, where the system families look too "app-like".
 */
val GameFontFamily = FontFamily(Font(com.semstress.mobile.R.font.lilita_one))

private val base = Typography()

val Typography = base.copy(
    displayLarge = base.displayLarge.copy(fontFamily = DisplayFontFamily),
    displayMedium = base.displayMedium.copy(fontFamily = DisplayFontFamily),
    displaySmall = base.displaySmall.copy(fontFamily = DisplayFontFamily),
    headlineLarge = base.headlineLarge.copy(fontFamily = DisplayFontFamily),
    headlineMedium = base.headlineMedium.copy(fontFamily = DisplayFontFamily),
    headlineSmall = base.headlineSmall.copy(fontFamily = DisplayFontFamily),
    titleLarge = base.titleLarge.copy(fontFamily = DisplayFontFamily),
    titleMedium = base.titleMedium.copy(fontFamily = DisplayFontFamily),
    titleSmall = base.titleSmall.copy(fontFamily = DisplayFontFamily),
    bodyLarge = base.bodyLarge.copy(fontFamily = BodyFontFamily),
    bodyMedium = base.bodyMedium.copy(fontFamily = BodyFontFamily),
    bodySmall = base.bodySmall.copy(fontFamily = BodyFontFamily),
    labelLarge = base.labelLarge.copy(fontFamily = BodyFontFamily),
    labelMedium = base.labelMedium.copy(fontFamily = BodyFontFamily),
    labelSmall = base.labelSmall.copy(fontFamily = BodyFontFamily)
)

/** UX-02: tabular (monospaced-width) figures so HUD numbers don't shift width as digits change. */
const val TABULAR_NUMBER_FEATURE = "tnum"
