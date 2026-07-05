package com.semstress.mobile.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundMusicPlayer @Inject constructor(@ApplicationContext private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrack: String? = null
    private var currentVolumePercent: Int = 70

    fun playLooping(rawResourceName: String, volumePercent: Int) {
        val name = rawResourceName.trim()
        if (name.isEmpty()) {
            stop()
            return
        }

        val normalizedVolume = volumePercent.coerceIn(0, 100)
        if (currentTrack == name && mediaPlayer != null) {
            if (currentVolumePercent != normalizedVolume) {
                currentVolumePercent = normalizedVolume
                applyVolume()
            }
            return
        }

        val resourceId = context.resources.getIdentifier(name, "raw", context.packageName)
        if (resourceId == 0) {
            Log.w("BackgroundMusicPlayer", "Audio resource not found: $name")
            stop()
            return
        }

        stop()
        mediaPlayer = MediaPlayer.create(context, resourceId)?.apply {
            isLooping = true
            currentVolumePercent = normalizedVolume
            applyVolume()
            start()
        }
        currentTrack = name
    }

    fun stop() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
        currentTrack = null
    }

    fun release() {
        stop()
    }

    /** RR-22: called when the app goes to background, so the music doesn't keep playing. */
    fun pause() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            }
        }
    }

    /** RR-22: called when the app returns to foreground, resuming from where it stopped. */
    fun resume() {
        mediaPlayer?.let { player ->
            if (!player.isPlaying) {
                player.start()
            }
        }
    }

    private fun applyVolume() {
        val gain = (currentVolumePercent.coerceIn(0, 100) / 100f)
        mediaPlayer?.setVolume(gain, gain)
    }
}
