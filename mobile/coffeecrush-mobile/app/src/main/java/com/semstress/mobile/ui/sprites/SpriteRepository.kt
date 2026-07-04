package com.semstress.mobile.ui.sprites

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Loads the [SpriteAtlas], downsampling frames when the device density needs less detail. */
interface SpriteAtlasSource {
    suspend fun load(): SpriteAtlas?
}

private const val SPRITE_ROOT = "sprites/current/items"
private const val SHEET_FILE_NAME = "sheet.png"
private const val EXPLOSION_ITEM = "fire"
private const val SHEET_COLUMNS = 4
private const val SHEET_ROWS = 3
private const val FRAME_COUNT = SHEET_COLUMNS * SHEET_ROWS
private const val NATIVE_FRAME_SIZE_PX = 64

/** Cell size the atlas is tuned for; boards render bigger/smaller cells but this keeps memory low. */
private const val REFERENCE_CELL_DP = 56f

private val PIECE_ITEMS = listOf(
    "coffee-red",
    "coffee-yellow",
    "coffee-white",
    "coffee-green",
    "coffee-brown",
    "coffee-beans"
)

/**
 * Reads the spritesheets already produced by the desktop `SpriteSheetGenerator` tool
 * (`sprites/current/items/<item>/sheet.png`, a 4x3 grid of 64x64 frames) instead of the 12
 * individual PNG frames per item used previously. Pre-loaded once (app-scoped singleton) during
 * the splash screen by [com.semstress.mobile.ui.state.MenuViewModel].
 */
@Singleton
class SpriteRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : SpriteAtlasSource {

    @Volatile
    private var cached: SpriteAtlas? = null

    override suspend fun load(): SpriteAtlas? {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            cached ?: buildAtlas()?.also { cached = it }
        }
    }

    private fun buildAtlas(): SpriteAtlas? {
        val sampleSize = sampleSizeForTargetCell()
        val pieceSheets = PIECE_ITEMS.withIndex()
            .mapNotNull { (value, item) -> decodeSheet(item, sampleSize)?.let { value to it } }
            .toMap()
        if (pieceSheets.isEmpty()) {
            return null
        }
        val frameSide = NATIVE_FRAME_SIZE_PX / sampleSize
        return SpriteAtlas(
            pieceSheets = pieceSheets,
            explosionSheet = decodeSheet(EXPLOSION_ITEM, sampleSize),
            frameSize = IntSize(frameSide, frameSide),
            columns = SHEET_COLUMNS,
            frameCount = FRAME_COUNT
        )
    }

    private fun sampleSizeForTargetCell(): Int {
        val density = context.resources.displayMetrics.density
        val targetPx = (REFERENCE_CELL_DP * density).toInt().coerceAtLeast(1)
        var sample = 1
        while (NATIVE_FRAME_SIZE_PX / (sample * 2) >= targetPx) {
            sample *= 2
        }
        return sample
    }

    private fun decodeSheet(itemName: String, sampleSize: Int): ImageBitmap? {
        val path = "$SPRITE_ROOT/$itemName/$SHEET_FILE_NAME"
        return runCatching {
            context.assets.open(path).use { input ->
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeStream(input, null, options)?.asImageBitmap()
            }
        }.getOrNull()
    }
}
