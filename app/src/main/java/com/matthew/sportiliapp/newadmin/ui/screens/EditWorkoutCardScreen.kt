package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Giorno
import java.util.Calendar
import java.util.LinkedHashMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutCardScreen(
    scheda: Scheda,
    onDaySelected: (String, Giorno, Scheda) -> Unit,
    onSave: (Scheda) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var startDate by remember { mutableStateOf(formatToDisplayDate(scheda.dataInizio)) }
    var duration by remember { mutableStateOf(scheda.durata.toString()) }
    // Manteniamo una lista modificabile dei giorni, preservando l'ordine
    var daysList by remember { mutableStateOf(scheda.giorni.toList().toMutableStateList()) }

    // Stato per mostrare il dialog per aggiungere un nuovo giorno
    var showAddDayDialog by remember { mutableStateOf(false) }
    var newDayName by remember { mutableStateOf("") }

    BackHandler {
        val updatedScheda = scheda.copy(
            dataInizio = formatToSaveDate(startDate),
            durata = duration.toIntOrNull() ?: scheda.durata,
            giorni = LinkedHashMap(daysList.toMap())
        )
        onSave(updatedScheda)
        onCancel()
    }

    // DatePickerDialog per selezionare la data
    val calendar = Calendar.getInstance()
    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = "$dayOfMonth/${month + 1}/$year"
            startDate = selectedDate
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Modifica Scheda") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDayDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            // Campo Data Inizio con DatePicker
            OutlinedTextField(
                value = startDate,
                onValueChange = {},
                label = { Text("Data Inizio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Seleziona Data")
                    }
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Durata (giorni)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Giorni di Allenamento", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(daysList) { (dayKey, giorno) ->
                    DayItem(
                        dayKey = dayKey,
                        day = giorno,
                        onMoveUp = {
                            val index = daysList.indexOfFirst { it.first == dayKey }
                            if (index > 0) {
                                val temp = daysList[index - 1]
                                daysList[index - 1] = daysList[index]
                                daysList[index] = temp
                                daysList = daysList.mapIndexed { i, pair -> "giorno${i + 1}" to pair.second }
                                    .toMutableStateList()
                            }
                        },
                        onMoveDown = {
                            val index = daysList.indexOfFirst { it.first == dayKey }
                            if (index < daysList.size - 1) {
                                val temp = daysList[index + 1]
                                daysList[index + 1] = daysList[index]
                                daysList[index] = temp
                                daysList = daysList.mapIndexed { i, pair -> "giorno${i + 1}" to pair.second }
                                    .toMutableStateList()
                            }
                        },
                        onRemove = {
                            daysList.removeAll { it.first == dayKey }
                            daysList = daysList.mapIndexed { i, pair -> "giorno${i + 1}" to pair.second }
                                .toMutableStateList()
                        },
                        onEdit = {
                            val updatedScheda = scheda.copy(
                                dataInizio = formatToSaveDate(startDate),
                                durata = duration.toIntOrNull() ?: scheda.durata,
                                giorni = LinkedHashMap(daysList.toMap())
                            )
                            onDaySelected(dayKey, giorno, updatedScheda)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCancel) { Text("Annulla") }
                Button(onClick = {
                    val updatedScheda = scheda.copy(
                        dataInizio = formatToSaveDate(startDate),
                        durata = duration.toIntOrNull() ?: scheda.durata,
                        giorni = LinkedHashMap(daysList.toMap())
                    )
                    onSave(updatedScheda)
                }) { Text("Salva") }
            }
        }

        if (showAddDayDialog) {
            AlertDialog(
                onDismissRequest = { showAddDayDialog = false },
                title = { Text("Aggiungi Giorno") },
                text = {
                    OutlinedTextField(
                        value = newDayName,
                        onValueChange = { newDayName = it },
                        label = { Text("Nome del Giorno") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val newKey = "giorno${(daysList.size + 1)}"
                        daysList.add(newKey to Giorno(newDayName))
                        newDayName = ""
                        showAddDayDialog = false
                    }) { Text("Aggiungi") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDayDialog = false }) { Text("Annulla") }
                },
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
fun DayItem(
    dayKey: String,
    day: Giorno,
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
                TextButton(onClick = {
                    showRemoveDialog = false
                    onRemove()
                }) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Annulla") }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onEdit() },
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
                Text(text = "ID: $dayKey", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onMoveUp) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = "Sposta Su")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Sposta Giù")
                }
                IconButton(onClick = { showRemoveDialog = true }) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Rimuovi")
                }
            }
        }
    }
}
