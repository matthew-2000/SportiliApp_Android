package com.matthew.sportiliapp.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GymViewModel
import com.matthew.sportiliapp.model.Scheda
import java.util.Calendar

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
    val scheda = utente.scheda ?: Scheda("", 0)
    if (scheda.giorni.isEmpty()) {
        val updatedGiorni = scheda.giorni.toMutableMap()
        updatedGiorni["giorno1"] = Giorno("A")
        updatedGiorni["giorno2"] = Giorno("B")
        updatedGiorni["giorno3"] = Giorno("C")
        scheda.giorni = updatedGiorni
    }

    var dataInizio by remember { mutableStateOf(scheda.dataInizio) }
    var durata by remember { mutableStateOf(scheda.durata.toString()) }
    var showAddGiornoDialog by remember { mutableStateOf(false) }
    val giorniList = remember { scheda.giorni.toList().toMutableStateList() }

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
                    dataInizio = "$dayOfMonth/${month + 1}/$year"
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
                onValueChange = { durata = it },
                label = { Text("Durata (giorni)") },
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
                    onEdit = { navController.navigate("addGruppoMuscolareScreen/$dayName") },
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
        }
    }
}

@Composable
fun GiornoItem(giorno: Giorno, onEdit: () -> Unit, onMoveUp: () -> Unit, onMoveDown: () -> Unit) {
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
                IconButton(onClick = onMoveUp) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Sposta Su")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Sposta Giù")
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
    val updatedGiorni = giorniList.mapIndexed { index, entry -> "giorno${index + 1}" to entry.second }.toMap()
    val updatedScheda = Scheda(dataInizio, durata.toInt(), updatedGiorni)
    gymViewModel.saveScheda(updatedScheda, utenteCode)
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