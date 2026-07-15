package com.semstress.mobile.ui.sprites

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import com.semstress.mobile.engine.Match3Engine
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

/** v2 HD pieces: one static 256px frame per item. */
private const val PIECE_FRAME_SIZE_PX = 256

/** Explosion keeps the v1 animated sheet: 4x3 grid of 64px frames. */
private const val EXPLOSION_ITEM = "fire"
private const val EXPLOSION_COLUMNS = 4
private const val EXPLOSION_ROWS = 3
private const val EXPLOSION_FRAME_SIZE_PX = 64

/** Cell size the atlas is tuned for; boards render bigger/smaller cells but this keeps memory low. */
private const val REFERENCE_CELL_DP = 56f

/** Regular pieces, indexed by their board value (0..5). */
private val PIECE_ITEMS = listOf(
    "coffee-red",
    "coffee-yellow",
    "coffee-white",
    "coffee-green",
    "coffee-brown",
    "coffee-beans"
)

/** GP-01 special pieces, keyed by their engine sentinel value. */
private val SPECIAL_ITEMS = mapOf(
    Match3Engine.SPECIAL_GRINDER to "special-grinder",
    Match3Engine.SPECIAL_FRENCH_PRESS to "special-french-press",
    SpriteAtlas.EMPTY_CUP_SPRITE_KEY to "special-steam"
)

/**
 * Reads the v2 HD sprite pack (`sprites/current/items/<item>/sheet.png`): one static 256px frame
 * per piece (regular and special), plus the animated 4x3 explosion sheet inherited from v1.
 * Pre-loaded once (app-scoped singleton) during the splash screen by
 * [com.semstress.mobile.ui.state.MenuViewModel].
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
        val pieceSide = PIECE_FRAME_SIZE_PX / sampleSize
        val pieceEntries = PIECE_ITEMS.withIndex().map { (value, item) -> value to item } +
            SPECIAL_ITEMS.map { (value, item) -> value to item }
        val pieceSheets = pieceEntries
            .mapNotNull { (value, item) ->
                decodeBitmap(item, sampleSize)?.let { bitmap ->
                    value to SpriteSheet(
                        bitmap = bitmap,
                        frameSize = IntSize(pieceSide, pieceSide),
                        columns = 1,
                        frameCount = 1
                    )
                }
            }
            .toMap()
        if (pieceSheets.isEmpty()) {
            return null
        }
        return SpriteAtlas(
            pieceSheets = pieceSheets,
            explosion = decodeBitmap(EXPLOSION_ITEM, sampleSize = 1)?.let { bitmap ->
                SpriteSheet(
                    bitmap = bitmap,
                    frameSize = IntSize(EXPLOSION_FRAME_SIZE_PX, EXPLOSION_FRAME_SIZE_PX),
                    columns = EXPLOSION_COLUMNS,
                    frameCount = EXPLOSION_COLUMNS * EXPLOSION_ROWS
                )
            }
        )
    }

    private fun sampleSizeForTargetCell(): Int {
        val density = context.resources.displayMetrics.density
        val targetPx = (REFERENCE_CELL_DP * density).toInt().coerceAtLeast(1)
        var sample = 1
        while (PIECE_FRAME_SIZE_PX / (sample * 2) >= targetPx) {
            sample *= 2
        }
        return sample
    }

    private fun decodeBitmap(itemName: String, sampleSize: Int): ImageBitmap? {
        val path = "$SPRITE_ROOT/$itemName/$SHEET_FILE_NAME"
        return runCatching {
            context.assets.open(path).use { input ->
                val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeStream(input, null, options)?.asImageBitmap()
            }
        }.getOrNull()
    }
}
