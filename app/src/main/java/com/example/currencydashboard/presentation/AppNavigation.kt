package com.example.currencydashboard.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.currencydashboard.presentation.add.AddAssetScreen
import com.example.currencydashboard.presentation.home.HomeScreen

object Destinations {
    const val HOME = "home"
    const val ADD_ASSET = "add_asset"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                onNavigateToAddAsset = {
                    navController.navigate(Destinations.ADD_ASSET)
                }
            )
        }
        
        composable(Destinations.ADD_ASSET) {
            AddAssetScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
} 