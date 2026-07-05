package com.semstress.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.semstress.mobile.ui.theme.CoffeeTheme

private const val CUP_HANDLE_RADIUS_FACTOR = 0.28f
private const val CUP_HANDLE_STROKE_FACTOR = 0.14f
private const val CUP_CORNER_FACTOR = 0.12f
private const val CUP_BORDER_FACTOR = 0.04f
private const val CUP_BODY_WIDTH_FACTOR = 0.6f
private const val CUP_HANDLE_GAP_FACTOR = 0.55f
private const val CUP_CENTER_FACTOR = 0.5f

/**
 * UX-01 / UX-05: signature "cup filling up" progress indicator, used for the score-toward-target
 * HUD meter. Draws a simple cup silhouette (rounded body + handle) and clips a rising liquid fill
 * to the [progress] fraction (0f..1f).
 */
@Composable
fun ProgressCup(
    progress: Float,
    modifier: Modifier = Modifier,
    cupWidth: Dp = 64.dp,
    cupHeight: Dp = 64.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = clamped, label = "progressCupFill")
    val colors = CoffeeTheme.colors

    Canvas(modifier = modifier.width(cupWidth).height(cupHeight)) {
        val bodyWidth = size.width * (1f - CUP_HANDLE_RADIUS_FACTOR * CUP_BODY_WIDTH_FACTOR)
        val bodyPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = bodyWidth,
                    bottom = size.height,
                    cornerRadius = CornerRadius(size.width * CUP_CORNER_FACTOR, size.width * CUP_CORNER_FACTOR)
                )
            )
        }

        drawPath(bodyPath, color = colors.progressTrack)

        clipPath(bodyPath) {
            val fillTop = size.height * (1f - animatedProgress)
            drawRect(
                color = colors.progressFill,
                topLeft = Offset(0f, fillTop),
                size = Size(bodyWidth, size.height - fillTop)
            )
        }

        drawPath(
            path = bodyPath,
            color = colors.surfaceBoardBorder,
            style = Stroke(width = size.width * CUP_BORDER_FACTOR)
        )

        val handleRadius = size.width * CUP_HANDLE_RADIUS_FACTOR
        drawCircle(
            color = colors.surfaceBoardBorder,
            radius = handleRadius,
            center = Offset(bodyWidth + handleRadius * CUP_HANDLE_GAP_FACTOR, size.height * CUP_CENTER_FACTOR),
            style = Stroke(width = size.width * CUP_HANDLE_STROKE_FACTOR)
        )
    }
}
