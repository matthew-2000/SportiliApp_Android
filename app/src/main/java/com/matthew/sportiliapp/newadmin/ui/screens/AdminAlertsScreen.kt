package com.matthew.sportiliapp.newadmin.ui.screens
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.ui.viewmodel.AdminActionState
import com.matthew.sportiliapp.newadmin.ui.viewmodel.AlertsAdminUiState
import com.matthew.sportiliapp.newadmin.ui.viewmodel.AlertsAdminViewModel
import com.matthew.sportiliapp.newadmin.ui.viewmodel.AlertsAdminViewModelFactory
import com.matthew.sportiliapp.ui.theme.sportiliStatusColors
import java.util.Locale
import java.time.ZoneOffset
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAlertsScreen(
    onBack: () -> Unit,
    viewModel: AlertsAdminViewModel = viewModel(
        factory = AlertsAdminViewModelFactory(
            ManualInjection.getAlertsUseCase,
            ManualInjection.addAlertUseCase,
            ManualInjection.updateAlertUseCase,
            ManualInjection.removeAlertUseCase
        )
    )
) {
    val uiState = viewModel.uiState.collectAsState().value
    val actionState = viewModel.actionState.collectAsState().value

    val isSaving = actionState is AdminActionState.InProgress
    var showDialog by remember { mutableStateOf(false) }
    var editingAlert by remember { mutableStateOf<Avviso?>(null) }
    var alertPendingDeletion by remember { mutableStateOf<Avviso?>(null) }

    var failedDeletion by remember { mutableStateOf<Avviso?>(null) }
    BackHandler(enabled = isSaving) {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avvisi") },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isSaving) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isSaving) FloatingActionButton(onClick = {
                if (!isSaving) {
                    viewModel.clearActionError()
                    editingAlert = null
                    showDialog = true
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nuovo avviso")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (actionState is AdminActionState.InProgress) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (!showDialog) Text("Eliminazione in corso…", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
            }
            val actionError = (actionState as? AdminActionState.Error)?.message
            actionError?.takeIf { !showDialog }?.let { message ->
                AdminActionError(message, onRetry = failedDeletion?.let { failed ->
                    { alertPendingDeletion = failed; viewModel.clearActionError() }
                })
            }

            when (uiState) {
                AlertsAdminUiState.Loading -> AdminListState("Caricamento avvisi…", loading = true)
                is AlertsAdminUiState.Error -> AdminListState(
                    "Avvisi non disponibili", "Non è stato possibile caricare gli avvisi. Riprova.",
                    onRetry = viewModel::retryLoading
                )

                is AlertsAdminUiState.Success -> {
                    val alerts = uiState.alerts
                    if (alerts.isEmpty()) {
                        AdminListState("Nessun avviso disponibile", "Usa Nuovo avviso per pubblicare un aggiornamento.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(alerts, key = { it.id }) { alert ->
                                AlertAdminCard(
                                    alert = alert,
                                    enabled = !isSaving,
                                    onEdit = {
                                        if (isSaving) return@AlertAdminCard
                                        viewModel.clearActionError()
                                        editingAlert = alert
                                        showDialog = true
                                    },
                                    onDelete = { if (!isSaving) alertPendingDeletion = alert }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertEditorSheet(
            initialAlert = editingAlert,
            isSaving = isSaving,
            errorMessage = (actionState as? AdminActionState.Error)?.message,
            onDismiss = {
                if (!isSaving) {
                    showDialog = false
                    viewModel.clearActionError()
                }
            },
            onConfirm = { alert ->
                val onResult: (Result<Unit>) -> Unit = { result ->
                    if (result.isSuccess) showDialog = false
                }
                if (alert.id.isBlank()) {
                    viewModel.addAlert(alert, onResult)
                } else {
                    viewModel.updateAlert(alert, onResult)
                }
            }
        )
    }

    alertPendingDeletion?.let { alert ->
        AlertDialog(
            onDismissRequest = { alertPendingDeletion = null },
            title = { Text("Elimina avviso") },
            text = { Text("Vuoi eliminare l'avviso \"${alert.titolo}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        failedDeletion = alert
                        viewModel.removeAlert(alert.id) { result ->
                            if (result.isSuccess) failedDeletion = null
                        }
                        alertPendingDeletion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Elimina")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { alertPendingDeletion = null }) {
                    Text("Annulla")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AlertAdminCard(alert: Avviso, onEdit: () -> Unit, onDelete: () -> Unit, enabled: Boolean = true) {
    val status = MaterialTheme.sportiliStatusColors
    val weight = alert.urgencyWeight()
    val container = when (weight) {
        3 -> MaterialTheme.colorScheme.errorContainer
        2 -> status.warningContainer
        else -> status.infoContainer
    }
    val foreground = when (weight) {
        3 -> MaterialTheme.colorScheme.onErrorContainer
        2 -> status.onWarningContainer
        else -> status.onInfoContainer
    }
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AdminStatusLabel(when (weight) {
                3 -> "Priorità alta"
                2 -> "Priorità media"
                1 -> "Priorità bassa"
                else -> "Senza priorità"
            }, if (weight >= 2) Icons.Default.Warning else Icons.Default.Info, container, foreground)
            Text(alert.titolo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(alert.descrizione, style = MaterialTheme.typography.bodyMedium)
            val deadline = alert.scadenza?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    .format(adminAlertDateFormatter)
            }
            Text(when {
                deadline == null -> "Attivo · Nessuna scadenza"
                alert.isExpired() -> "Scaduto il $deadline"
                else -> "Attivo · Scade il $deadline"
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, enabled = enabled) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifica avviso")
                    Spacer(Modifier.width(8.dp))
                    Text("Modifica")
                }
                TextButton(onClick = onDelete, enabled = enabled) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina avviso")
                    Spacer(Modifier.width(8.dp))
                    Text("Elimina")
                }
            }
        }
    }
}

private val adminAlertDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertEditorSheet(
    initialAlert: Avviso?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: (Avviso) -> Unit
) {
    val currentSaving by rememberUpdatedState(isSaving)
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { !currentSaving || it != SheetValue.Hidden }
    )

    var title by remember(initialAlert) { mutableStateOf(initialAlert?.titolo.orEmpty()) }
    var description by remember(initialAlert) { mutableStateOf(initialAlert?.descrizione.orEmpty()) }
    var urgencyExpanded by remember { mutableStateOf(false) }
    var urgency by remember(initialAlert) { mutableStateOf(initialAlert?.urgenza?.replaceFirstChar { it.uppercase() }.orEmpty()) }
    val zoneId = remember { ZoneId.systemDefault() }
    var selectedDate by remember(initialAlert) {
        mutableStateOf(
            initialAlert?.scadenza?.let { millis ->
                Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
            }
        )
    }
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(Locale.ITALIAN) }
    var titleError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            datePickerState.selectedDateMillis = alertPickerMillis(selectedDate)
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis?.let { millis ->
                            alertPickerDate(millis)
                        }
                        showDatePicker = false
                    }
                ) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val urgencyOptions = listOf("", "Bassa", "Media", "Alta")

    val editorScrollState = rememberScrollState()

    // Material 1.3 retains the dialog's initial back callback. Recreate that dialog when
    // pending changes, while keeping the draft fields and scroll position above this key intact.
    key(isSaving) {
        ModalBottomSheet(
            onDismissRequest = { if (!isSaving) onDismiss() },
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !isSaving),
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(editorScrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (initialAlert == null) "Nuovo avviso" else "Modifica avviso",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("Titolo e descrizione sono obbligatori.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError && it.isNotBlank()) titleError = false
                    },
                    label = { Text("Titolo") },
                    enabled = !isSaving,
                    isError = titleError,
                    supportingText = if (titleError) {
                        { Text("Inserisci un titolo") }
                    } else null,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        if (descriptionError && it.isNotBlank()) descriptionError = false
                    },
                    label = { Text("Descrizione") },
                    enabled = !isSaving,
                    isError = descriptionError,
                    supportingText = if (descriptionError) {
                        { Text("Inserisci una descrizione") }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    minLines = 5,
                    maxLines = 12
                )
                ExposedDropdownMenuBox(
                    expanded = urgencyExpanded,
                    onExpandedChange = { if (!isSaving) urgencyExpanded = !urgencyExpanded }
                ) {
                    OutlinedTextField(
                        value = urgency.ifBlank { "Nessuna" },
                        onValueChange = { urgency = it },
                        label = { Text("Priorità") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = urgencyExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        enabled = !isSaving,
                        readOnly = true
                    )
                    ExposedDropdownMenu(
                        expanded = urgencyExpanded,
                        onDismissRequest = { urgencyExpanded = false }
                    ) {
                        urgencyOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(if (option.isBlank()) "Nessuna" else option) },
                                onClick = {
                                    urgency = option
                                    urgencyExpanded = false
                                }
                            )
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Scadenza", style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(
                        enabled = !isSaving,
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selectedDate?.format(dateFormatter) ?: "Nessuna scadenza")
                    }
                    if (selectedDate != null) {
                        TextButton(enabled = !isSaving, onClick = { selectedDate = null }) {
                            Text("Rimuovi scadenza")
                        }
                    }
                }
                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("Salvataggio in corso…", style = MaterialTheme.typography.bodyMedium)
                }
                errorMessage?.let { AdminActionError(it) }
                if (errorMessage != null) Text("La bozza è conservata. Premi Salva per riprovare.", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, enabled = !isSaving, modifier = Modifier.weight(1f)) {
                        Text("Annulla")
                    }
                    Button(
                        enabled = !isSaving && title.isNotBlank() && description.isNotBlank(),
                        onClick = {
                            titleError = title.isBlank()
                            descriptionError = description.isBlank()

                            if (titleError || descriptionError) return@Button

                            val normalizedUrgency = urgency.lowercase().takeIf { it.isNotBlank() }
                            val deadlineMillis = alertDeadlineMillis(selectedDate, initialAlert?.scadenza, zoneId)

                            val newAlert = Avviso(
                                id = initialAlert?.id.orEmpty(),
                                titolo = title.trim(),
                                descrizione = description.trim(),
                                urgenza = normalizedUrgency,
                                scadenza = deadlineMillis
                            )
                            onConfirm(newAlert)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salva")
                    }
                }
            }
        }
    }
}

// Material DatePicker uses UTC midnight for calendar dates. The stored deadline stays local,
// and an untouched existing timestamp is preserved exactly.
internal fun alertPickerMillis(date: LocalDate?): Long? =
    date?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()

internal fun alertPickerDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

internal fun alertDeadlineMillis(date: LocalDate?, original: Long?, zone: ZoneId): Long? {
    val originalDate = original?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
    return if (date == originalDate) original else date?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
}
