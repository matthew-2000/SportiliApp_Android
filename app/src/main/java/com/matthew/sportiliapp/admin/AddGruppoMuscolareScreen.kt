package com.matthew.sportiliapp.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.GymViewModel
import okhttp3.internal.notify
import java.util.Locale

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
                title = { Text(giorno.name) },
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
                            navController.navigate("editGruppoMuscolareScreen/${gruppiMuscolariList[index].second.nome}/${giorno.name}")
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
                    onGruppoMuscolariAdded = { gruppiList ->
                        for (newGruppo in gruppiList) {
                            gruppiMuscolariList.add("gruppo${gruppiMuscolariList.size + 1}" to newGruppo)
                            onGruppoMuscolareAdded(newGruppo)
                        }
                        showAddGruppoMuscolareDialog = false
                    },
                    giorno = giorno
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
                Text(text = gruppo.nome,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
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
    onGruppoMuscolariAdded: (List<GruppoMuscolare>) -> Unit,
    giorno: Giorno
) {
    // Lista dei gruppi muscolari disponibili
    val gruppiMuscolari = mutableListOf(
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

    for (gruppo in giorno.gruppiMuscolari) {
        gruppiMuscolari.removeAt(gruppiMuscolari.indexOf(gruppo.value.nome))
    }

    // Stato per mantenere traccia dei gruppi muscolari selezionati
    val selectedGruppi = remember { mutableStateMapOf<String, Boolean>().apply {
        gruppiMuscolari.forEach { put(it, false) }
    }}

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Gruppi Muscolari") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 350.dp)  // Imposta un'altezza massima e abilita lo scrolling
                    .verticalScroll(rememberScrollState()) // Scroll verticale
            ) {
                // Lista di Checkbox per i gruppi muscolari
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
            TextButton(
                onClick = {
                    // Crea una lista di GruppoMuscolare per quelli selezionati
                    val gruppiMuscolariSelezionati = selectedGruppi.filter { it.value }
                        .map { GruppoMuscolare(it.key) }

                    // Passa la lista dei gruppi muscolari selezionati
                    if (gruppiMuscolariSelezionati.isNotEmpty()) {
                        onGruppoMuscolariAdded(gruppiMuscolariSelezionati)
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