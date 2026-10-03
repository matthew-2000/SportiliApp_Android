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
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.scheda.SchedaOverviewContent
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** S03 host: presentation only, no repositories, Auth, preferences or backend calls. */
class S03PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "active"
        setContent {
            SportiliAppTheme(isDarkTheme = intent.getBooleanExtra("dark", false)) {
                if (screen.startsWith("login")) S03LoginFixture(screen)
                else S03HomeFixture(screen)
            }
        }
    }
}

@Composable
internal fun S03LoginFixture(screen: String = "login") {
    var code by remember { mutableStateOf(if (screen == "login-long") "SPORTILI2026CODICEMOLTOLUNGO1234567890" else "") }
    var error by remember { mutableStateOf<String?>(if (screen == "login-error") "Il codice non è valido. Controllalo e riprova." else null) }
    var help by remember { mutableStateOf(false) }
    LoginContent(code, error, false, onCodeChange = { code = it; error = null },
        onSubmit = { error = if (code.isBlank()) "Inserisci il codice di accesso." else "Il codice non è valido. Controllalo e riprova." },
        onShowCodeInfo = { help = true })
    if (help) AlertDialog(onDismissRequest = { help = false },
        title = { Text("Codice di accesso") },
        text = { Text("Il codice viene fornito dal tuo trainer. Contattalo se non lo hai ancora ricevuto.") },
        confirmButton = { TextButton(onClick = { help = false }) { Text("Ho capito") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun S03HomeFixture(screen: String = "active") {
    val nav = rememberNavController()
    var retryCount by remember { mutableIntStateOf(0) }
    var requested by remember { mutableStateOf(screen == "requested") }
    var showCode by remember { mutableStateOf(false) }
    val workout = remember(screen) { s03Workout(screen) }
    Scaffold(bottomBar = { ContentBottomNavigation(nav) }) { outerPadding ->
        NavHost(navController = nav, startDestination = "scheda", modifier = Modifier.padding(outerPadding)) {
            composable("scheda") {
                Scaffold(topBar = { TopAppBar(title = { Text("Scheda") }) }) { padding ->
                    SchedaOverviewContent(modifier = Modifier.padding(padding), userName = "Matteo",
                        scheda = workout?.copy(cambioRichiesto = requested), isLoading = screen == "loading", isOffline = screen == "offline",
                        errorMessage = if (screen in listOf("error", "cached-error", "empty-error") && retryCount == 0) "Errore locale" else null,
                        userCode = "AB12CD", isCodeVisible = showCode, isRequesting = false, requestError = null,
                        onToggleCodeVisibility = { showCode = !showCode }, onRetry = { retryCount++ },
                        onOpenDay = { nav.navigate("giorno/$it") }, onRequestNewWorkout = { requested = true })
                }
            }
            composable("giorno/{giornoId}") { entry ->
                Column(Modifier.padding(24.dp)) {
                    Text(workout?.giorni?.get(entry.arguments?.getString("giornoId"))?.name.orEmpty())
                    Text("Dettaglio locale · ${entry.arguments?.getString("giornoId")}")
                    TextButton(onClick = { nav.popBackStack() }) { Text("Indietro") }
                }
            }
            composable("avvisi") { Text("Avvisi locali") }
            composable("impostazioni") { Text("Impostazioni locali") }
        }
    }
}

internal fun s03Workout(screen: String): Scheda? {
    if (screen in listOf("empty", "error", "loading")) return null
    val offset = when (screen) { "expired", "requested" -> -43; "expiring" -> -38; else -> -7 }
    val date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
    return Scheda(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT).format(date.time), 6,
        giorni = if (screen == "empty-error") emptyMap() else linkedMapOf(
            "first-z" to Giorno("Giorno A · Spinta", linkedMapOf("0" to GruppoMuscolare("Petto"), "1" to GruppoMuscolare("Spalle e tricipiti"))),
            "second-a" to Giorno("Giorno B · Trazione e stabilità del tronco", linkedMapOf("0" to GruppoMuscolare("Dorso e addominali")))
        ), cambioRichiesto = screen == "requested")
}
