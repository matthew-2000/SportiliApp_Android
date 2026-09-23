package com.matthew.sportiliapp.scheda
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
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
    val userCode = viewModel.getCurrentUserCode().orEmpty()

    var showReportDialog by remember { mutableStateOf(false) }
    var isCodeVisible by rememberSaveable { mutableStateOf(false) }
    var reportMessage by remember { mutableStateOf("") }
    var isSubmittingReport by remember { mutableStateOf(false) }
    var reportError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val submitReportUseCase = remember { ManualInjection.submitWorkoutIssueReportUseCase }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(getTitle(nomeUtente)) },
                actions = {
                    TextButton(onClick = {
                        reportError = null
                        showReportDialog = true
                    }) {
                        Text("Segnala")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        content = { padding ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Caricamento scheda...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // Mostra la scheda o la schermata "non disponibile"
                if (scheda == null || scheda!!.giorni.isEmpty()) {
                    SchedaNonDisponibileScreen(
                        isOffline = isOfflineMode,
                        userCode = userCode,
                        isCodeVisible = isCodeVisible,
                        onToggleCodeVisibility = { isCodeVisible = !isCodeVisible }
                    )
                } else {
                    val currentScheda = scheda!!
                    val isExpired = !currentScheda.isSchedaValida()
                    val canRequestCambio = isExpired

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

                        item {
                            Text(
                                text = currentScheda.tempoRimanente(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (isExpired) {
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
                                        text = "⚠️ Scheda scaduta",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )

                                    val statusMessage = if (currentScheda.cambioRichiesto) {
                                        "Hai già richiesto una nuova scheda. Attendi il caricamento da parte del personal trainer."
                                    } else {
                                        "La tua scheda è scaduta. Puoi inviare la richiesta di cambio."
                                    }
                                    Text(
                                        text = statusMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = Color.Gray,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )

                                    Text(
                                        text = "Le modifiche delle schede vengono gestite nel weekend, indicativamente il sabato. Per agevolare il cambio, invia la richiesta tra le 20:00 di venerdì e le 10:00 di sabato; durante la settimana l'aggiornamento potrebbe non essere effettuato.",
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
                                        enabled = canRequestCambio && !currentScheda.cambioRichiesto,
                                        modifier = Modifier
                                            .wrapContentWidth()
                                            .height(40.dp)
                                    ) {
                                        Text(
                                            text = if (currentScheda.cambioRichiesto) "Richiesta inviata" else "Richiedi nuova scheda",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
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
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                thickness = 1.dp,
                                color = Color.LightGray
                            )                        }
                        item {
                            UserCodeCard(
                                code = userCode,
                                isCodeVisible = isCodeVisible,
                                onToggleCodeVisibility = { isCodeVisible = !isCodeVisible }
                            )
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
fun SchedaNonDisponibileScreen(
    isOffline: Boolean,
    userCode: String,
    isCodeVisible: Boolean,
    onToggleCodeVisibility: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
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
        if (isOffline) {
            Text(
                text = "Quando tornerai online aggiorneremo automaticamente queste informazioni.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        UserCodeCard(
            code = userCode,
            isCodeVisible = isCodeVisible,
            onToggleCodeVisibility = onToggleCodeVisibility
        )
    }
}

@Composable
private fun UserCodeCard(
    code: String,
    isCodeVisible: Boolean,
    onToggleCodeVisibility: () -> Unit
) {
    val hasCode = code.isNotBlank()
    val shownCode = when {
        !hasCode -> "Codice non disponibile"
        isCodeVisible -> code
        else -> "••••••"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Il tuo codice",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = shownCode,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            TextButton(
                onClick = onToggleCodeVisibility,
                enabled = hasCode
            ) {
                Text(if (isCodeVisible) "Nascondi" else "Mostra")
            }
        }
    }
}



@Composable
fun GiornoItem(giorno: Giorno, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = giorno.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = getGruppiString(giorno),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Apri giorno ${giorno.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
