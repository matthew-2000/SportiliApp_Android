package com.matthew.sportiliapp

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.matthew.sportiliapp.scheda.SchedaScreen


@Composable
fun ContentScreen() {
    val navController = rememberNavController()

    // Bottom navigation items
    val items = listOf(
        BottomNavItem("Scheda", Icons.Filled.Home, "scheda"),
        BottomNavItem("Impostazioni", Icons.Filled.Settings, "impostazioni")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route)
                        }
                    )
                }
            }
        },
        content = { padding ->
            // NavHost per la navigazione tra le schede
            NavHost(
                navController = navController,
                startDestination = "scheda",
                modifier = Modifier.fillMaxSize()
                    .padding(padding),
                enterTransition = {
                    EnterTransition.None
                },
                exitTransition = {
                    ExitTransition.None
                }
            ) {
                composable("scheda") { SchedaScreen(navController = navController) }
                composable("impostazioni") { ImpostazioniScreen() }
            }
        }
    )
}


data class BottomNavItem(val title: String, val icon: ImageVector, val route: String)


@Preview(showBackground = true)
@Composable
fun ContentPreview() {
    ContentScreen()
}