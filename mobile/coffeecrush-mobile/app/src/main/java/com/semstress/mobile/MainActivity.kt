package com.semstress.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.semstress.mobile.audio.MusicaFundoPlayer
import com.semstress.mobile.data.ProgressRepository
import com.semstress.mobile.data.StageRepository
import com.semstress.mobile.ui.screens.GameScreen
import com.semstress.mobile.ui.screens.StageMenuScreen
import com.semstress.mobile.ui.state.GameAction
import com.semstress.mobile.ui.state.GameViewModel
import com.semstress.mobile.ui.state.MenuAction
import com.semstress.mobile.ui.state.MenuViewModel
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
    val progressRepository = remember(appContext) {
        ProgressRepository(appContext)
    }
    val musicaFundoPlayer = remember(appContext) {
        MusicaFundoPlayer(appContext)
    }

    val menuViewModel: MenuViewModel = viewModel(
        factory = MenuViewModel.factory(stageCatalog, progressRepository)
    )
    val menuState by menuViewModel.uiState.collectAsStateWithLifecycle()
    val activeGame = menuState.activeGame

    val musicaAtual = if (activeGame == null) {
        val menuMusicSilent = stageCatalog.menuMusicVolumePercent <= 0 ||
            stageCatalog.menuMusicName == StageRepository.SILENT_MUSIC_RESOURCE
        if (menuMusicSilent) {
            null
        } else {
            Pair(stageCatalog.menuMusicName, stageCatalog.menuMusicVolumePercent)
        }
    } else {
        val stage = stageCatalog.stages.firstOrNull { it.id == activeGame.stageId }
        stage?.let {
            if (it.musicVolumePercent <= 0 || it.musicName == StageRepository.SILENT_MUSIC_RESOURCE) {
                null
            } else {
                Pair(it.musicName, it.musicVolumePercent)
            }
        }
    }

    LaunchedEffect(musicaAtual?.first, musicaAtual?.second, menuState.musicMuted) {
        if (menuState.musicMuted || musicaAtual == null) {
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

    if (activeGame == null) {
        StageMenuScreen(
            stages = menuState.stages,
            progress = menuState.progress,
            selectedStageId = menuState.selectedStageId,
            isMusicMuted = menuState.musicMuted,
            onSelectStage = { menuViewModel.onAction(MenuAction.SelectStage(it)) },
            onPlaySelectedStage = { menuViewModel.onAction(MenuAction.PlaySelectedStage) },
            onToggleMusic = { menuViewModel.onAction(MenuAction.ToggleMusic) }
        )
    } else {
        val stage = stageCatalog.stages.first { it.id == activeGame.stageId }
        val gameViewModel: GameViewModel = viewModel(
            key = "game_${activeGame.stageId}_${activeGame.playToken}",
            factory = GameViewModel.factory(stage, stageCatalog.stages.size, progressRepository)
        )
        val gameState by gameViewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(gameViewModel) {
            gameViewModel.backToMenuRequests.collect {
                menuViewModel.onAction(MenuAction.ReturnToMenu)
            }
        }

        GameScreen(
            game = gameState,
            isMusicMuted = menuState.musicMuted,
            onCellTap = { row, col -> gameViewModel.onAction(GameAction.CellTapped(row, col)) },
            onCellDragSwap = { fromRow, fromCol, toRow, toCol ->
                gameViewModel.onAction(GameAction.CellDragSwapped(fromRow, fromCol, toRow, toCol))
            },
            onBackToMenu = { gameViewModel.onAction(GameAction.BackToMenu) },
            onReplayStage = { gameViewModel.onAction(GameAction.Replay) },
            onToggleMusic = { menuViewModel.onAction(MenuAction.ToggleMusic) }
        )
    }
}
