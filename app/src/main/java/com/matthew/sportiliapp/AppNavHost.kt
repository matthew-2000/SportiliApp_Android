package com.matthew.sportiliapp

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController(), startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController) }
        composable("content") { ContentScreen() }
    }
}
