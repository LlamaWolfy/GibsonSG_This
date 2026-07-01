package com.pulsewave.visualizer.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pulsewave.visualizer.ui.library.LibraryScreen
import com.pulsewave.visualizer.ui.player.PlayerScreen
import com.pulsewave.visualizer.ui.player.PlayerViewModel
import com.pulsewave.visualizer.ui.theme.PulseWaveTheme

private object Routes {
    const val PLAYER = "player"
    const val LIBRARY = "library"
}

@Composable
fun PulseWaveApp() {
    PulseWaveTheme {
        val navController = rememberNavController()
        val viewModel: PlayerViewModel = viewModel()

        NavHost(navController = navController, startDestination = Routes.PLAYER) {
            composable(Routes.PLAYER) {
                PlayerScreen(
                    viewModel = viewModel,
                    onOpenLibrary = { navController.navigateToLibrary() },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onTrackSelected = { navController.popBackStack() },
                )
            }
        }
    }
}

private fun NavHostController.navigateToLibrary() {
    navigate(Routes.LIBRARY)
}
