package com.semstress.mobile.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic color roles (UX-01): screens should reference these instead of raw palette colors. */
data class CoffeeSemanticColors(
    val surfaceBoard: Color,
    val surfaceBoardBorder: Color,
    val pieceHighlight: Color,
    val pieceExplosion: Color,
    val success: Color,
    val danger: Color,
    val warning: Color,
    val hudText: Color,
    val hudTextMuted: Color,
    val panelBackground: Color,
    val panelBorder: Color,
    val progressTrack: Color,
    val progressFill: Color
)

val LightCoffeeColors = CoffeeSemanticColors(
    surfaceBoard = Latte,
    surfaceBoardBorder = Caramel,
    pieceHighlight = Gold,
    pieceExplosion = Mint,
    success = Mint,
    danger = Danger,
    warning = Gold,
    hudText = CoffeeDark,
    hudTextMuted = CoffeeDark.copy(alpha = 0.7f),
    panelBackground = Cream,
    panelBorder = Caramel.copy(alpha = 0.5f),
    progressTrack = CoffeeLight,
    progressFill = Caramel
)

val DarkCoffeeColors = CoffeeSemanticColors(
    surfaceBoard = CoffeeMedium,
    surfaceBoardBorder = Caramel,
    pieceHighlight = Gold,
    pieceExplosion = Mint,
    success = Mint,
    danger = DangerOnDark,
    warning = Gold,
    hudText = Cream,
    hudTextMuted = Cream.copy(alpha = 0.7f),
    panelBackground = CoffeeDark,
    panelBorder = Caramel.copy(alpha = 0.4f),
    progressTrack = CoffeeMedium,
    progressFill = Gold
)

val LocalCoffeeColors = staticCompositionLocalOf { LightCoffeeColors }
