package com.uk2solutions.sudoku.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.ui.game.GameScreen
import com.uk2solutions.sudoku.ui.home.HomeScreen
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.ui.theme.SudokuTheme

private const val GAME_ROUTE = "game"
private const val PREMIUM_ROUTE = "premium"
private const val LESSON_ROUTE = "lesson"

enum class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector?, @androidx.annotation.DrawableRes val iconRes: Int = 0) {
    Home("home", R.string.tab_home, Icons.Filled.Home),
    Training("training", R.string.tab_training, null, R.drawable.ic_bulb),
    Stats("stats", R.string.tab_stats, Icons.Filled.Star),
    Settings("settings", R.string.tab_settings, Icons.Filled.Settings),
}

@Composable
fun SudokuNavHost(hasPendingChallenge: Boolean = false) {
    val nav = rememberNavController()
    androidx.compose.runtime.LaunchedEffect(hasPendingChallenge) {
        if (hasPendingChallenge) nav.navigate(Tab.Home.route) { popUpTo(nav.graph.findStartDestination().id); launchSingleTop = true }
    }
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    val inGame = current?.startsWith(GAME_ROUTE) == true || current == PREMIUM_ROUTE || current?.startsWith(LESSON_ROUTE) == true
    Scaffold(
        containerColor = SudokuTheme.colors.background,
        bottomBar = {
            if (!inGame) NavigationBar(containerColor = SudokuTheme.colors.surface) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { if (tab.icon != null) Icon(tab.icon, contentDescription = null) else Icon(androidx.compose.ui.res.painterResource(tab.iconRes), contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        // 태블릿·폴더블(Android 16에서는 targetSdk 36 앱의 방향 고정이 대화면에서 무시됨)에서 내용이 늘어지지 않도록 폭을 제한한다.
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.widthIn(max = 560.dp)) {
            composable(Tab.Home.route) {
                HomeScreen(
                    onResume = { nav.navigate(GAME_ROUTE) },
                    onDaily = { nav.navigate("$GAME_ROUTE?difficulty=DAILY") },
                    onNewGame = { nav.navigate("$GAME_ROUTE?difficulty=${it.name}") },
                    onPremium = { nav.navigate(PREMIUM_ROUTE) },
                )
            }
            composable(
                "$GAME_ROUTE?difficulty={difficulty}",
                arguments = listOf(navArgument("difficulty") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                GameScreen(
                    onLearn = { id -> nav.navigate("$LESSON_ROUTE/$id") },
                    onExit = { nav.popBackStack(Tab.Home.route, inclusive = false) },
                    onNewGame = { d: Difficulty ->
                        nav.navigate("$GAME_ROUTE?difficulty=${d.name}") { popUpTo(Tab.Home.route) }
                    },
                )
            }
            composable(Tab.Training.route) {
                com.uk2solutions.sudoku.ui.training.TrainingHubScreen(onOpen = { nav.navigate("$LESSON_ROUTE/$it") }, onPremium = { nav.navigate(PREMIUM_ROUTE) })
            }
            composable("$LESSON_ROUTE/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                com.uk2solutions.sudoku.ui.training.LessonScreen(
                    onBack = { nav.popBackStack(Tab.Training.route, inclusive = false) },
                    onOpen = { id -> nav.navigate("$LESSON_ROUTE/$id") { popUpTo(Tab.Training.route) } },
                    onPremium = { nav.navigate(PREMIUM_ROUTE) },
                )
            }
            composable(Tab.Stats.route) { com.uk2solutions.sudoku.ui.stats.StatsScreen(onPremium = { nav.navigate(PREMIUM_ROUTE) }) }
            composable(Tab.Settings.route) { com.uk2solutions.sudoku.ui.settings.SettingsScreen(onPremium = { nav.navigate(PREMIUM_ROUTE) }) }
            composable(PREMIUM_ROUTE) { com.uk2solutions.sudoku.ui.premium.PremiumScreen(onBack = { nav.popBackStack() }) }
        }
        }
    }
}

