package com.example.clickerapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.clickerapp.ui.game.GameScreen
import com.example.clickerapp.ui.game.UpgradesScreen
import com.example.clickerapp.ui.info.InfoScreen
import com.example.clickerapp.ui.mining.MiningScreen
import com.example.clickerapp.ui.onlyinapp.OnlyInAppScreen
import com.example.clickerapp.ui.room.RoomScreen
import com.example.clickerapp.viewmodel.GameViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    gameViewModel: GameViewModel,
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.Game.route,
    ) {
        composable(Destinations.Game.route) {
            GameScreen(
                viewModel = gameViewModel,
                onOpenUpgrades = { navController.navigate(Destinations.Upgrades.route) },
                onOpenRoom = { navController.navigate(Destinations.Room.route) },
                onOpenMining = { navController.navigate(Destinations.Mining.route) },
            )
        }
        composable(Destinations.Upgrades.route) {
            UpgradesScreen(
                viewModel = gameViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Destinations.Room.route) {
            RoomScreen(
                viewModel = gameViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Destinations.Mining.route) {
            MiningScreen(
                viewModel = gameViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Destinations.Info.route) {
            InfoScreen(gameViewModel = gameViewModel)
        }
        composable(Destinations.OnlyInApp.route) {
            OnlyInAppScreen()
        }
    }
}

