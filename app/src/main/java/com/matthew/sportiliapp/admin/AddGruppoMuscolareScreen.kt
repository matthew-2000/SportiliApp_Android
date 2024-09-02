package com.matthew.sportiliapp.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import okhttp3.internal.notify

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGruppoMuscolareScreen(
    navController: NavController,
    giorno: Giorno,
    onGruppoMuscolareAdded: (GruppoMuscolare) -> Unit,
    onGruppoMuscolareDeleted: (index: Int) -> Unit,
    onGruppoMuscolareMoved: (oldIndex: Int, newIndex: Int) -> Unit
) {
    var showAddGruppoMuscolareDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var gruppoToDeleteIndex by remember { mutableStateOf(-1) }

    val gruppiMuscolariList = remember { giorno.gruppiMuscolari.toList().toMutableStateList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giorno ${giorno.name}") },
                actions = {
                    IconButton(onClick = {
                        showAddGruppoMuscolareDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi Gruppo Muscolare")
                    }
                }
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
            Text("Gruppi Muscolari", style = MaterialTheme.typography.headlineSmall)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn {
                itemsIndexed(gruppiMuscolariList) { index, gruppo ->
                    GruppoMuscolareItem(
                        gruppo = gruppo.second,
                        onEdit = {
                            navController.navigate("editGruppoMuscolareScreen/${gruppiMuscolariList[index].first}")
                        },
                        onMoveUp = {
                            if (index > 0) {
                                gruppiMuscolariList.move(index, index - 1)
                                onGruppoMuscolareMoved(index, index - 1)
                            }
                        },
                        onMoveDown = {
                            if (index < gruppiMuscolariList.size - 1) {
                                gruppiMuscolariList.move(index, index + 1)
                                onGruppoMuscolareMoved(index, index + 1)
                            }
                        },
                        onDelete = {
                            gruppoToDeleteIndex = index
                            showDeleteDialog = true
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                showAddGruppoMuscolareDialog = true
            }) {
                Text("Aggiungi Gruppo Muscolare")
            }

            if (showAddGruppoMuscolareDialog) {
                AddGruppoMuscolareDialog(
                    onDismiss = { showAddGruppoMuscolareDialog = false },
                    onGruppoMuscolareAdded = { newGruppo ->
                        gruppiMuscolariList.add("gruppo${gruppiMuscolariList.size + 1}" to newGruppo)
                        onGruppoMuscolareAdded(newGruppo)
                        showAddGruppoMuscolareDialog = false
                    }
                )
            }

            if (showDeleteDialog && gruppoToDeleteIndex >= 0) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Conferma Eliminazione") },
                    text = { Text("Sei sicuro di voler eliminare questo gruppo muscolare?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                gruppiMuscolariList.removeAt(gruppoToDeleteIndex)
                                onGruppoMuscolareDeleted(gruppoToDeleteIndex)
                                showDeleteDialog = false
                                gruppoToDeleteIndex = -1
                            }
                        ) {
                            Text("Elimina")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Annulla")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GruppoMuscolareItem(
    gruppo: GruppoMuscolare,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
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
                Text(text = gruppo.nome, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onMoveUp) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Sposta Su")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Sposta Giù")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Elimina Gruppo Muscolare")
                }
            }

            gruppo.esercizi.values.forEach { esercizio ->
                EsercizioItem(esercizio)
            }
        }
    }
}


@Composable
fun EsercizioItem(esercizio: Esercizio) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = esercizio.name,
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = esercizio.serie,
            style = MaterialTheme.typography.headlineSmall
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGruppoMuscolareDialog(
    onDismiss: () -> Unit,
    onGruppoMuscolareAdded: (GruppoMuscolare) -> Unit
) {
    val gruppiMuscolari = listOf(
        "Addominali",
        "Gambe e Glutei",
        "Polpacci",
        "Pettorali",
        "Spalle",
        "Dorsali",
        "Tricipiti",
        "Bicipiti",
        "Riscaldamento",
        "Defaticamento",
        "Cardio"
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedGruppo by remember { mutableStateOf(gruppiMuscolari[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Gruppo Muscolare") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    TextField(
                        value = selectedGruppo,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Gruppo Muscolare") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        gruppiMuscolari.forEach { gruppo ->
                            DropdownMenuItem(
                                text = { Text(gruppo) },
                                onClick = {
                                    selectedGruppo = gruppo
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedGruppo.isNotBlank()) {
                        onGruppoMuscolareAdded(GruppoMuscolare(selectedGruppo))
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

