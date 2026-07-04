package com.semstress.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CoffeeMedium,
    onPrimary = Cream,
    secondary = Caramel,
    onSecondary = CoffeeDark,
    background = Cream,
    onBackground = CoffeeDark,
    surface = Latte,
    onSurface = CoffeeDark
)

private val DarkColors = darkColorScheme(
    primary = Caramel,
    onPrimary = CoffeeDark,
    secondary = CoffeeLight,
    onSecondary = CoffeeDark,
    background = CoffeeDark,
    onBackground = Cream,
    surface = CoffeeMedium,
    onSurface = Cream
)

@Composable
fun CoffeeCrushTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
