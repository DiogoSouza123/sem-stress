package com.semstress.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.semstress.mobile.audio.BackgroundMusicPlayer
import com.semstress.mobile.audio.SfxPlayer
import com.semstress.mobile.common.MutableFeatureFlags
import com.semstress.mobile.data.StageRepository
import com.semstress.mobile.debug.DebugMenuActions
import com.semstress.mobile.debug.DebugMenuHost
import com.semstress.mobile.domain.DAILY_CHALLENGE_STAGE_ID
import com.semstress.mobile.domain.StageConfig
import com.semstress.mobile.domain.ZEN_MODE_STAGE_ID
import com.semstress.mobile.ui.navigation.GameRoute
import com.semstress.mobile.ui.navigation.MenuRoute
import com.semstress.mobile.ui.navigation.SettingsRoute
import com.semstress.mobile.ui.screens.GameScreen
import com.semstress.mobile.ui.screens.GameScreenActions
import com.semstress.mobile.ui.screens.GameScreenDebugTools
import com.semstress.mobile.ui.screens.GameScreenSound
import com.semstress.mobile.ui.screens.SettingsScreen
import com.semstress.mobile.ui.screens.SettingsScreenActions
import com.semstress.mobile.ui.screens.SettingsScreenState
import com.semstress.mobile.ui.screens.StageMenuScreen
import com.semstress.mobile.ui.screens.StageMenuScreenActions
import com.semstress.mobile.ui.screens.StageMenuScreenSound
import com.semstress.mobile.ui.state.GameAction
import com.semstress.mobile.ui.state.GameSessionSpec
import com.semstress.mobile.ui.state.GameViewModel
import com.semstress.mobile.ui.state.MenuAction
import com.semstress.mobile.ui.state.MenuUiState
import com.semstress.mobile.ui.state.MenuViewModel
import com.semstress.mobile.ui.state.SettingsViewModel
import com.semstress.mobile.ui.theme.CoffeeCrushTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val GAME_DEEP_LINK_PATTERN = "coffeecrush://game/{stageId}"

/** Bundles the app-scoped dependencies [GameDestination] needs, to stay under the parameter limit. */
private data class GameDestinationDependencies(
    val backgroundMusicPlayer: BackgroundMusicPlayer,
    val sfxPlayer: SfxPlayer,
    val debugMenuHost: DebugMenuHost,
    val featureFlags: MutableFeatureFlags
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var backgroundMusicPlayer: BackgroundMusicPlayer

    @Inject
    lateinit var sfxPlayer: SfxPlayer

    @Inject
    lateinit var debugMenuHost: DebugMenuHost

    @Inject
    lateinit var featureFlags: MutableFeatureFlags

    private val menuViewModel: MenuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { menuViewModel.uiState.value.isLoading }
        enableEdgeToEdge()
        setContent {
            CoffeeCrushTheme {
                CoffeeCrushApp(
                    GameDestinationDependencies(backgroundMusicPlayer, sfxPlayer, debugMenuHost, featureFlags)
                )
            }
        }
    }
}

@Composable
private fun CoffeeCrushApp(dependencies: GameDestinationDependencies) {
    val backgroundMusicPlayer = dependencies.backgroundMusicPlayer
    val menuViewModel: MenuViewModel = hiltViewModel()
    val menuState by menuViewModel.uiState.collectAsStateWithLifecycle()

    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        onDispose {
            backgroundMusicPlayer.release()
        }
    }

    if (menuState.isLoading) {
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = MenuRoute,
        modifier = Modifier.safeDrawingPadding()
    ) {
        composable<MenuRoute> {
            MenuDestination(navController, menuViewModel, settingsViewModel, backgroundMusicPlayer)
        }

        composable<SettingsRoute> {
            SettingsDestination(navController, settingsViewModel)
        }

        composable<GameRoute>(
            deepLinks = listOf(navDeepLink { uriPattern = GAME_DEEP_LINK_PATTERN })
        ) { backStackEntry ->
            val route: GameRoute = backStackEntry.toRoute()
            GameDestination(
                route,
                navController,
                menuState,
                settingsViewModel,
                dependencies
            )
        }
    }
}

@Composable
private fun MenuDestination(
    navController: NavController,
    menuViewModel: MenuViewModel,
    settingsViewModel: SettingsViewModel,
    backgroundMusicPlayer: BackgroundMusicPlayer
) {
    val menuState by menuViewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    // Refreshes progress every time the menu becomes the visible destination again (both the
    // explicit "back to menu" button and the system back gesture from the game screen land
    // here), since GameViewModel writes progress directly to disk.
    LaunchedEffect(Unit) {
        menuViewModel.onAction(MenuAction.ReturnToMenu)
    }

    PlayTrackForMenu(menuState, settingsState.musicMuted, backgroundMusicPlayer)

    StageMenuScreen(
        stages = menuState.stages,
        progress = menuState.progress,
        selectedStageId = menuState.selectedStageId,
        sound = StageMenuScreenSound(
            isMusicMuted = settingsState.musicMuted,
            isSfxMuted = settingsState.sfxMuted
        ),
        actions = StageMenuScreenActions(
            onSelectStage = { menuViewModel.onAction(MenuAction.SelectStage(it)) },
            onPlaySelectedStage = { navController.navigate(GameRoute(menuState.selectedStageId)) },
            onPlayZenMode = {
                navController.navigate(GameRoute(stageId = menuState.stages.first().id, zen = true))
            },
            onPlayDailyChallenge = {
                val today = java.time.LocalDate.now().toEpochDay()
                navController.navigate(GameRoute(stageId = menuState.stages.first().id, dailySeed = today))
            },
            onToggleMusic = { settingsViewModel.toggleMusic() },
            onToggleSfx = { settingsViewModel.toggleSfx() },
            onOpenSettings = { navController.navigate(SettingsRoute) }
        )
    )
}

@Composable
private fun SettingsDestination(navController: NavController, settingsViewModel: SettingsViewModel) {
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        state = SettingsScreenState(
            musicMuted = settingsState.musicMuted,
            sfxMuted = settingsState.sfxMuted,
            symbolModeEnabled = settingsState.symbolModeEnabled
        ),
        actions = SettingsScreenActions(
            onToggleMusic = { settingsViewModel.toggleMusic() },
            onToggleSfx = { settingsViewModel.toggleSfx() },
            onToggleSymbolMode = { settingsViewModel.toggleSymbolMode() },
            onBack = { navController.popBackStack() }
        )
    )
}

@Composable
private fun GameDestination(
    route: GameRoute,
    navController: NavController,
    menuState: MenuUiState,
    settingsViewModel: SettingsViewModel,
    dependencies: GameDestinationDependencies
) {
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val baseStage = menuState.stages.first { it.id == route.stageId }
    val stage = when {
        route.zen -> baseStage.copy(id = ZEN_MODE_STAGE_ID, isZenMode = true)
        route.dailySeed != null -> baseStage.copy(id = DAILY_CHALLENGE_STAGE_ID)
        else -> baseStage
    }
    val gameViewModel: GameViewModel = hiltViewModel<GameViewModel, GameViewModel.Factory>(
        creationCallback = { factory ->
            factory.create(GameSessionSpec(stage, menuState.stages.size, route.dailySeed))
        }
    )
    val gameState by gameViewModel.uiState.collectAsStateWithLifecycle()

    PlayTrackForStage(stage, settingsState.musicMuted, dependencies.backgroundMusicPlayer)

    LaunchedEffect(gameViewModel) {
        gameViewModel.backToMenuRequests.collect {
            navController.popBackStack()
        }
    }

    GameScreen(
        game = gameState,
        sound = GameScreenSound(
            isMusicMuted = settingsState.musicMuted,
            isSfxMuted = settingsState.sfxMuted,
            sfxPlayer = dependencies.sfxPlayer,
            isSymbolModeEnabled = settingsState.symbolModeEnabled
        ),
        spriteAtlas = menuState.spriteAtlas,
        actions = GameScreenActions(
            onCellTap = { row, col -> gameViewModel.onAction(GameAction.CellTapped(row, col)) },
            onCellDragSwap = { fromRow, fromCol, toRow, toCol ->
                gameViewModel.onAction(GameAction.CellDragSwapped(fromRow, fromCol, toRow, toCol))
            },
            onBackToMenu = { gameViewModel.onAction(GameAction.BackToMenu) },
            onReplayStage = { gameViewModel.onAction(GameAction.Replay) },
            onToggleMusic = { settingsViewModel.toggleMusic() },
            onToggleSfx = { settingsViewModel.toggleSfx() },
            onActivateBaristaSkill = { gameViewModel.onAction(GameAction.ActivateBaristaSkill) }
        ),
        debugTools = GameScreenDebugTools(
            host = dependencies.debugMenuHost,
            featureFlags = dependencies.featureFlags,
            actions = DebugMenuActions(
                onAddMoves = { amount -> gameViewModel.onAction(GameAction.DebugAddMoves(amount)) },
                onForceWin = { gameViewModel.onAction(GameAction.DebugForceWin) },
                onReshuffleWithSeed = { seed -> gameViewModel.onAction(GameAction.DebugReshuffleWithSeed(seed)) }
            )
        )
    )
}

@Composable
private fun PlayTrackForMenu(menuState: MenuUiState, musicMuted: Boolean, player: BackgroundMusicPlayer) {
    val silent = menuState.menuMusicVolumePercent <= 0 ||
        menuState.menuMusicName == StageRepository.SILENT_MUSIC_RESOURCE
    LaunchedEffect(menuState.menuMusicName, menuState.menuMusicVolumePercent, musicMuted) {
        if (musicMuted || silent) {
            player.stop()
        } else {
            player.playLooping(menuState.menuMusicName, menuState.menuMusicVolumePercent)
        }
    }
}

@Composable
private fun PlayTrackForStage(stage: StageConfig, musicMuted: Boolean, player: BackgroundMusicPlayer) {
    val silent = stage.musicVolumePercent <= 0 || stage.musicName == StageRepository.SILENT_MUSIC_RESOURCE
    LaunchedEffect(stage.musicName, stage.musicVolumePercent, musicMuted) {
        if (musicMuted || silent) {
            player.stop()
        } else {
            player.playLooping(stage.musicName, stage.musicVolumePercent)
        }
    }
}
