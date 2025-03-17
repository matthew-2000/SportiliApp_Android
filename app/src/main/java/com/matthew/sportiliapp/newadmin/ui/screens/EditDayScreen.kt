package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import java.util.LinkedHashMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDayScreen(
    dayKey: String,
    day: Giorno,
    onSave: (Giorno) -> Unit,
    onCancel: () -> Unit,
    onMuscleGroupSelected: (String, GruppoMuscolare, Giorno) -> Unit
) {
    var dayName by remember { mutableStateOf(day.name) }
    // Creiamo una lista modificabile dei gruppi muscolari (preservando l'ordine)
    var groupsList by remember { mutableStateOf(day.gruppiMuscolari.toList().toMutableStateList()) }

    // Stato per il dialog per aggiungere un nuovo gruppo
    var showAddGroupDialog by remember { mutableStateOf(false) }

    // Lista dei gruppi muscolari disponibili
    val gruppiMuscolari = mutableListOf(
        "Addominali",
        "Bicipiti",
        "Cardio",
        "Defaticamento",
        "Dorsali",
        "Gambe e Glutei",
        "Polpacci",
        "Pettorali",
        "Riscaldamento",
        "Spalle",
        "Tricipiti",
        )

    for (gruppo in day.gruppiMuscolari) {
        gruppiMuscolari.removeAt(gruppiMuscolari.indexOf(gruppo.value.nome))
    }

    // Stato per mantenere traccia dei gruppi muscolari selezionati
    val selectedGruppi = remember { mutableStateMapOf<String, Boolean>().apply {
        gruppiMuscolari.forEach { put(it, false) }
    }}

    BackHandler {
        val updatedDay = day.copy(
            name = dayName,
            gruppiMuscolari = LinkedHashMap(groupsList.toMap())
        )
        onSave(updatedDay)
        onCancel()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Modifica Giorno") }) } ,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddGroupDialog = true }) {
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
            OutlinedTextField(
                value = dayName,
                onValueChange = { dayName = it },
                label = { Text("Nome del Giorno") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Gruppi Muscolari", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(groupsList) { (groupKey, muscleGroup) ->
                    MuscleGroupItem(
                        muscleGroup = muscleGroup,
                        onMoveUp = {
                            val index = groupsList.indexOfFirst { it.first == groupKey }
                            if (index > 0) {
                                val temp = groupsList[index - 1]
                                groupsList[index - 1] = groupsList[index]
                                groupsList[index] = temp
                                groupsList = groupsList.mapIndexed { i, pair -> "gruppo${i + 1}" to pair.second }
                                    .toMutableStateList()
                            }
                        },
                        onMoveDown = {
                            val index = groupsList.indexOfFirst { it.first == groupKey }
                            if (index < groupsList.size - 1) {
                                val temp = groupsList[index + 1]
                                groupsList[index + 1] = groupsList[index]
                                groupsList[index] = temp
                                groupsList = groupsList.mapIndexed { i, pair -> "gruppo${i + 1}" to pair.second }
                                    .toMutableStateList()
                            }
                        },
                        onRemove = {
                            groupsList.removeAll { it.first == groupKey }
                            groupsList = groupsList.mapIndexed { i, pair -> "gruppo${i + 1}" to pair.second }
                                .toMutableStateList()
                        },
                        onEdit = {
                            val updatedDay = day.copy(
                                name = dayName,
                                gruppiMuscolari = LinkedHashMap(groupsList.toMap())
                            )
                            onMuscleGroupSelected(groupKey, muscleGroup, updatedDay)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Annulla") }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = {
                    val updatedDay = day.copy(
                        name = dayName,
                        gruppiMuscolari = LinkedHashMap(groupsList.toMap())
                    )
                    onSave(updatedDay)
                }, modifier = Modifier.weight(1f)) { Text("Salva") }
            }
        }
    }
    if (showAddGroupDialog) {
        AlertDialog(
            onDismissRequest = { showAddGroupDialog = false },
            title = { Text("Aggiungi Gruppi Muscolare") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)  // Imposta un'altezza massima e abilita lo scrolling
                        .verticalScroll(rememberScrollState()) // Scroll verticale
                ) {
                    gruppiMuscolari.forEach { gruppo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedGruppi[gruppo] == true,
                                onCheckedChange = { isChecked ->
                                    selectedGruppi[gruppo] = isChecked
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(gruppo)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Crea una lista di GruppoMuscolare per quelli selezionati
                    val gruppiMuscolariSelezionati = selectedGruppi.filter { it.value }
                        .map { GruppoMuscolare(it.key) }
                    for (g in gruppiMuscolariSelezionati) {
                        val newKey = "gruppo${groupsList.size + 1}"
                        groupsList.add(newKey to GruppoMuscolare(nome = g.nome))
                    }
                    showAddGroupDialog = false
                }) { Text("Aggiungi") }
            },
            dismissButton = {
                TextButton(onClick = { showAddGroupDialog = false }) { Text("Annulla") }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleGroupItem(
    muscleGroup: GruppoMuscolare,
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
            text = { Text("Sei sicuro di voler rimuovere questo gruppo?") },
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
                Text(
                    text = muscleGroup.nome,
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(text = "Esercizi: ${muscleGroup.esercizi.count()}", style = MaterialTheme.typography.bodySmall)
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