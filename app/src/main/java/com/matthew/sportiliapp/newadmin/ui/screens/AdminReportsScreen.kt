package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.activity.compose.BackHandler
import com.matthew.sportiliapp.ui.theme.sportiliStatusColors
import java.util.Locale
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.WorkoutIssueReport
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.ui.viewmodel.AdminActionState
import com.matthew.sportiliapp.newadmin.ui.viewmodel.WorkoutReportsUiState
import com.matthew.sportiliapp.newadmin.ui.viewmodel.WorkoutReportsViewModel
import com.matthew.sportiliapp.newadmin.ui.viewmodel.WorkoutReportsViewModelFactory
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    onBack: () -> Unit,
    viewModel: WorkoutReportsViewModel = viewModel(
        factory = WorkoutReportsViewModelFactory(
            ManualInjection.getWorkoutIssueReportsUseCase,
            ManualInjection.updateWorkoutIssueReportUseCase,
            ManualInjection.removeWorkoutIssueReportUseCase
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    var reportPendingDeletion by remember { mutableStateOf<WorkoutIssueReport?>(null) }

    val isSaving = actionState is AdminActionState.InProgress
    var failedReport by remember { mutableStateOf<WorkoutIssueReport?>(null) }
    var failedRemoval by remember { mutableStateOf(false) }
    BackHandler(enabled = isSaving) {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Segnalazioni") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (actionState is AdminActionState.InProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            val actionError = (actionState as? AdminActionState.Error)?.message
            if (isSaving) Text("Aggiornamento in corso…", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            actionError?.let { message ->
                AdminActionError(message, onRetry = failedReport?.let { failed ->
                    {
                        if (failedRemoval) {
                            reportPendingDeletion = failed
                            viewModel.clearActionError()
                        } else {
                            viewModel.toggleResolved(failed)
                        }
                    }
                })
            }

            when (val state = uiState) {
                WorkoutReportsUiState.Loading -> AdminListState("Caricamento segnalazioni…", loading = true)
                is WorkoutReportsUiState.Error -> AdminListState(
                    "Segnalazioni non disponibili", "Non è stato possibile caricare le segnalazioni. Riprova.",
                    onRetry = viewModel::retryLoading
                )

                is WorkoutReportsUiState.Success -> {
                    val reports = state.reports
                    if (reports.isEmpty()) {
                        AdminListState("Nessuna segnalazione ricevuta", "Qui troverai le richieste di chiarimento sulle schede.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(reports, key = { it.id }) { report ->
                                ReportCard(
                                    report = report,
                                    enabled = !isSaving,
                                    onToggleResolved = {
                                        if (!isSaving) {
                                            failedReport = report
                                            failedRemoval = false
                                            viewModel.toggleResolved(report)
                                        }
                                    },
                                    onRemove = { if (!isSaving) reportPendingDeletion = report }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    reportPendingDeletion?.let { report ->
        AlertDialog(
            onDismissRequest = { reportPendingDeletion = null },
            title = { Text("Elimina segnalazione") },
            text = {
                Text("Vuoi eliminare la segnalazione di ${report.userName.ifBlank { report.userCode }}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        failedReport = report
                        failedRemoval = true
                        viewModel.removeReport(report.id)
                        reportPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { reportPendingDeletion = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ReportCard(
    report: WorkoutIssueReport,
    onToggleResolved: () -> Unit,
    onRemove: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val status = MaterialTheme.sportiliStatusColors
            AdminStatusLabel(if (report.resolved) "Risolta" else "Da rivedere",
                if (report.resolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                if (report.resolved) status.successContainer else status.warningContainer,
                if (report.resolved) status.onSuccessContainer else status.onWarningContainer)
            Text(report.userName.ifBlank { report.userCode }, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text("Codice ${report.userCode}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(report.message, style = MaterialTheme.typography.bodyMedium)

            Text(
                text = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.ITALIAN)
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(report.createdAt)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onToggleResolved, enabled = enabled) {
                    Text(if (report.resolved) "Segna come da rivedere" else "Segna come risolta")
                }
                OutlinedButton(onClick = onRemove, enabled = enabled) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Elimina")
                }
            }
        }
    }
}
