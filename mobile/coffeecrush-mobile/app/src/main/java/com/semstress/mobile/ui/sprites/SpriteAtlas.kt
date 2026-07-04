package com.semstress.mobile.ui.sprites

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * One spritesheet bitmap per piece value plus one for the explosion effect, all sharing the same
 * frame grid. Replaces the old per-frame `ImageBitmap` list (84 separate decoded bitmaps) with 7
 * bitmaps total; frame lookup becomes a `srcOffset` into the sheet instead of a list index.
 */
data class SpriteAtlas(
    private val pieceSheets: Map<Int, ImageBitmap>,
    private val explosionSheet: ImageBitmap?,
    val frameSize: IntSize,
    val columns: Int,
    val frameCount: Int
) {
    fun pieceSheet(value: Int): ImageBitmap? = pieceSheets[value]

    fun explosionSheet(): ImageBitmap? = explosionSheet

    fun srcOffsetFor(frameIndex: Int): IntOffset {
        val frame = frameIndex % frameCount
        val column = frame % columns
        val row = frame / columns
        return IntOffset(column * frameSize.width, row * frameSize.height)
    }
}
