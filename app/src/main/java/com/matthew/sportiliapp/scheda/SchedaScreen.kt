package com.matthew.sportiliapp.scheda
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.WorkoutIssueReport
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.SchedaViewModel
import com.matthew.sportiliapp.model.SchedaViewModelFactory
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedaScreen(navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: SchedaViewModel = viewModel(factory = SchedaViewModelFactory(context))

    val scheda by viewModel.scheda.observeAsState()
    val nomeUtente by viewModel.name.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(true) // Osserviamo lo stato di caricamento
    val isOfflineMode by viewModel.isOfflineMode.observeAsState(false)

    var showReportDialog by remember { mutableStateOf(false) }
    var reportMessage by remember { mutableStateOf("") }
    var isSubmittingReport by remember { mutableStateOf(false) }
    var reportError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val submitReportUseCase = remember { ManualInjection.submitWorkoutIssueReportUseCase }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getTitle(nomeUtente)) }
            )
        },
        content = { padding ->
            if (isLoading) {
                // Mostra l'indicatore di caricamento
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    //CircularProgressIndicator()
                }
            } else {
                // Mostra la scheda o la schermata "non disponibile"
                if (scheda == null || scheda!!.giorni.isEmpty()) {
                    SchedaNonDisponibileScreen(isOfflineMode) {
                        reportError = null
                        showReportDialog = true
                    }
                } else {
                    val currentScheda = scheda!!
                    val isExpired = !currentScheda.isSchedaValida()
                    val weeksLeft = currentScheda.getSettimaneMancanti()
                    val canRequestCambio = isExpired || weeksLeft <= 1

                    LazyColumn(
                        modifier = Modifier
                            .padding(padding)
                            .padding(all = 16.dp)
                    ) {
                        if (isOfflineMode) {
                            item {
                                OfflineBanner()
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                            ) {
                                Text(
                                    "Inizio: ${convertDateTime(currentScheda.dataInizio)} ",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "x${currentScheda.durata} sett.",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (canRequestCambio) {
                            item {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    thickness = 1.dp,
                                    color = Color.LightGray
                                )
                            }
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp)
                                        .padding(horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = if (isExpired) "⚠️ Scheda scaduta!" else "⏳ Scheda in scadenza",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    if (currentScheda.cambioRichiesto) {
                                        Text(
                                            text = "Hai già richiesto una nuova scheda. Attendi che il personal trainer la carichi.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(bottom = 8.dp)
                                        )
                                        Button(
                                            onClick = { /* disabilitato */ },
                                            enabled = false,
                                            modifier = Modifier.wrapContentWidth().height(40.dp)
                                        ) {
                                            Text(
                                                text = "Richiesta inviata",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    } else {
                                        val infoMessage = when {
                                            isExpired -> "Richiedi un aggiornamento al personal trainer."
                                            weeksLeft <= 0 -> "Manca meno di una settimana alla scadenza. Puoi richiedere subito un cambio scheda."
                                            else -> "Manca 1 settimana alla scadenza. Puoi già richiedere un cambio scheda."
                                        }
                                        Text(
                                            text = infoMessage,
                                            style = MaterialTheme.typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                            color = Color.Gray,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )
                                        Button(
                                            onClick = {
                                                viewModel.inviaRichiestaCambioScheda(
                                                    onSuccess = {
                                                        Toast.makeText(context, "Richiesta inviata!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onError = { e ->
                                                        Toast.makeText(context, "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                )
                                            },
                                            modifier = Modifier.wrapContentWidth().height(40.dp)
                                        ) {
                                            Text(
                                                text = "Richiedi nuova scheda",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (!isExpired && weeksLeft > 1) {
                            item {
                                Text(
                                    "$weeksLeft settimane rimanenti",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        item {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                thickness = 1.dp,
                                color = Color.LightGray
                            )
                        }

                        items(currentScheda.giorni.entries.toList()) { (key, giorno) ->
                            GiornoItem(giorno) {
                                navController.navigate("giorno/$key")
                            }
                        }
                        item {
                            ReportProblemSection(onClick = {
                                reportError = null
                                showReportDialog = true
                            })
                        }
                    }
                }
            }
        }
    )

    if (showReportDialog) {
        ReportProblemDialog(
            message = reportMessage,
            onMessageChange = {
                reportMessage = it
                if (!reportError.isNullOrEmpty()) {
                    reportError = null
                }
            },
            isSubmitting = isSubmittingReport,
            errorMessage = reportError,
            onDismiss = {
                if (!isSubmittingReport) {
                    showReportDialog = false
                    reportMessage = ""
                    reportError = null
                }
            },
            onSubmit = {
                val trimmed = reportMessage.trim()
                if (trimmed.isEmpty()) {
                    reportError = "Inserisci una descrizione del problema"
                    return@ReportProblemDialog
                }
                val code = viewModel.getCurrentUserCode()
                if (code.isNullOrBlank()) {
                    Toast.makeText(context, "Codice utente non disponibile", Toast.LENGTH_SHORT).show()
                    return@ReportProblemDialog
                }
                isSubmittingReport = true
                coroutineScope.launch {
                    val report = WorkoutIssueReport(
                        userCode = code,
                        userName = nomeUtente.orEmpty(),
                        message = trimmed
                    )
                    val result = submitReportUseCase(report)
                    isSubmittingReport = false
                    if (result.isSuccess) {
                        Toast.makeText(context, "Segnalazione inviata", Toast.LENGTH_SHORT).show()
                        showReportDialog = false
                        reportMessage = ""
                        reportError = null
                    } else {
                        reportError = result.exceptionOrNull()?.localizedMessage
                            ?: "Invio non riuscito"
                    }
                }
            }
        )
    }
}

@Composable
private fun OfflineBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Modalità offline attiva. Stai visualizzando l'ultima scheda salvata sul dispositivo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SchedaNonDisponibileScreen(isOffline: Boolean, onReportClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isOffline) {
            OfflineBanner()
        }
        Text(
            text = "La tua scheda di allenamento non è ancora disponibile.",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Il personal trainer deve ancora caricare la tua scheda. Ti preghiamo di attendere o contattare il personal trainer per ulteriori informazioni.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        OutlinedButton(onClick = onReportClick) {
            Text("Segnala un problema")
        }
        if (isOffline) {
            Text(
                text = "Quando tornerai online aggiorneremo automaticamente queste informazioni.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
    }
}



@Composable
fun GiornoItem(giorno: Giorno, onClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
        .padding(vertical = 16.dp)) {
        Text(giorno.name, style = MaterialTheme.typography.titleMedium)
        Text(getGruppiString(giorno), style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        Divider(color = Color.LightGray, thickness = 1.dp)
    }
}

@Composable
private fun ReportProblemSection(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedButton(onClick = onClick) {
            Text("Segnala un problema")
        }
    }
}

@Composable
private fun ReportProblemDialog(
    message: String,
    onMessageChange: (String) -> Unit,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Segnala un problema") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Descrivi cosa non funziona nella tua scheda. Il personal trainer riceverà la segnalazione.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    label = { Text("Messaggio") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!isSubmitting) onSubmit() }, enabled = !isSubmitting) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Invia")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSubmitting) {
                Text("Annulla")
            }
        }
    )
}

fun getTitle(nomeUtente: String?): String {
    return if (nomeUtente != null) {
        "Ciao $nomeUtente"
    } else {
        "Home"
    }
}

fun convertDateTime(inputDateTime: String): String {
    // Formato di input della data e dell'ora
    val formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH)
    // Formato di output desiderato per la data
    val formatterOutput = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ENGLISH)

    // Parsing della stringa di input nella classe LocalDateTime
    val localDateTime = LocalDateTime.parse(inputDateTime, formatterInput)

    // Formattazione della data secondo il formato desiderato
    return localDateTime.format(formatterOutput)
}

fun getGruppiString(giorno: Giorno): String {
    return giorno.gruppiMuscolari.values.joinToString(", ") { it.nome }
}
