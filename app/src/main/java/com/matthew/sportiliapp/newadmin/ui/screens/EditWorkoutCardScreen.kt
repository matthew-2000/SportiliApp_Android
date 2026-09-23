package com.matthew.sportiliapp.newadmin.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.Scheda
import java.util.Calendar
import java.util.LinkedHashMap

internal fun buildUpdatedScheda(
    originalScheda: Scheda,
    startDate: String,
    duration: String,
    daysList: List<Pair<String, Giorno>>
): Scheda =
    originalScheda.copy(
        dataInizio = formatToSaveDate(startDate),
        durata = duration.toIntOrNull() ?: originalScheda.durata,
        giorni = LinkedHashMap(daysList.toMap())
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutCardScreen(
    scheda: Scheda,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onDaySelected: (String, Giorno, Scheda) -> Unit,
    onSave: (Scheda) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val initialDaysList = remember(scheda) { scheda.giorni.toList() }

    var startDate by rememberSaveable(scheda.dataInizio) {
        mutableStateOf(formatToDisplayDate(scheda.dataInizio))
    }
    var duration by rememberSaveable(scheda.dataInizio, scheda.durata) {
        mutableStateOf(scheda.durata.toString())
    }
    var daysList by remember(scheda) { mutableStateOf(initialDaysList) }
    var showAddDayDialog by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var newDayName by rememberSaveable { mutableStateOf("") }
    var durationError by rememberSaveable(scheda.dataInizio, scheda.durata) { mutableStateOf<String?>(null) }
    var newDayNameError by rememberSaveable { mutableStateOf<String?>(null) }

    val currentScheda = remember(scheda, startDate, duration, daysList) {
        buildUpdatedScheda(scheda, startDate, duration, daysList)
    }
    val isDirty = remember(startDate, duration, daysList, scheda) {
        startDate != formatToDisplayDate(scheda.dataInizio) ||
            duration != scheda.durata.toString() ||
            daysList != initialDaysList
    }

    fun validateAndSave(onValidated: (Scheda) -> Unit) {
        durationError = when (val parsed = duration.toIntOrNull()) {
            null -> "Inserisci una durata valida"
            in 1..52 -> null
            else -> "La durata deve essere tra 1 e 52 settimane"
        }

        if (durationError != null) return
        onValidated(currentScheda)
    }

    fun requestExit() {
        if (isSaving) return
        if (isDirty) {
            showExitDialog = true
        } else {
            onCancel()
        }
    }

    BackHandler(enabled = !isSaving) {
        requestExit()
    }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            startDate = "$dayOfMonth/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (showExitDialog) {
        UnsavedChangesDialog(
            onSave = { validateAndSave(onSave) },
            onDiscard = {
                showExitDialog = false
                onCancel()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifica Scheda") },
                actions = {
                    IconButton(onClick = { showScheduleSheet = true }, enabled = !isSaving) {
                        Icon(Icons.Default.Info, contentDescription = "Visualizza Scheda")
                    }
                    IconButton(onClick = { showAddDayDialog = true }, enabled = !isSaving) {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi Giorno")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            if (isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = startDate,
                onValueChange = {},
                label = { Text("Data Inizio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSaving) { datePickerDialog.show() },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }, enabled = !isSaving) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Seleziona Data"
                        )
                    }
                },
                enabled = !isSaving
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = duration,
                onValueChange = {
                    duration = it.filter(Char::isDigit)
                    if (durationError != null) durationError = null
                },
                label = { Text("Durata (settimane)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                isError = durationError != null,
                supportingText = durationError?.let { { Text(it) } }
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Giorni di Allenamento", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(daysList) { (dayKey, giorno) ->
                    DayItem(
                        dayKey = dayKey,
                        day = giorno,
                        enabled = !isSaving,
                        onMoveUp = {
                            val index = daysList.indexOfFirst { it.first == dayKey }
                            if (index > 0) {
                                daysList = daysList.toMutableList().apply {
                                    val previous = this[index - 1]
                                    this[index - 1] = this[index]
                                    this[index] = previous
                                }.mapIndexed { position, pair ->
                                    "giorno${position + 1}" to pair.second
                                }
                            }
                        },
                        onMoveDown = {
                            val index = daysList.indexOfFirst { it.first == dayKey }
                            if (index in 0 until daysList.lastIndex) {
                                daysList = daysList.toMutableList().apply {
                                    val next = this[index + 1]
                                    this[index + 1] = this[index]
                                    this[index] = next
                                }.mapIndexed { position, pair ->
                                    "giorno${position + 1}" to pair.second
                                }
                            }
                        },
                        onRemove = {
                            daysList = daysList
                                .filterNot { it.first == dayKey }
                                .mapIndexed { position, pair ->
                                    "giorno${position + 1}" to pair.second
                                }
                        },
                        onEdit = {
                            validateAndSave { updatedScheda ->
                                onDaySelected(dayKey, giorno, updatedScheda)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            if (scheda.cambioRichiesto) {
                OutlinedButton(
                    onClick = {
                        validateAndSave { updatedScheda ->
                            onSave(updatedScheda.copy(cambioRichiesto = false))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) { Text("Salva e segna il cambio come completato") }
                Spacer(modifier = Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { requestExit() },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) { Text("Annulla") }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { validateAndSave(onSave) },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) { Text("Salva") }
            }
        }

        if (showAddDayDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddDayDialog = false
                    newDayName = ""
                    newDayNameError = null
                },
                title = { Text("Aggiungi Giorno") },
                text = {
                    OutlinedTextField(
                        value = newDayName,
                        onValueChange = {
                            newDayName = it
                            if (newDayNameError != null) newDayNameError = null
                        },
                        label = { Text("Nome del Giorno") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = newDayNameError != null,
                        supportingText = newDayNameError?.let { { Text(it) } },
                        enabled = !isSaving
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val normalizedDayName = newDayName.trim()
                            newDayNameError = if (normalizedDayName.isBlank()) {
                                "Inserisci un nome per il giorno"
                            } else {
                                null
                            }
                            if (newDayNameError != null) return@Button

                            val newKey = "giorno${daysList.size + 1}"
                            daysList = daysList + (newKey to Giorno(normalizedDayName))
                            newDayName = ""
                            showAddDayDialog = false
                        },
                        enabled = !isSaving
                    ) { Text("Aggiungi") }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showAddDayDialog = false
                            newDayName = ""
                            newDayNameError = null
                        },
                        enabled = !isSaving
                    ) { Text("Annulla") }
                },
                shape = RoundedCornerShape(8.dp)
            )
        }

        if (showScheduleSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = { showScheduleSheet = false },
                sheetState = sheetState,
                modifier = Modifier.fillMaxWidth()
            ) {
                WorkoutCardSheet(
                    scheda = currentScheda,
                    onClose = { showScheduleSheet = false }
                )
            }
        }
    }
}

@Composable
fun DayItem(
    dayKey: String,
    day: Giorno,
    enabled: Boolean = true,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Conferma Rimozione") },
            text = { Text("Sei sicuro di voler rimuovere questo giorno?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveDialog = false
                        onRemove()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Conferma") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveDialog = false }) { Text("Annulla") }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = enabled) { onEdit() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = day.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Gruppi Muscolari: ${day.gruppiMuscolari.size}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row {
                IconButton(onClick = onMoveUp, enabled = enabled) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = "Sposta Su")
                }
                IconButton(onClick = onMoveDown, enabled = enabled) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Sposta Giù")
                }
                IconButton(onClick = { showRemoveDialog = true }, enabled = enabled) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Rimuovi")
                }
            }
        }
    }
}
