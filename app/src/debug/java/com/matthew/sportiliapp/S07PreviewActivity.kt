package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.newadmin.ui.viewmodel.UiState
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Local admin navigation and controlled results. No repository, Auth or network access. */
class S07PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen").orEmpty()
        setContent {
            SportiliAppTheme(isDarkTheme = intent.getBooleanExtra("dark", false)) {
                S07AdminFixture(screen)
            }
        }
    }
}

internal fun s07Users(): List<Utente> = previewAdminUsers() + (5..24).map {
    Utente(code = "LOCAL-$it", nome = "Utente $it", cognome = "Locale")
}

@Composable
internal fun S07AdminFixture(screen: String = "list") {
    val nav = rememberNavController()
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf(if (screen == "empty") emptyList() else s07Users()) }
    var state by remember {
        mutableStateOf<UiState<List<Utente>>>(when (screen) {
            "error" -> UiState.Error(IllegalStateException("Local failure"))
            "loading" -> UiState.Loading
            else -> UiState.Success(users)
        })
    }
    val initialRoute = when {
        screen.startsWith("edit") -> "user/SPT-042"
        screen.startsWith("new") -> "user/new"
        else -> "list"
    }
    LaunchedEffect(Unit) {
        if (initialRoute != "list") nav.navigate(initialRoute)
    }
    NavHost(navController = nav, startDestination = "list") {
        composable("list") {
            AdminUserListScreen(state, 3,
                onUserSelected = { nav.navigate("user/${it.code}") },
                onAddUser = { nav.navigate("user/new") },
                onManageAlerts = { nav.navigate("area/Avvisi") },
                onViewReports = { nav.navigate("area/Segnalazioni") },
                onRetry = { state = UiState.Success(users) })
        }
        composable("user/{code}") { entry ->
            val code = entry.arguments?.getString("code")
            val user = users.find { it.code == code }
            var pending by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            var attempts by remember { mutableIntStateOf(0) }
            fun result(action: () -> Unit) {
                pending = true
                error = null
                scope.launch {
                    delay(6000)
                    pending = false
                    attempts++
                    if (screen.contains("failure") && attempts == 1) {
                        error = "Connessione interrotta. I dati sono ancora nel form."
                    } else {
                        action()
                    }
                }
            }
            EditUserScreen(initialUser = user, isSaving = pending, errorMessage = error,
                onSave = { saved -> result {
                    users = users.filterNot { it.code == saved.code } + saved
                    state = UiState.Success(users)
                    nav.navigate("area/Utente salvato")
                } },
                onRemove = user?.let {{ result {
                    users = users.filterNot { it.code == user.code }
                    state = UiState.Success(users)
                    nav.navigate("list") { popUpTo("list") { inclusive = true } }
                } }},
                onCancel = { if (!nav.popBackStack()) nav.navigate("list") },
                onEditWorkoutCard = { nav.navigate("area/Scheda di allenamento") })
        }
        composable("area/{title}") { entry ->
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(entry.arguments?.getString("title").orEmpty(), style = MaterialTheme.typography.headlineMedium)
                Text("Destinazione locale · nessun dato remoto")
                OutlinedButton(onClick = { nav.popBackStack() }) { Text("Torna al form") }
            }
        }
    }
}
