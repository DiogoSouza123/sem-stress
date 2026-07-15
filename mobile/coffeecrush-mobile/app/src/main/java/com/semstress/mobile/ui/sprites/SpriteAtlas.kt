package com.semstress.mobile.ui.sprites

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.semstress.mobile.engine.EmptyCupState

/**
 * One decoded spritesheet plus its own frame grid. Since the v2 HD pack, sheets no longer share
 * a single global grid: pieces are single 256px frames while the explosion keeps the animated
 * 4x3 grid of 64px frames, so each sheet carries its own layout.
 */
data class SpriteSheet(
    val bitmap: ImageBitmap,
    val frameSize: IntSize,
    val columns: Int,
    val frameCount: Int
) {
    fun srcOffsetFor(frameIndex: Int): IntOffset {
        val frame = frameIndex % frameCount
        val column = frame % columns
        val row = frame / columns
        return IntOffset(column * frameSize.width, row * frameSize.height)
    }
}

/**
 * Piece-value → sheet lookup for the board renderer. Special pieces (GP-01) resolve to their own
 * sprites: the grinder and french press are single sentinel values, while the empty cup packs its
 * state into a range of values ([EmptyCupState]) that all map to the same sprite.
 */
data class SpriteAtlas(
    private val pieceSheets: Map<Int, SpriteSheet>,
    private val explosion: SpriteSheet?
) {
    /** Highest frame count across sheets — drives the shared draw-phase animation ticker. */
    val maxFrameCount: Int =
        (pieceSheets.values.map { it.frameCount } + (explosion?.frameCount ?: 1)).max()

    fun pieceSheet(value: Int): SpriteSheet? = pieceSheets[normalizePieceValue(value)]

    fun explosionSheet(): SpriteSheet? = explosion

    private fun normalizePieceValue(value: Int): Int =
        if (EmptyCupState.matches(value)) EMPTY_CUP_SPRITE_KEY else value

    companion object {
        /** Synthetic map key for every encoded empty-cup state value. */
        const val EMPTY_CUP_SPRITE_KEY = Int.MIN_VALUE
    }
}
