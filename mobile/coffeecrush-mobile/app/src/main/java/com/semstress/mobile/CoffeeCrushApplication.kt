package com.semstress.mobile

import android.app.Application
import android.os.StrictMode
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.semstress.mobile.audio.MusicaFundoPlayer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CoffeeCrushApplication : Application() {

    @Inject
    lateinit var musicaFundoPlayer: MusicaFundoPlayer

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
        // RR-22: a musica continuava tocando com o app em background; pausa/retoma com o ciclo
        // de vida do processo inteiro em vez de depender de uma tela especifica estar visivel.
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    musicaFundoPlayer.pausar()
                }

                override fun onStart(owner: LifecycleOwner) {
                    musicaFundoPlayer.retomar()
                }
            }
        )
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .build()
        )
    }
}
