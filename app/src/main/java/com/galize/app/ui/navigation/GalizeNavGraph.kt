package com.galize.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.galize.app.ui.screen.HomeScreen
import com.galize.app.ui.screen.SettingsScreen
import com.galize.app.ui.screen.HistoryScreen

@Composable
fun GalizeNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToHistory = { navController.navigate("history") }
            )
        }
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable("history") {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
    }
}
