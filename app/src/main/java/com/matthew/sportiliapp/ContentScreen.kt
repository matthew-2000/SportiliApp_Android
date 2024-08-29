package com.matthew.sportiliapp

import android.os.Build
import android.util.Log
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
        BottomNavItem("Home", Icons.Filled.Home, "admin"),
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
                            selectedIconColor= MaterialTheme.colorScheme.background,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                        ),
                        onClick = {
                            navController2.navigate(item.route)
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
                    .padding(padding),
                enterTransition = {
                    EnterTransition.None
                },
                exitTransition = {
                    ExitTransition.None
                }
            ) {
                composable("scheda") { SchedaScreen(navController = navController2) }
                composable("admin") { AdminHomeScreen(navController = navController2) }
                composable("impostazioni") { ImpostazioniScreen(navController) }
                composable("giorno") {
                    val bundle = it.arguments
                    val giorno = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        bundle?.getParcelable("giorno", Giorno::class.java)
                    } else {
                        bundle?.getParcelable("giorno") as? Giorno
                    }
                    if (giorno != null) {
                        GiornoScreen(navController = navController2, giorno = giorno)
                    }
                }
                composable("esercizio") {
                    val bundle = it.arguments
                    val esercizio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        bundle?.getParcelable("esercizio", Esercizio::class.java)
                    } else {
                        bundle?.getParcelable("esercizio") as? Esercizio
                    }
                    if (esercizio != null) {
                        Log.e("AAAA", esercizio.name)
                    } else {
                        Log.e("AAAA", "NOOOOOOO")
                    }
                    if (esercizio != null) {
                        EsercizioScreen(esercizio = esercizio)
                    }
                }
                composable(
                    route = "utenteNavHost?utenteCode={utenteCode}",
                    arguments = listOf(
                        navArgument(
                            name = "utente"
                        ) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    )) {
                    val utenteCode = it.arguments?.getString("utenteCode")
                    if (utenteCode != null) {
                        UtenteNavHost(navController = navController2, utenteCode = utenteCode)
                    }
                }
            }
        }
    )
}


data class BottomNavItem(val title: String, val icon: ImageVector, val route: String)
