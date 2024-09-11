package com.matthew.sportiliapp.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GymViewModel
import com.matthew.sportiliapp.model.Scheda
import java.text.SimpleDateFormat
import java.util.*

fun formatToDisplayDate(dateString: String): String {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(parser.parse(dateString) ?: Date())
}

fun formatToSaveDate(dateString: String): String {
    val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
    return formatter.format(parser.parse(dateString) ?: Date())
}

fun getCurrentFormattedDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
    return dateFormat.format(Date())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSchedaScreen(
    navController: NavController,
    gymViewModel: GymViewModel,
    utenteCode: String,
    onDismiss: () -> Unit
) {
    val users by gymViewModel.users.observeAsState(emptyList())
    val utente = users.firstOrNull { it.code == utenteCode } ?: return
    val scheda = utente.scheda ?: Scheda(dataInizio = getCurrentFormattedDate(), 0)
    if (scheda.giorni.isEmpty()) {
        val updatedGiorni = scheda.giorni.toMutableMap()
        updatedGiorni["giorno1"] = Giorno("A")
        updatedGiorni["giorno2"] = Giorno("B")
        updatedGiorni["giorno3"] = Giorno("C")
        scheda.giorni = updatedGiorni
    }

    var dataInizio by remember { mutableStateOf(formatToDisplayDate(scheda.dataInizio)) }
    var durata by remember { mutableStateOf(scheda.durata.toString()) }
    var showAddGiornoDialog by remember { mutableStateOf(false) }
    var showRenameGiornoDialog by remember { mutableStateOf(false) }
    val giorniList = remember { scheda.giorni.toList().toMutableStateList() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var giornoToDeleteIndex by remember { mutableStateOf(-1) }
    var giornoToRenameIndex by remember { mutableStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifica Scheda") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // DatePicker Field per la Data Inizio
            val context = LocalContext.current
            val calendar = Calendar.getInstance()
            val datePickerDialog = android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedDate = "$dayOfMonth/${month + 1}/$year"
                    dataInizio = selectedDate
                    updateScheda(giorniList, gymViewModel, utenteCode, dataInizio, durata)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            OutlinedTextField(
                value = dataInizio,
                onValueChange = {},
                label = { Text("Data Inizio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = durata,
                onValueChange = {
                    durata = it
                    updateScheda(giorniList, gymViewModel, utenteCode, dataInizio, durata)
                },
                label = { Text("Durata (settimane)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // UI per i giorni della scheda
            Text("Giorni della Scheda", style = MaterialTheme.typography.headlineSmall)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            giorniList.forEachIndexed { index, (dayName, giorno) ->
                GiornoItem(
                    giorno = giorno,
                    onEdit = { navController.navigate("addGruppoMuscolareScreen/${giorniList[index].first}") },
                    onMoveUp = {
                        if (index > 0) {
                            giorniList.move(index, index - 1)
                            updateScheda(giorniList, gymViewModel, utenteCode, dataInizio, durata)
                        }
                    },
                    onMoveDown = {
                        if (index < giorniList.size - 1) {
                            giorniList.move(index, index + 1)
                            updateScheda(giorniList, gymViewModel, utenteCode, dataInizio, durata)
                        }
                    },
                    onDelete = {
                        giornoToDeleteIndex = index
                        showDeleteDialog = true
                    },
                    onNameEdit = {
                        giornoToRenameIndex = index
                        showRenameGiornoDialog = true
                    }
                )
            }

            if (showDeleteDialog && giornoToDeleteIndex >= 0) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Conferma Eliminazione") },
                    text = { Text("Sei sicuro di voler eliminare questo giorno?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                giorniList.removeAt(giornoToDeleteIndex)
                                updateScheda(giorniList, gymViewModel, utenteCode, dataInizio, durata)
                                showDeleteDialog = false
                                giornoToDeleteIndex = -1
                            }
                        ) {
                            Text("Elimina", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Annulla")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                showAddGiornoDialog = true
            }) {
                Text("Aggiungi Giorno")
            }

            if (showAddGiornoDialog) {
                AddGiornoDialog(
                    onDismiss = { showAddGiornoDialog = false },
                    onGiornoAdded = { newGiorno ->
                        giorniList.add("giorno${giorniList.size + 1}" to newGiorno)
                        updateScheda(giorniList, gymViewModel, utenteCode, dataInizio, durata)
                        showAddGiornoDialog = false
                    }
                )
            }

            if (showRenameGiornoDialog) {
                RenameGiornoDialog(giorno = giorniList[giornoToRenameIndex].second,
                    onDismiss = { showRenameGiornoDialog = false },
                    onGiornoRenamed = { newGiornoName ->
                        giorniList[giornoToRenameIndex].second.name = newGiornoName
                        updateScheda(giorniList, gymViewModel, utenteCode, dataInizio, durata)
                        showRenameGiornoDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun GiornoItem(
    giorno: Giorno,
    onEdit: () -> Unit,
    onNameEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) } // Stato per mostrare o nascondere il menu a tendina

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onEdit),
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = giorno.name, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.weight(1f))

                // Menu a comparsa
                IconButton(onClick = { expanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Opzioni")
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sposta Su") },
                        onClick = {
                            onMoveUp()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Sposta Giù") },
                        onClick = {
                            onMoveDown()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Modifica nome") },
                        onClick = {
                            onNameEdit()
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Elimina Giorno") },
                        onClick = {
                            onDelete()
                            expanded = false
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Gruppi Muscolari: ${giorno.gruppiMuscolari.size}", style = MaterialTheme.typography.headlineSmall)
        }
    }
}


fun <T> MutableList<T>.move(fromIndex: Int, toIndex: Int) {
    val item = removeAt(fromIndex)
    add(toIndex, item)
}

fun updateScheda(
    giorniList: List<Pair<String, Giorno>>,
    gymViewModel: GymViewModel,
    utenteCode: String,
    dataInizio: String,
    durata: String
) {
    val durataInt = durata.toIntOrNull()
    if (durataInt != null) {
        val formattedDataInizio = formatToSaveDate(dataInizio)
        val updatedGiorni = giorniList.mapIndexed { index, entry -> "giorno${index + 1}" to entry.second }.toMap()
        val updatedScheda = Scheda(formattedDataInizio, durataInt, updatedGiorni)
        gymViewModel.saveScheda(updatedScheda, utenteCode)
    }
}

@Composable
fun AddGiornoDialog(
    onDismiss: () -> Unit,
    onGiornoAdded: (Giorno) -> Unit
) {
    var giornoName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Giorno") },
        text = {
            OutlinedTextField(
                value = giornoName,
                onValueChange = { giornoName = it },
                label = { Text("Nome del Giorno") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (giornoName.isNotBlank()) {
                        onGiornoAdded(Giorno(giornoName))
                    }
                }
            ) {
                Text("Aggiungi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun RenameGiornoDialog(giorno: Giorno ,onDismiss: () -> Unit, onGiornoRenamed: (String) -> Unit) {
    var nome by remember { mutableStateOf(giorno.name) }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Rinomina Giorno") },
        text = {
            Column {
                TextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome giorno") })
            }
        },
        confirmButton = {
            Button(onClick = {
                giorno.name = nome
                onGiornoRenamed(nome)
            }) {
                Text("Salva")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Annulla")
            }
        }
    )
}
