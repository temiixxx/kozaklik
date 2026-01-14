package com.example.clickerapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.clickerapp.R

@Composable
fun RootScaffold(
    navController: NavHostController,
    content: @Composable () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute !in listOf(
        Destinations.Upgrades.route,
        Destinations.Room.route,
        Destinations.Mining.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Destinations.Game.route,
                        onClick = { navController.navigateSingleTop(Destinations.Game.route) },
                        icon = { Icon(Icons.Rounded.Pets, contentDescription = null) },
                        label = { androidx.compose.material3.Text(stringResource(R.string.nav_game)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destinations.Info.route,
                        onClick = { navController.navigateSingleTop(Destinations.Info.route) },
                        icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
                        label = { androidx.compose.material3.Text(stringResource(R.string.nav_info)) },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Destinations.OnlyInApp.route,
                        onClick = { navController.navigateSingleTop(Destinations.OnlyInApp.route) },
                        icon = { Icon(Icons.Rounded.Public, contentDescription = null) },
                        label = { androidx.compose.material3.Text(stringResource(R.string.nav_only_in_app)) },
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            content()
        }
    }
}

private fun NavHostController.navigateSingleTop(route: String) {
    navigate(route) {
        launchSingleTop = true
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        restoreState = true
    }
}

