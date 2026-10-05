package com.matthew.sportiliapp.newadmin.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
        dataInizio = if (startDate == formatToDisplayDate(originalScheda.dataInizio)) originalScheda.dataInizio else formatToSaveDate(startDate),
        durata = duration.toIntOrNull() ?: originalScheda.durata,
        giorni = LinkedHashMap(daysList.toMap()),
        dayOrigins = daysList.mapNotNull { (key, day) ->
            originalScheda.giorni.entries.firstOrNull { it.value === day }?.let { key to it.key }
        }.toMap()
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutCardScreen(
    scheda: Scheda,
    userCode: String = "",
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
    var openingDayKey by rememberSaveable { mutableStateOf<String?>(null) }
    var completingChange by rememberSaveable { mutableStateOf(false) }
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
        if (isSaving) return
        showExitDialog = false
        durationError = when (val parsed = duration.toIntOrNull()) {
            null -> "Inserisci una durata valida"
            in 1..52 -> null
            else -> "La durata deve essere tra 1 e 52 settimane"
        }

        if (durationError != null) return
        onValidated(currentScheda)
    }

    fun saveCard(complete: Boolean = false) {
        validateAndSave { updated ->
            openingDayKey = null
            completingChange = complete
            onSave(if (complete) updated.copy(cambioRichiesto = false) else updated)
        }
    }

    fun openDay(key: String, day: Giorno) {
        validateAndSave { updated ->
            openingDayKey = key
            completingChange = false
            onDaySelected(key, day, updated)
        }
    }

    fun retryLastAction() {
        val dayToOpen = daysList.firstOrNull { it.first == openingDayKey }
        if (dayToOpen != null) openDay(dayToOpen.first, dayToOpen.second)
        else saveCard(complete = completingChange)
    }

    fun requestExit() {
        if (isSaving) return
        if (isDirty) {
            showExitDialog = true
        } else {
            onCancel()
        }
    }

    BackHandler {
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
            onSave = { saveCard() },
            onDiscard = {
                showExitDialog = false
                onCancel()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            AdminContextAppBar(
                title = "Modifica scheda",
                context = "Utente $userCode · Scheda",
                enabled = !isSaving,
                onBack = { requestExit() }
            )
        },
        bottomBar = {
            AdminEditorBottomBar(
                isDirty = isDirty,
                isSaving = isSaving,
                onCancel = { requestExit() },
                onSave = { saveCard() },
                onComplete = if (scheda.cambioRichiesto) ({
                    saveCard(complete = true)
                }) else null
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            item {
                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                }
                errorMessage?.let { message ->
                    AdminEditorErrorBanner(
                        message = message,
                        onRetry = { retryLastAction() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                AdminEditorSection(
                    title = "Dati scheda",
                    supportingText = "Imposta l’inizio e la durata del programma."
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = startDate,
                    onValueChange = {},
                    label = { Text("Data inizio") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSaving) { datePickerDialog.show() },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }, enabled = !isSaving) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Seleziona data"
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

                AdminEditorSection(
                    title = "Giorni di allenamento",
                    supportingText = "Aprire un giorno salva prima la bozza della scheda."
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showScheduleSheet = true },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Anteprima")
                    }
                    Button(
                        onClick = { showAddDayDialog = true },
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Aggiungi giorno")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (daysList.isEmpty()) {
                item { Text("Nessun giorno. Aggiungi il primo giorno di allenamento.", modifier = Modifier.padding(vertical = 12.dp)) }
            }
            itemsIndexed(daysList) { index, (dayKey, giorno) ->
                DayItem(
                    dayKey = dayKey,
                    day = giorno,
                    enabled = !isSaving,
                    position = index + 1,
                    total = daysList.size,
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
                    onEdit = { openDay(dayKey, giorno) }
                )
            }
            }


        if (showAddDayDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddDayDialog = false
                    newDayName = ""
                    newDayNameError = null
                },
                title = { Text("Aggiungi giorno") },
                text = {
                    OutlinedTextField(
                        value = newDayName,
                        onValueChange = {
                            newDayName = it
                            if (newDayNameError != null) newDayNameError = null
                        },
                        label = { Text("Nome del giorno") },
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
    position: Int = 1,
    total: Int = 1,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) = AdminOrderedItem(day.name, "Gruppi muscolari: ${day.gruppiMuscolari.size}",
    "giorno", position, total, enabled, onMoveUp, onMoveDown, onRemove, onEdit)
