package com.semstress.mobile.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_CONCURRENT_STREAMS = 4
private const val PLAYBACK_PRIORITY = 1
private const val NO_LOOP = 0
private const val NORMAL_RATE = 1f
private const val DEFAULT_VOLUME_PERCENT = 100
private const val MIN_VOLUME_PERCENT = 0
private const val MAX_VOLUME_PERCENT = 100

/** Short sound effects (RR-22), distinct from the looping background track in [BackgroundMusicPlayer]. */
enum class SfxEffect(val resourceName: String) {
    SELECT("sfx_select"),
    SWAP("sfx_swap"),
    INVALID_MOVE("sfx_invalid"),
    MATCH("sfx_match"),
    VICTORY("sfx_victory")
}

/**
 * `SoundPool` is the right tool for short, latency-sensitive one-shots (performance.md §5) —
 * unlike [BackgroundMusicPlayer]'s `MediaPlayer`, sounds are decoded once up front and played with
 * near-zero start latency. All [SfxEffect] sounds are preloaded eagerly since they are tiny.
 */
@Singleton
class SfxPlayer @Inject constructor(@ApplicationContext context: Context) {
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MAX_CONCURRENT_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val soundIds: Map<SfxEffect, Int> = SfxEffect.entries.mapNotNull { effect ->
        val resourceId = context.resources.getIdentifier(effect.resourceName, "raw", context.packageName)
        if (resourceId == 0) null else effect to soundPool.load(context, resourceId, 1)
    }.toMap()

    fun play(effect: SfxEffect, volumePercent: Int = DEFAULT_VOLUME_PERCENT) {
        val soundId = soundIds[effect] ?: return
        val gain = volumePercent.coerceIn(MIN_VOLUME_PERCENT, MAX_VOLUME_PERCENT) / MAX_VOLUME_PERCENT.toFloat()
        soundPool.play(soundId, gain, gain, PLAYBACK_PRIORITY, NO_LOOP, NORMAL_RATE)
    }

    fun release() {
        soundPool.release()
    }
}
