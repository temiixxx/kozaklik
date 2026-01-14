package com.example.clickerapp.ui.navigation

sealed class Destinations(val route: String) {
    data object Game : Destinations("game")
    data object Upgrades : Destinations("upgrades")
    data object Room : Destinations("room")
    data object Mining : Destinations("mining")
    data object Info : Destinations("info")
    data object OnlyInApp : Destinations("only_in_app")
}

