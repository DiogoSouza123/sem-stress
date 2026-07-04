package com.semstress.mobile.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

class MusicaFundoPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var musicaAtual: String? = null
    private var volumeAtualPercentual: Int = 70

    fun tocarEmLoop(nomeRecursoRaw: String, volumePercentual: Int) {
        val nome = nomeRecursoRaw.trim()
        if (nome.isEmpty()) {
            parar()
            return
        }

        val volumeNormalizado = volumePercentual.coerceIn(0, 100)
        if (musicaAtual == nome && mediaPlayer != null) {
            if (volumeAtualPercentual != volumeNormalizado) {
                volumeAtualPercentual = volumeNormalizado
                aplicarVolume()
            }
            return
        }

        val resourceId = context.resources.getIdentifier(nome, "raw", context.packageName)
        if (resourceId == 0) {
            Log.w("MusicaFundoPlayer", "Recurso de audio nao encontrado: $nome")
            parar()
            return
        }

        parar()
        mediaPlayer = MediaPlayer.create(context, resourceId)?.apply {
            isLooping = true
            volumeAtualPercentual = volumeNormalizado
            aplicarVolume()
            start()
        }
        musicaAtual = nome
    }

    fun parar() {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
        }
        mediaPlayer = null
        musicaAtual = null
    }

    fun liberar() {
        parar()
    }

    private fun aplicarVolume() {
        val ganho = (volumeAtualPercentual.coerceIn(0, 100) / 100f)
        mediaPlayer?.setVolume(ganho, ganho)
    }
}
