package com.matthew.sportiliapp

import android.os.Build
import android.util.Log
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.matthew.sportiliapp.admin.AdminHomeScreen
import com.matthew.sportiliapp.admin.UtenteNavHost
import com.matthew.sportiliapp.admin.UtenteScreen
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.scheda.EsercizioScreen
import com.matthew.sportiliapp.scheda.GiornoScreen
import com.matthew.sportiliapp.scheda.SchedaScreen


@Composable
fun ContentScreen(navController: NavHostController) {
    val navController2 = rememberNavController()

    // Bottom navigation items
    val items = listOf(
        BottomNavItem("Scheda", Icons.Filled.Home, "scheda"),
        BottomNavItem("Impostazioni", Icons.Filled.Settings, "impostazioni")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController2.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.background,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        ),
                        onClick = {
                            navController2.navigate(item.route) {
                                // Prevents building a large back stack
                                popUpTo(navController2.graph.startDestinationId) { saveState = true }
                                restoreState = true
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        },
        content = { padding ->
            // NavHost per la navigazione tra le schede
            NavHost(
                navController = navController2,
                startDestination = "scheda",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                composable("scheda") {
                    SchedaScreen(navController = navController2)
                }
                composable(
                    "impostazioni",
//                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
//                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) {
                    ImpostazioniScreen(navController)
                }
                composable(
                    "giorno/{giornoId}",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) { backStackEntry ->
                    val giornoId = backStackEntry.arguments?.getString("giornoId") ?: return@composable
                    GiornoScreen(navController = navController2, giornoId = giornoId)
                }
                composable(
                    "esercizio/{giornoId}/{gruppoMuscolareId}/{esercizioId}",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) { backStackEntry ->
                    val giornoId = backStackEntry.arguments?.getString("giornoId") ?: return@composable
                    val gruppoMuscolareId = backStackEntry.arguments?.getString("gruppoMuscolareId") ?: return@composable
                    val esercizioId = backStackEntry.arguments?.getString("esercizioId") ?: return@composable

                    EsercizioScreen(
                        navController = navController2,
                        giornoId = giornoId,
                        gruppoMuscolareId = gruppoMuscolareId,
                        esercizioId = esercizioId,
                    )
                }
            }

        }
    )
}


data class BottomNavItem(val title: String, val icon: ImageVector, val route: String)
