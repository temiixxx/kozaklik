package com.example.clickerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.example.clickerapp.ui.navigation.AppNavGraph
import com.example.clickerapp.ui.navigation.RootScaffold
import com.example.clickerapp.ui.theme.ClickerAppTheme
import com.example.clickerapp.viewmodel.GameViewModel
import com.example.clickerapp.viewmodel.GameViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = (application as ClickerAppApplication).repository
        setContent {
            val navController = rememberNavController()
            val gameViewModel: GameViewModel = viewModel(factory = GameViewModelFactory(repository))
            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> gameViewModel.onAppForegrounded()
                        Lifecycle.Event.ON_STOP -> gameViewModel.onAppBackgrounded()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            ClickerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RootScaffold(navController = navController) {
                        AppNavGraph(
                            navController = navController,
                            gameViewModel = gameViewModel,
                        )
                    }
                }
            }
        }
    }
}
