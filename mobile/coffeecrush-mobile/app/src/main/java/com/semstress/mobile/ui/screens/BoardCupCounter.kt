package com.semstress.mobile.ui.screens

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

private val RainbowRed = Color(color = 0xFFFF3B30)
private val RainbowOrange = Color(color = 0xFFFF9500)
private val RainbowYellow = Color(color = 0xFFFFCC00)
private val RainbowGreen = Color(color = 0xFF34C759)
private val RainbowBlue = Color(color = 0xFF32ADE6)
private val RainbowViolet = Color(color = 0xFFAF52DE)

/** GP-01: rainbow palette for the Vapor counter - each digit cycles to the next color. */
private val RainbowDigitColors =
    listOf(RainbowRed, RainbowOrange, RainbowYellow, RainbowGreen, RainbowBlue, RainbowViolet)

private const val COUNTER_FONT_FRACTION = 0.24f
private const val COUNTER_BOTTOM_PADDING_FRACTION = 0.02f
private const val COUNTER_MAX_WIDTH_FRACTION = 0.94f
private const val COUNTER_SHADOW_BLUR = 5f

/**
 * GP-01: the points the Vapor piece has absorbed so far, drawn along the bottom edge of its own
 * cell (never bleeding into neighboring cells) with one rainbow color per digit.
 */
internal fun DrawScope.drawCupAbsorbedPoints(
    textMeasurer: TextMeasurer,
    points: Int,
    topLeft: Offset,
    cellSizePx: Float
) {
    val layout = measureCounter(textMeasurer, rainbowDigits(points.toString()), cellSizePx)
    val bottomPadding = cellSizePx * COUNTER_BOTTOM_PADDING_FRACTION
    val offset = Offset(
        x = topLeft.x + (cellSizePx - layout.size.width) / 2f,
        y = topLeft.y + cellSizePx - layout.size.height - bottomPadding
    )
    drawText(layout, topLeft = offset)
}

/** Measures at the default size, shrinking the font once if the digits would overflow the cell. */
private fun DrawScope.measureCounter(
    textMeasurer: TextMeasurer,
    text: AnnotatedString,
    cellSizePx: Float
): TextLayoutResult {
    val maxWidthPx = cellSizePx * COUNTER_MAX_WIDTH_FRACTION
    val baseSizePx = cellSizePx * COUNTER_FONT_FRACTION
    val baseLayout = textMeasurer.measure(text, counterStyle(baseSizePx))
    if (baseLayout.size.width <= maxWidthPx) {
        return baseLayout
    }
    val shrunkSizePx = baseSizePx * (maxWidthPx / baseLayout.size.width)
    return textMeasurer.measure(text, counterStyle(shrunkSizePx))
}

private fun DrawScope.counterStyle(fontSizePx: Float): TextStyle = TextStyle(
    fontSize = fontSizePx.toSp(),
    fontWeight = FontWeight.Bold,
    shadow = Shadow(color = Color.Black, blurRadius = COUNTER_SHADOW_BLUR)
)

private fun rainbowDigits(text: String): AnnotatedString = buildAnnotatedString {
    text.forEachIndexed { index, char ->
        withStyle(SpanStyle(color = RainbowDigitColors[index % RainbowDigitColors.size])) {
            append(char)
        }
    }
}
