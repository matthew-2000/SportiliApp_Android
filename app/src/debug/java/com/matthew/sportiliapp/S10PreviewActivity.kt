package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.remember
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.newadmin.domain.*
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.newadmin.ui.viewmodel.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll

/** DEBUG host using the production screens and ViewModels with in-memory repository only. */
class S10PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "alerts"
        val scenario = intent.getStringExtra("scenario") ?: "success"
        val repository = S02FixtureRepository().apply {
            if (scenario != "empty") {
                alertsFlow.value = s10Alerts
                reportsFlow.value = s10Reports
            }
            var alertLoads = 0
            var reportLoads = 0
            observeAlerts = { flow {
                if (scenario == "loading") awaitCancellation()
                if (scenario == "load-error" && ++alertLoads == 1) error("Raw Firebase details")
                emitAll(alertsFlow)
            } }
            observeReports = { flow {
                if (scenario == "loading") awaitCancellation()
                if (scenario == "load-error" && ++reportLoads == 1) error("Raw Firebase details")
                emitAll(reportsFlow)
            } }
            saveAlert = {
                delay(3000)
                if (scenario == "write-error" && alertWrites == 1) Result.failure(IllegalStateException("Raw write error"))
                else Result.success(Unit)
            }
            deleteAlert = {
                delay(3000)
                if (scenario == "write-error" && alertRemovals == 1) Result.failure(IllegalStateException("Raw delete error"))
                else Result.success(Unit)
            }
            saveReport = {
                delay(3000)
                if (scenario == "write-error" && reportWrites == 1) Result.failure(IllegalStateException("Raw update error"))
                else Result.success(Unit)
            }
            deleteReport = {
                delay(3000)
                if (scenario == "write-error" && reportRemovals == 1) Result.failure(IllegalStateException("Raw delete error"))
                else Result.success(Unit)
            }
        }
        setContent {
            SportiliAppTheme(isDarkTheme = intent.getBooleanExtra("dark", false)) {
                Surface {
                    if (screen == "reports") {
                        val vm = remember { WorkoutReportsViewModel(GetWorkoutIssueReportsUseCase(repository),
                            UpdateWorkoutIssueReportUseCase(repository), RemoveWorkoutIssueReportUseCase(repository)) }
                        AdminReportsScreen(onBack = { finish() }, viewModel = vm)
                    } else {
                        val vm = remember { AlertsAdminViewModel(GetAlertsUseCase(repository),
                            AddAlertUseCase(repository), UpdateAlertUseCase(repository), RemoveAlertUseCase(repository)) }
                        AdminAlertsScreen(onBack = { finish() }, viewModel = vm)
                    }
                }
            }
        }
    }
}

internal val s10Alerts = listOf(
    s02Alert.copy(id = "high", titolo = "Chiusura straordinaria della sala pesi per manutenzione",
        descrizione = "Venerdì la sala pesi chiude alle 19. Gli altri corsi si svolgeranno regolarmente.", urgenza = "alta"),
    s02Alert.copy(id = "medium", titolo = "Orari del corso mobility", urgenza = "media"),
    s02Alert.copy(id = "low", titolo = "Porta un asciugamano", descrizione = "Ricorda di portare un asciugamano per l'allenamento.", urgenza = "bassa", scadenza = null),
    s02Alert.copy(id = "expired", titolo = "Orario precedente", urgenza = null, scadenza = 1L)
)
internal val s10Reports = listOf(
    s02Report.copy(userName = "Alessandro Giovanni Bianchi", resolutionNote = "Nota mantenuta"),
    s02Report.copy(id = "resolved", userName = "Luca Rossi", resolved = true, message = "Recupero del giorno B chiarito.")
)
