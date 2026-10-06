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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.avvisi.AvvisiScreen
import androidx.compose.ui.platform.LocalContext
import com.matthew.sportiliapp.model.SchedaViewModel
import com.matthew.sportiliapp.model.SchedaViewModelFactory
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.scheda.ExerciseDetailActions
import com.matthew.sportiliapp.scheda.EsercizioScreen
import com.matthew.sportiliapp.scheda.GiornoScreen
import com.matthew.sportiliapp.scheda.SchedaScreen


@Composable
fun ContentScreen(navController: NavHostController) {
    val workoutViewModel: SchedaViewModel = viewModel(
        factory = SchedaViewModelFactory(LocalContext.current.applicationContext)
    )

    ContentNavigation(
        workoutViewModel,
        overview = { SchedaScreen(navController = it, viewModel = workoutViewModel) },
        alerts = { AvvisiScreen() },
        settings = { ImpostazioniScreen(navController) }
    )
}

/** The production shell and routes; local QA supplies content and in-memory writes. */
@Composable
internal fun ContentNavigation(
    workoutViewModel: SchedaViewModel,
    overview: @Composable (NavHostController) -> Unit,
    alerts: @Composable () -> Unit,
    settings: @Composable () -> Unit,
    exerciseActions: ExerciseDetailActions? = null
) {
    val navController2 = rememberNavController()
    Scaffold(
        bottomBar = { ContentBottomNavigation(navController2) },
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
                    overview(navController2)
                }
                composable("avvisi") {
                    alerts()
                }
                composable(
                    "impostazioni",
//                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
//                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) {
                    settings()
                }
                composable(
                    "giorno/{giornoId}",
                    enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
                    exitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
                ) { backStackEntry ->
                    val giornoId = backStackEntry.arguments?.getString("giornoId") ?: return@composable
                    GiornoScreen(navController = navController2, giornoId = giornoId, viewModel = workoutViewModel)
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
                        viewModel = workoutViewModel,
                        actions = exerciseActions,
                    )
                }
            }

        }
    )
}


data class BottomNavItem(val title: String, val icon: ImageVector, val route: String)

/** Detail routes belong to Scheda; match route segments rather than arbitrary prefixes. */
internal fun bottomNavigationRoute(currentRoute: String?): String? =
    when (currentRoute?.substringBefore('/')) {
        "scheda", "giorno", "esercizio" -> "scheda"
        "avvisi" -> "avvisi"
        "impostazioni" -> "impostazioni"
        else -> null
    }

@Composable
internal fun ContentBottomNavigation(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Scheda", Icons.Filled.Home, "scheda"),
        BottomNavItem("Avvisi", Icons.Filled.Notifications, "avvisi"),
        BottomNavItem("Impostazioni", Icons.Filled.Settings, "impostazioni")
    )
    val entry by navController.currentBackStackEntryAsState()
    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = null) },
                label = { Text(item.title, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center) },
                selected = bottomNavigationRoute(entry?.destination?.route) == item.route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.background,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary
                ),
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        restoreState = true
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
