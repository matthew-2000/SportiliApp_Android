package com.matthew.sportiliapp.newadmin.ui.screens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.ui.viewmodel.AlertsAdminUiState
import com.matthew.sportiliapp.newadmin.ui.viewmodel.AlertsAdminViewModel
import com.matthew.sportiliapp.newadmin.ui.viewmodel.AlertsAdminViewModelFactory
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

    var showDialog by remember { mutableStateOf(false) }
    var editingAlert by remember { mutableStateOf<Avviso?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Avvisi") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingAlert = null
                showDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nuovo avviso")
            }
        }
    ) { paddingValues ->
        when (uiState) {
            AlertsAdminUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Caricamento avvisi...")
                }
            }

            is AlertsAdminUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Errore: ${uiState.throwable.localizedMessage ?: "Sconosciuto"}")
                }
            }

            is AlertsAdminUiState.Success -> {
                val alerts = uiState.alerts
                if (alerts.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Nessun avviso disponibile")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(alerts, key = { it.id }) { alert ->
                            AlertAdminCard(
                                alert = alert,
                                onEdit = {
                                    editingAlert = alert
                                    showDialog = true
                                },
                                onDelete = { viewModel.removeAlert(alert.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertEditorSheet(
            initialAlert = editingAlert,
            onDismiss = { showDialog = false },
            onConfirm = { alert ->
                if (alert.id.isBlank()) {
                    viewModel.addAlert(alert)
                } else {
                    viewModel.updateAlert(alert)
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun AlertAdminCard(
    alert: Avviso,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(alert.titolo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    alert.scadenza?.let { deadline ->
                        val formatted = Instant.ofEpochMilli(deadline)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        Text(
                            text = "Scadenza: ${formatted.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    alert.urgenza?.takeIf { it.isNotBlank() }?.let { urgency ->
                        Text(
                            text = "Urgenza: ${urgency.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifica avviso")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Elimina avviso")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(alert.descrizione, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertEditorSheet(
    initialAlert: Avviso?,
    onDismiss: () -> Unit,
    onConfirm: (Avviso) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG) }
    var titleError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()
    LaunchedEffect(showDatePicker) {
        if (showDatePicker) {
            datePickerState.selectedDateMillis = selectedDate?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis?.let { millis ->
                            Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialAlert == null) "Nuovo avviso" else "Modifica avviso",
                style = MaterialTheme.typography.headlineSmall
            )
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                label = { Text("Titolo") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Inserisci un titolo") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                    if (descriptionError && it.isNotBlank()) descriptionError = false
                },
                label = { Text("Descrizione") },
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
                onExpandedChange = { urgencyExpanded = !urgencyExpanded }
            ) {
                OutlinedTextField(
                    value = urgency,
                    onValueChange = { urgency = it },
                    label = { Text("Urgenza") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = urgencyExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
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
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedDate?.format(dateFormatter) ?: "Nessuna scadenza")
                }
                if (selectedDate != null) {
                    TextButton(onClick = { selectedDate = null }) {
                        Text("Rimuovi scadenza")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Annulla")
                }
                Button(
                    onClick = {
                        titleError = title.isBlank()
                        descriptionError = description.isBlank()

                        if (titleError || descriptionError) return@Button

                        val normalizedUrgency = urgency.lowercase().takeIf { it.isNotBlank() }
                        val deadlineMillis = selectedDate?.atStartOfDay(zoneId)?.toInstant()?.toEpochMilli()

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
