package com.matthew.sportiliapp.scheda

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.SchedaViewModel
import com.matthew.sportiliapp.model.WorkoutIssueReport
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import com.matthew.sportiliapp.ui.theme.sportiliStatusColors
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedaScreen(navController: NavHostController, viewModel: SchedaViewModel) {
    val context = LocalContext.current
    val loadError by viewModel.loadError.observeAsState()
    val scheda by viewModel.scheda.observeAsState()
    val nomeUtente by viewModel.name.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(true)
    val isOfflineMode by viewModel.isOfflineMode.observeAsState(false)
    val userCode = viewModel.getCurrentUserCode().orEmpty()

    var showReportDialog by remember { mutableStateOf(false) }
    var showOverflow by remember { mutableStateOf(false) }
    var isCodeVisible by rememberSaveable { mutableStateOf(false) }
    var reportMessage by remember { mutableStateOf("") }
    var isSubmittingReport by remember { mutableStateOf(false) }
    var reportError by remember { mutableStateOf<String?>(null) }
    var isRequesting by remember { mutableStateOf(false) }
    var requestError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val submitReportUseCase = remember { ManualInjection.submitWorkoutIssueReportUseCase }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scheda") },
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = !isLoading) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Aggiorna scheda")
                    }
                    Box {
                        IconButton(onClick = { showOverflow = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Altre azioni")
                        }
                        DropdownMenu(
                            expanded = showOverflow,
                            onDismissRequest = { showOverflow = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Segnala un problema") },
                                leadingIcon = { Icon(Icons.Filled.Warning, contentDescription = null) },
                                onClick = {
                                    showOverflow = false
                                    reportError = null
                                    showReportDialog = true
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        SchedaOverviewContent(
            modifier = Modifier.padding(padding),
            userName = nomeUtente,
            scheda = scheda,
            isLoading = isLoading,
            isOffline = isOfflineMode,
            errorMessage = loadError,
            userCode = userCode,
            isCodeVisible = isCodeVisible,
            isRequesting = isRequesting,
            requestError = requestError,
            onToggleCodeVisibility = { isCodeVisible = !isCodeVisible },
            onRetry = viewModel::refresh,
            onOpenDay = { navController.navigate("giorno/$it") },
            onRequestNewWorkout = {
                if (!isRequesting) {
                    isRequesting = true
                    requestError = null
                    viewModel.inviaRichiestaCambioScheda(
                        onSuccess = {
                            isRequesting = false
                        },
                        onError = {
                            isRequesting = false
                            requestError = "Non riusciamo a inviare la richiesta. Controlla la connessione e riprova."
                        }
                    )
                }
            }
        )
    }

    if (showReportDialog) {
        ReportProblemDialog(
            message = reportMessage,
            onMessageChange = {
                reportMessage = it
                if (!reportError.isNullOrEmpty()) reportError = null
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
                    reportError = "Codice utente non disponibile. Effettua di nuovo l'accesso."
                    return@ReportProblemDialog
                }
                isSubmittingReport = true
                coroutineScope.launch {
                    val result = submitReportUseCase(
                        WorkoutIssueReport(
                            userCode = code,
                            userName = nomeUtente.orEmpty(),
                            message = trimmed
                        )
                    )
                    isSubmittingReport = false
                    if (result.isSuccess) {
                        Toast.makeText(context, "Segnalazione inviata", Toast.LENGTH_SHORT).show()
                        showReportDialog = false
                        reportMessage = ""
                        reportError = null
                    } else {
                        reportError = "Invio non riuscito. Controlla la connessione e riprova."
                    }
                }
            }
        )
    }
}

@Composable
internal fun SchedaOverviewContent(
    modifier: Modifier = Modifier,
    userName: String?,
    scheda: Scheda?,
    isLoading: Boolean,
    isOffline: Boolean,
    errorMessage: String?,
    userCode: String,
    isCodeVisible: Boolean,
    isRequesting: Boolean,
    requestError: String?,
    onToggleCodeVisibility: () -> Unit,
    onRetry: () -> Unit,
    onOpenDay: (String) -> Unit,
    onRequestNewWorkout: () -> Unit
) {
    when {
        isLoading && scheda == null -> LoadingState(modifier)
        errorMessage != null && scheda == null -> MessageState(
            modifier = modifier,
            title = "Non riusciamo a caricare la scheda",
            message = "Controlla la connessione e riprova.",
            icon = Icons.Filled.Warning,
            onRetry = onRetry
        )
        scheda == null || scheda.giorni.isEmpty() -> EmptyState(
            modifier = modifier,
            isOffline = isOffline,
            userCode = userCode,
            isCodeVisible = isCodeVisible,
            onToggleCodeVisibility = onToggleCodeVisibility,
            onRetry = onRetry
        )
        else -> WorkoutOverview(
            modifier = modifier,
            userName = userName,
            scheda = scheda,
            isOffline = isOffline,
            errorMessage = errorMessage,
            userCode = userCode,
            isCodeVisible = isCodeVisible,
            isRequesting = isRequesting,
            requestError = requestError,
            onToggleCodeVisibility = onToggleCodeVisibility,
            onRetry = onRetry,
            onOpenDay = onOpenDay,
            onRequestNewWorkout = onRequestNewWorkout
        )
    }
}

@Composable
private fun WorkoutOverview(
    modifier: Modifier,
    userName: String?,
    scheda: Scheda,
    isOffline: Boolean,
    errorMessage: String?,
    userCode: String,
    isCodeVisible: Boolean,
    isRequesting: Boolean,
    requestError: String?,
    onToggleCodeVisibility: () -> Unit,
    onRetry: () -> Unit,
    onOpenDay: (String) -> Unit,
    onRequestNewWorkout: () -> Unit
) {
    val days = scheda.giorni.entries.toList()
    val status = workoutStatus(scheda)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!userName.isNullOrBlank()) {
            item {
                Text(
                    text = "Ciao, $userName",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
        if (isOffline) {
            item { OfflineBanner() }
        } else if (errorMessage != null) {
            item { InlineError(onRetry) }
        }
        item { WorkoutSummary(scheda) }
        item {
            WorkoutStatusCard(
                status = status,
                remainingTime = scheda.tempoRimanente(),
                isRequesting = isRequesting,
                requestError = requestError,
                onPrimaryAction = when (status) {
                    WorkoutStatus.Expired -> onRequestNewWorkout
                    WorkoutStatus.Active, WorkoutStatus.Expiring -> {
                        {
                            days.firstOrNull()?.key?.let(onOpenDay)
                            Unit
                        }
                    }
                    WorkoutStatus.Requested -> null
                }
            )
        }
        item {
            Text(
                text = "Allenamenti",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
        }
        items(days, key = { it.key }) { (key, day) ->
            GiornoItem(day) { onOpenDay(key) }
        }
        item {
            UserCodeCard(userCode, isCodeVisible, onToggleCodeVisibility)
        }
    }
}

@Composable
private fun WorkoutSummary(scheda: Scheda) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SummaryItem("Data di inizio", formatWorkoutDate(scheda.dataInizio), Modifier.weight(1f))
            SummaryItem(
                "Durata",
                "${scheda.durata} ${if (scheda.durata == 1) "settimana" else "settimane"}",
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

internal enum class WorkoutStatus { Active, Expiring, Expired, Requested }

internal fun workoutStatus(scheda: Scheda, now: Date = Date()): WorkoutStatus {
    if (scheda.cambioRichiesto) return WorkoutStatus.Requested
    val endDate = workoutEndDate(scheda) ?: return WorkoutStatus.Expired
    if (!now.before(endDate)) return WorkoutStatus.Expired
    val warningStart = Calendar.getInstance().apply {
        time = endDate
        add(Calendar.DAY_OF_YEAR, -7)
    }.time
    return if (!now.before(warningStart)) WorkoutStatus.Expiring else WorkoutStatus.Active
}

private data class StatusVisuals(
    val title: String,
    val message: String,
    val action: String?,
    val icon: ImageVector,
    val container: Color,
    val foreground: Color
)

@Composable
private fun statusVisuals(status: WorkoutStatus): StatusVisuals {
    val colors = MaterialTheme.sportiliStatusColors
    return when (status) {
        WorkoutStatus.Active -> StatusVisuals(
            "Scheda attiva", "Continua dal prossimo allenamento.", "Apri allenamento",
            Icons.Filled.CheckCircle, colors.successContainer, colors.onSuccessContainer
        )
        WorkoutStatus.Expiring -> StatusVisuals(
            "Scheda in scadenza", "Termina a breve.", "Apri allenamento",
            Icons.Filled.Warning, colors.warningContainer, colors.onWarningContainer
        )
        WorkoutStatus.Expired -> StatusVisuals(
            "Scheda terminata", "Chiedi al trainer una nuova scheda.", "Richiedi nuova scheda",
            Icons.Filled.Warning, MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        WorkoutStatus.Requested -> StatusVisuals(
            "Richiesta inviata", "Il trainer ha ricevuto la richiesta.", null,
            Icons.AutoMirrored.Filled.Send, colors.infoContainer, colors.onInfoContainer
        )
    }
}

@Composable
private fun WorkoutStatusCard(
    status: WorkoutStatus,
    remainingTime: String,
    isRequesting: Boolean,
    requestError: String?,
    onPrimaryAction: (() -> Unit)?
) {
    val visuals = statusVisuals(status)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = visuals.container,
        contentColor = visuals.foreground
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.semantics(mergeDescendants = true) { heading() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(visuals.icon, contentDescription = null)
                Text(visuals.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(visuals.message, style = MaterialTheme.typography.bodyMedium)
            if (status == WorkoutStatus.Active || status == WorkoutStatus.Expiring) {
                Text(remainingTime, style = MaterialTheme.typography.bodySmall)
            }
            if (visuals.action != null && onPrimaryAction != null) {
                Button(
                    onClick = onPrimaryAction,
                    enabled = !isRequesting,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                ) {
                    if (isRequesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isRequesting) "Invio in corso…" else visuals.action)
                }
            }
            if (!requestError.isNullOrBlank()) {
                Text(requestError, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.semantics { contentDescription = "Caricamento scheda" }
        ) {
            CircularProgressIndicator()
            Text("Caricamento scheda…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MessageState(
    modifier: Modifier,
    title: String,
    message: String,
    icon: ImageVector,
    onRetry: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Riprova") }
            }
        }
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier,
    isOffline: Boolean,
    userCode: String,
    isCodeVisible: Boolean,
    onToggleCodeVisibility: () -> Unit,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    if (isOffline) Icons.Filled.Warning else Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    if (isOffline) "Sei offline" else "Nessuna scheda disponibile",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    if (isOffline) "Connettiti per caricare la scheda."
                    else "Quando il trainer ne assegnerà una, la troverai qui.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Riprova") }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        UserCodeCard(userCode, isCodeVisible, onToggleCodeVisibility)
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null)
            Text("Sei offline. Mostriamo gli ultimi dati disponibili.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun InlineError(onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Text(
                "Aggiornamento non riuscito. I dati mostrati potrebbero non essere recenti.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp)
            )
            TextButton(onClick = onRetry) { Text("Riprova") }
        }
    }
}

@Composable
private fun UserCodeCard(code: String, isCodeVisible: Boolean, onToggleCodeVisibility: () -> Unit) {
    val hasCode = code.isNotBlank()
    val shownCode = when {
        !hasCode -> "Codice non disponibile"
        isCodeVisible -> code
        else -> "••••••"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Il tuo codice", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(shownCode, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = onToggleCodeVisibility, enabled = hasCode) {
                Text(if (isCodeVisible) "Nascondi" else "Mostra")
            }
        }
    }
}

@Composable
fun GiornoItem(giorno: Giorno, onClick: () -> Unit) {
    val groups = getGruppiString(giorno)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(giorno.name)
                    if (groups.isNotBlank()) append(". $groups")
                    append(". Apri allenamento")
                }
            },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(giorno.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (groups.isNotBlank()) {
                    Text(groups, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("Apri allenamento", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                    "Descrivi cosa non funziona nella tua scheda. Il trainer riceverà la segnalazione.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    label = { Text("Messaggio") },
                    isError = !errorMessage.isNullOrBlank(),
                    supportingText = errorMessage?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (!isSubmitting) onSubmit() }, enabled = !isSubmitting) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Invia")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSubmitting) { Text("Annulla") } }
    )
}

private fun workoutEndDate(scheda: Scheda): Date? {
    val start = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT).parse(scheda.dataInizio)
    } catch (_: Exception) {
        null
    } ?: return null
    return Calendar.getInstance().apply {
        time = start
        add(Calendar.WEEK_OF_YEAR, scheda.durata)
    }.time
}

internal fun formatWorkoutDate(value: String, locale: Locale = Locale.ITALIAN): String {
    val date = try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ROOT).parse(value)
    } catch (_: Exception) {
        null
    } ?: return "Data non disponibile"
    return SimpleDateFormat("d MMM yyyy", locale).format(date)
}

fun getGruppiString(giorno: Giorno): String = giorno.gruppiMuscolari.values.joinToString(", ") { it.nome }

private val previewDay = Giorno(
    name = "Giorno A · Spinta",
    gruppiMuscolari = linkedMapOf(
        "0" to GruppoMuscolare("Petto"),
        "1" to GruppoMuscolare("Spalle"),
        "2" to GruppoMuscolare("Tricipiti")
    )
)

private fun previewWorkout(expired: Boolean, requested: Boolean = false) = Scheda(
    dataInizio = if (expired) "2020-01-01T08:00:00+0100" else "2099-01-01T08:00:00+0100",
    durata = 6,
    giorni = linkedMapOf(
        "giorno-a" to previewDay,
        "giorno-b" to previewDay.copy(name = "Giorno B · Trazione")
    ),
    cambioRichiesto = requested
)

@Composable
private fun WorkoutPreview(scheda: Scheda?, error: String? = null, dark: Boolean = false) {
    SportiliAppTheme(isDarkTheme = dark) {
        Surface {
            SchedaOverviewContent(
                userName = "Matteo", scheda = scheda, isLoading = false, isOffline = false,
                errorMessage = error, userCode = "AB12CD", isCodeVisible = false,
                isRequesting = false, requestError = null, onToggleCodeVisibility = {},
                onRetry = {}, onOpenDay = {}, onRequestNewWorkout = {}
            )
        }
    }
}

@Preview(name = "Scheda attiva", showBackground = true, widthDp = 360, heightDp = 760)
@Composable private fun ActiveWorkoutPreview() = WorkoutPreview(previewWorkout(expired = false))

@Preview(
    name = "Scheda attiva font 1,6x",
    showBackground = true,
    widthDp = 360,
    heightDp = 760,
    fontScale = 1.6f
)
@Composable private fun ActiveWorkoutLargeFontPreview() = WorkoutPreview(previewWorkout(expired = false))

@Preview(name = "Scheda terminata", showBackground = true, widthDp = 360, heightDp = 760)
@Composable private fun ExpiredWorkoutPreview() = WorkoutPreview(previewWorkout(expired = true))

@Preview(name = "Richiesta inviata dark", showBackground = true, widthDp = 360, heightDp = 760)
@Composable private fun RequestedWorkoutPreview() = WorkoutPreview(previewWorkout(expired = true, requested = true), dark = true)

@Preview(name = "Scheda vuota", showBackground = true, widthDp = 360, heightDp = 760)
@Composable private fun EmptyWorkoutPreview() = WorkoutPreview(null)

@Preview(name = "Errore scheda", showBackground = true, widthDp = 360, heightDp = 760)
@Composable private fun ErrorWorkoutPreview() = WorkoutPreview(null, error = "errore")

@Preview(name = "Scheda offline", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun OfflineWorkoutPreview() {
    SportiliAppTheme {
        Surface {
            SchedaOverviewContent(
                userName = "Matteo", scheda = previewWorkout(expired = false), isLoading = false,
                isOffline = true, errorMessage = null, userCode = "AB12CD", isCodeVisible = false,
                isRequesting = false, requestError = null, onToggleCodeVisibility = {},
                onRetry = {}, onOpenDay = {}, onRequestNewWorkout = {}
            )
        }
    }
}
