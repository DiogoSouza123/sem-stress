package com.semstress.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.semstress.mobile.audio.MusicaFundoPlayer
import com.semstress.mobile.data.ProgressRepository
import com.semstress.mobile.data.StageRepository
import com.semstress.mobile.ui.screens.GameScreen
import com.semstress.mobile.ui.screens.StageMenuScreen
import com.semstress.mobile.ui.state.AppScreen
import com.semstress.mobile.ui.state.CoffeeCrushController
import com.semstress.mobile.ui.theme.CoffeeCrushTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoffeeCrushTheme {
                CoffeeCrushApp()
            }
        }
    }
}

@Composable
private fun CoffeeCrushApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val stageCatalog = remember(appContext) {
        StageRepository(appContext).load()
    }
    val controller = remember {
        CoffeeCrushController(
            stageCatalog = stageCatalog,
            progressRepository = ProgressRepository(appContext)
        )
    }
    val musicaFundoPlayer = remember(appContext) {
        MusicaFundoPlayer(appContext)
    }
    val state = controller.uiState
    val musicaAtual = when (state.screen) {
        AppScreen.MENU -> {
            if (state.menuMusicVolumePercent <= 0 || state.menuMusicName == StageRepository.SILENT_MUSIC_RESOURCE) {
                null
            } else {
                Pair(state.menuMusicName, state.menuMusicVolumePercent)
            }
        }
        AppScreen.GAME -> {
            val game = state.game
            val fase = state.stages.firstOrNull { it.id == game?.stageId }
            fase?.let {
                if (it.musicVolumePercent <= 0 || it.musicName == StageRepository.SILENT_MUSIC_RESOURCE) {
                    null
                } else {
                    Pair(it.musicName, it.musicVolumePercent)
                }
            }
        }
    }

    LaunchedEffect(musicaAtual?.first, musicaAtual?.second, state.musicMuted) {
        if (state.musicMuted || musicaAtual == null) {
            musicaFundoPlayer.parar()
        } else {
            musicaFundoPlayer.tocarEmLoop(musicaAtual.first, musicaAtual.second)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            musicaFundoPlayer.liberar()
        }
    }

    when (state.screen) {
        AppScreen.MENU -> {
            StageMenuScreen(
                stages = state.stages,
                progress = state.progress,
                selectedStageId = state.selectedStageId,
                isMusicMuted = state.musicMuted,
                onSelectStage = controller::selectStage,
                onPlaySelectedStage = controller::startSelectedStage,
                onToggleMusic = controller::toggleMusic
            )
        }

        AppScreen.GAME -> {
            val game = state.game
            if (game != null) {
                GameScreen(
                    game = game,
                    isMusicMuted = state.musicMuted,
                    onCellTap = controller::onCellTap,
                    onCellDragSwap = controller::onCellDragSwap,
                    onBackToMenu = controller::backToMenu,
                    onReplayStage = controller::replayCurrentStage,
                    onToggleMusic = controller::toggleMusic
                )
            }
        }
    }
}
