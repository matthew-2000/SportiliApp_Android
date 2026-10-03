package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.newadmin.domain.*
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.newadmin.ui.viewmodel.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.delay

/** S02 fixture host. No production repositories, preferences, authentication or network calls. */
class S02PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen").orEmpty()
        val repository = S02FixtureRepository().apply {
            alertsFlow.value = listOf(s02Alert)
            reportsFlow.value = listOf(s02Report, s02Report.copy(
                id = "report-resolved", userName = "Luca Rossi", resolved = true,
                message = "Recupero del giorno B chiarito."
            ))
            saveAlert = {
                delay(8000)
                if (screen == "alerts-failure" && alertWrites == 1) {
                    Result.failure(IllegalStateException("Connessione interrotta. Riprova il salvataggio."))
                } else Result.success(Unit)
            }
        }
        setContent {
            SportiliAppTheme(isDarkTheme = intent.getBooleanExtra("dark", false)) {
                Surface {
                    when (screen) {
                        "circuit" -> EditMuscleGroupContent(
                            "FIXTURE", "day-a", GruppoMuscolare(nome = "Circuito"),
                            s02Catalog, onSave = {}, onCancel = {}
                        )
                        "alerts-success", "alerts-failure" -> AdminAlertsScreen(
                            onBack = {},
                            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                factory = AlertsAdminViewModelFactory(
                                    GetAlertsUseCase(repository), AddAlertUseCase(repository),
                                    UpdateAlertUseCase(repository), RemoveAlertUseCase(repository)
                                )
                            )
                        )
                        "reports" -> AdminReportsScreen(
                            onBack = {},
                            viewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                                factory = WorkoutReportsViewModelFactory(
                                    GetWorkoutIssueReportsUseCase(repository),
                                    UpdateWorkoutIssueReportUseCase(repository),
                                    RemoveWorkoutIssueReportUseCase(repository)
                                )
                            )
                        )
                        else -> S02NavigationFixture()
                    }
                }
            }
        }
    }
}

internal val s02Alert = Avviso(
    id = "fixture-existing", titolo = "Orari di ottobre",
    descrizione = "Sabato la palestra apre alle 9. Buon allenamento!", urgenza = "media",
    scadenza = java.time.LocalDate.of(2026, 10, 11)
        .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
)
internal val s02Report = WorkoutIssueReport(
    id = "report-open", userCode = "AB12CD", userName = "Matteo Bianchi",
    message = "Nella scheda del giorno A manca il tempo di recupero della panca.",
    createdAt = 1791014400000L
)
internal val s02Catalog = listOf(
    GruppoMuscolarePredefinito("petto", "Pettorali", listOf(
        EsercizioPredefinito("panca", "Panca piana", ""),
        EsercizioPredefinito("croci", "Croci ai cavi", ""),
        EsercizioPredefinito("inclinata", "Panca inclinata", "")
    )),
    GruppoMuscolarePredefinito("abs", "Addominali", listOf(
        EsercizioPredefinito("crunch", "Crunch", "")
    )),
    GruppoMuscolarePredefinito("gambe", "Gambe e Glutei", listOf(
        EsercizioPredefinito("squat", "Squat", "")
    ))
)

/** Real bottom navigation and route patterns, with local bodies to isolate tab/back behavior. */
@Composable
internal fun S02NavigationFixture() {
    val nav = rememberNavController()
    Scaffold(bottomBar = { ContentBottomNavigation(nav) }) { padding ->
        NavHost(navController = nav, startDestination = "scheda", modifier = Modifier.padding(padding)) {
            composable("scheda") {
                S02NavigationBody("La tua scheda", "Giorno A · Spinta") { nav.navigate("giorno/a") }
            }
            composable("giorno/{giornoId}") {
                S02NavigationBody("Giorno A · Spinta", "Panca piana", onBack = { nav.popBackStack() }) {
                    nav.navigate("esercizio/a/petto/panca")
                }
            }
            composable("esercizio/{giornoId}/{gruppoMuscolareId}/{esercizioId}") {
                S02NavigationBody("Panca piana", "3 × 10 · Recupero 1 minuto", onBack = { nav.popBackStack() }) {}
            }
            composable("avvisi") { S02NavigationBody("Avvisi", "Nessun avviso") {} }
            composable("impostazioni") { S02NavigationBody("Impostazioni", "Codice FIXTURE") {} }
        }
    }
}

@Composable
private fun S02NavigationBody(title: String, action: String, onBack: (() -> Unit)? = null, onOpen: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Fixture locale · navigazione", style = MaterialTheme.typography.labelMedium)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Button(onClick = onOpen) { Text(action) }
        onBack?.let { TextButton(onClick = it) { Text("Indietro") } }
    }
}
