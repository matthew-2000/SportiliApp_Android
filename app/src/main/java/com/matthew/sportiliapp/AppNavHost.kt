package com.matthew.sportiliapp

import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.matthew.sportiliapp.admin.AdminHomeScreen
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.scheda.GiornoScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController(), startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController) }
        composable("content") { ContentScreen(navController) }
        composable("admin") { AdminHomeScreen(navController) }
    }
}
