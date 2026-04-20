package com.example.galize.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.galize.ui.screen.HomeScreen
import com.example.galize.ui.screen.SettingsScreen
import com.example.galize.ui.screen.HistoryScreen

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
