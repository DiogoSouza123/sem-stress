package com.semstress.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.semstress.mobile.audio.MusicaFundoPlayer
import com.semstress.mobile.data.StageRepository
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.ui.navigation.GameRoute
import com.semstress.mobile.ui.navigation.MenuRoute
import com.semstress.mobile.ui.screens.GameScreen
import com.semstress.mobile.ui.screens.StageMenuScreen
import com.semstress.mobile.ui.state.GameAction
import com.semstress.mobile.ui.state.GameViewModel
import com.semstress.mobile.ui.state.MenuAction
import com.semstress.mobile.ui.state.MenuUiState
import com.semstress.mobile.ui.state.MenuViewModel
import com.semstress.mobile.ui.state.SettingsViewModel
import com.semstress.mobile.ui.theme.CoffeeCrushTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val GAME_DEEP_LINK_PATTERN = "coffeecrush://game/{stageId}"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var musicaFundoPlayer: MusicaFundoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CoffeeCrushTheme {
                CoffeeCrushApp(musicaFundoPlayer)
            }
        }
    }
}

@Composable
private fun CoffeeCrushApp(musicaFundoPlayer: MusicaFundoPlayer) {
    val menuViewModel: MenuViewModel = hiltViewModel()
    val menuState by menuViewModel.uiState.collectAsStateWithLifecycle()

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            musicaFundoPlayer.liberar()
        }
    }

    if (menuState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = MenuRoute) {
        composable<MenuRoute> {
            MenuDestination(navController, menuViewModel, settingsViewModel, musicaFundoPlayer)
        }

        composable<GameRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = GAME_DEEP_LINK_PATTERN })
        ) { backStackEntry ->
            val route: GameRoute = backStackEntry.toRoute()
            GameDestination(route, navController, menuState, settingsViewModel, musicaFundoPlayer)
        }
    }
}

@Composable
private fun MenuDestination(
    navController: NavController,
    menuViewModel: MenuViewModel,
    settingsViewModel: SettingsViewModel,
    musicaFundoPlayer: MusicaFundoPlayer
) {
    val menuState by menuViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    // Refreshes progress every time the menu becomes the visible destination again (both the
    // explicit "back to menu" button and the system back gesture from the game screen land
    // here), since GameViewModel writes progress directly to disk.
    LaunchedEffect(Unit) {
        menuViewModel.onAction(MenuAction.ReturnToMenu)
    }

    PlayTrackForMenu(menuState, settingsState.musicMuted, musicaFundoPlayer)

    StageMenuScreen(
        stages = menuState.stages,
        progress = menuState.progress,
        selectedStageId = menuState.selectedStageId,
        isMusicMuted = settingsState.musicMuted,
        onSelectStage = { menuViewModel.onAction(MenuAction.SelectStage(it)) },
        onPlaySelectedStage = { navController.navigate(GameRoute(menuState.selectedStageId)) },
        onToggleMusic = { settingsViewModel.toggleMusic() }
    )
}

@Composable
private fun GameDestination(
    route: GameRoute,
    navController: NavController,
    menuState: MenuUiState,
    settingsViewModel: SettingsViewModel,
    musicaFundoPlayer: MusicaFundoPlayer
) {
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val stage = menuState.stages.first { it.id == route.stageId }
    val gameViewModel: GameViewModel = hiltViewModel<GameViewModel, GameViewModel.Factory>(
        creationCallback = { factory -> factory.create(stage, menuState.stages.size) }
    )
    val gameState by gameViewModel.uiState.collectAsStateWithLifecycle()

    PlayTrackForStage(stage, settingsState.musicMuted, musicaFundoPlayer)

    LaunchedEffect(gameViewModel) {
        gameViewModel.backToMenuRequests.collect {
            navController.popBackStack()
        }
    }

    GameScreen(
        game = gameState,
        isMusicMuted = settingsState.musicMuted,
        onCellTap = { row, col -> gameViewModel.onAction(GameAction.CellTapped(row, col)) },
        onCellDragSwap = { fromRow, fromCol, toRow, toCol ->
            gameViewModel.onAction(GameAction.CellDragSwapped(fromRow, fromCol, toRow, toCol))
        },
        onBackToMenu = { gameViewModel.onAction(GameAction.BackToMenu) },
        onReplayStage = { gameViewModel.onAction(GameAction.Replay) },
        onToggleMusic = { settingsViewModel.toggleMusic() }
    )
}

@Composable
private fun PlayTrackForMenu(menuState: MenuUiState, musicMuted: Boolean, player: MusicaFundoPlayer) {
    val silent = menuState.menuMusicVolumePercent <= 0 ||
        menuState.menuMusicName == StageRepository.SILENT_MUSIC_RESOURCE
    LaunchedEffect(menuState.menuMusicName, menuState.menuMusicVolumePercent, musicMuted) {
        if (musicMuted || silent) {
            player.parar()
        } else {
            player.tocarEmLoop(menuState.menuMusicName, menuState.menuMusicVolumePercent)
        }
    }
}

@Composable
private fun PlayTrackForStage(stage: StageConfig, musicMuted: Boolean, player: MusicaFundoPlayer) {
    val silent = stage.musicVolumePercent <= 0 || stage.musicName == StageRepository.SILENT_MUSIC_RESOURCE
    LaunchedEffect(stage.musicName, stage.musicVolumePercent, musicMuted) {
        if (musicMuted || silent) {
            player.parar()
        } else {
            player.tocarEmLoop(stage.musicName, stage.musicVolumePercent)
        }
    }
}
