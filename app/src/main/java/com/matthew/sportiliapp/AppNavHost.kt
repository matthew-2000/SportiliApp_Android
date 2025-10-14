package com.matthew.sportiliapp

import android.os.Build
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.newadmin.ui.navigation.AdminNavGraph
import com.matthew.sportiliapp.scheda.GiornoScreen

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController(), startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") { LoginScreen(navController) }
        composable("content") { ContentScreen(navController) }
        composable("admin") { AdminNavGraph(navController = navController) }
    }
}
