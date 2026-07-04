package com.semstress.mobile.ui.sprites

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private const val SPRITE_ROOT = "sprites/current/items"
private const val FRAMES_PER_ITEM = 12
private const val EXPLOSION_ITEM = "fire"

private val PIECE_ITEMS = listOf(
    "coffee-red",
    "coffee-yellow",
    "coffee-white",
    "coffee-green",
    "coffee-brown",
    "coffee-beans"
)

data class GameSpritePack(
    private val pieceFramesByValue: Map<Int, List<ImageBitmap>>,
    private val explosionFrames: List<ImageBitmap>
) {
    val maxFrameCount: Int =
        (pieceFramesByValue.values.maxOfOrNull { it.size } ?: 1)
            .coerceAtLeast(explosionFrames.size.coerceAtLeast(1))

    fun pieceFrame(value: Int, frameIndex: Int): ImageBitmap? {
        val frames = pieceFramesByValue[value] ?: return null
        if (frames.isEmpty()) {
            return null
        }
        return frames[frameIndex % frames.size]
    }

    fun explosionFrame(frameIndex: Int): ImageBitmap? {
        if (explosionFrames.isEmpty()) {
            return null
        }
        return explosionFrames[frameIndex % explosionFrames.size]
    }
}

@Composable
fun rememberGameSpritePack(): GameSpritePack? {
    val context = LocalContext.current
    return produceState<GameSpritePack?>(initialValue = SpritePackCache.current, context) {
        if (value != null) {
            return@produceState
        }
        val loaded = loadSpritePack(context)
        if (loaded != null) {
            SpritePackCache.current = loaded
        }
        value = loaded
    }.value
}

private suspend fun loadSpritePack(context: Context): GameSpritePack? = withContext(Dispatchers.IO) {
    val pieceFramesByValue = mutableMapOf<Int, List<ImageBitmap>>()
    PIECE_ITEMS.forEachIndexed { value, itemName ->
        val frames = loadFrames(context, itemName)
        if (frames.isNotEmpty()) {
            pieceFramesByValue[value] = frames
        }
    }
    if (pieceFramesByValue.isEmpty()) {
        return@withContext null
    }
    val explosionFrames = loadFrames(context, EXPLOSION_ITEM)
    return@withContext GameSpritePack(
        pieceFramesByValue = pieceFramesByValue,
        explosionFrames = explosionFrames
    )
}

private fun loadFrames(context: Context, itemName: String): List<ImageBitmap> {
    val frames = ArrayList<ImageBitmap>(FRAMES_PER_ITEM)
    for (index in 0 until FRAMES_PER_ITEM) {
        val fileName = String.format(Locale.US, "frame_%02d.png", index)
        val path = "$SPRITE_ROOT/$itemName/frames/$fileName"
        val bitmap = BitmapCache.getOrLoad(context, path) ?: continue
        frames += bitmap
    }
    return frames
}

private object SpritePackCache {
    @Volatile
    var current: GameSpritePack? = null
}

private object BitmapCache {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun getOrLoad(context: Context, assetPath: String): ImageBitmap? {
        cache[assetPath]?.let { return it }
        val decoded = runCatching {
            context.assets.open(assetPath).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull() ?: return null
        cache.putIfAbsent(assetPath, decoded)
        return cache[assetPath] ?: decoded
    }
}
