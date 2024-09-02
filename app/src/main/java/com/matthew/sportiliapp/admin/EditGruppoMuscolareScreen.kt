package com.matthew.sportiliapp.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGruppoMuscolareScreen(
    navController: NavController,
    gruppoMuscolare: GruppoMuscolare,
    onEsercizioAdded: (Esercizio) -> Unit,
    onEsercizioMoved: (oldIndex: Int, newIndex: Int) -> Unit,
    onEsercizioDeleted: (index: Int) -> Unit,
    onEsercizioEdited: (index: Int, esercizio: Esercizio) -> Unit
) {
    var showAddEsercizioDialog by remember { mutableStateOf(false) }
    var showEditEsercizioDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var esercizioToDeleteIndex by remember { mutableStateOf(-1) }
    var esercizioToEditIndex by remember { mutableStateOf(-1) }
    val eserciziList = remember { gruppoMuscolare.esercizi.toList().toMutableStateList() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(gruppoMuscolare.nome) },
                actions = {
                    IconButton(onClick = { showAddEsercizioDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi Esercizio")
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
            Text("Esercizi", style = MaterialTheme.typography.headlineSmall)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn {
                itemsIndexed(eserciziList) { index, esercizio ->
                    EsercizioItem(
                        esercizio = esercizio.second,
                        onEdit = {
                            showEditEsercizioDialog = true
                            esercizioToEditIndex = index
                        },
                        onMoveUp = {
                            if (index > 0) {
                                eserciziList.move(index, index - 1)
                                onEsercizioMoved(index, index - 1)
                            }
                        },
                        onMoveDown = {
                            if (index < eserciziList.size - 1) {
                                eserciziList.move(index, index + 1)
                                onEsercizioMoved(index, index + 1)
                            }
                        },
                        onDelete = {
                            esercizioToDeleteIndex = index
                            showDeleteDialog = true
                        }
                    )
                }
            }

            if (showEditEsercizioDialog && esercizioToEditIndex>=0) {
                EditEsercizioDialog(
                    esercizio = eserciziList.get(esercizioToEditIndex).second,
                    onDismiss = { showEditEsercizioDialog = false },
                    onEsercizioEdited = { newEsercizio ->
                        eserciziList.add("esercizio${eserciziList.size + 1}" to newEsercizio)
                        onEsercizioAdded(newEsercizio)
                        showAddEsercizioDialog = false
                    }
                )
            }

            if (showAddEsercizioDialog) {
                AddEsercizioDialog(
                    onDismiss = { showAddEsercizioDialog = false },
                    onEsercizioAdded = { newEsercizio ->
                        eserciziList.add("esercizio${eserciziList.size + 1}" to newEsercizio)
                        onEsercizioAdded(newEsercizio)
                        showAddEsercizioDialog = false
                    }
                )
            }

            if (showDeleteDialog && esercizioToDeleteIndex >= 0) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Conferma Eliminazione") },
                    text = { Text("Sei sicuro di voler eliminare questo esercizio?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                eserciziList.removeAt(esercizioToDeleteIndex)
                                onEsercizioDeleted(esercizioToDeleteIndex)
                                showDeleteDialog = false
                                esercizioToDeleteIndex = -1
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
fun EsercizioItem(
    esercizio: Esercizio,
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
                Text(text = esercizio.name, style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onMoveUp) {
                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Sposta Su")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Sposta Giù")
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Elimina Esercizio")
                }
            }

            Text(text = esercizio.serie, style = MaterialTheme.typography.headlineSmall)
            if (esercizio.riposo?.isNotEmpty() == true) {
                Text(text = "Riposo: ${esercizio.riposo}", style = MaterialTheme.typography.headlineSmall)
            }
            if (esercizio.notePT?.isNotEmpty() == true) {
                Text(text = "Note: ${esercizio.notePT}", style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
fun AddEsercizioDialog(
    onDismiss: () -> Unit,
    onEsercizioAdded: (Esercizio) -> Unit
) {
    var esercizioName by remember { mutableStateOf("") }
    var serie by remember { mutableStateOf(3) }
    var ripetizioni by remember { mutableStateOf(10) }
    var serieDescrizione by remember { mutableStateOf("") }
    var riposo by remember { mutableStateOf("") }
    var notePT by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Esercizio") },
        text = {
            Column {
                OutlinedTextField(
                    value = esercizioName,
                    onValueChange = { esercizioName = it },
                    label = { Text("Nome Esercizio") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (serieDescrizione.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Stepper(
                        value = serie,
                        onValueChange = { serie = it },
                        range = 1..30,
                        label = "Serie"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Stepper(
                        value = ripetizioni,
                        onValueChange = { ripetizioni = it },
                        range = 1..50,
                        label = "Ripetizioni"
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = serieDescrizione,
                    onValueChange = { serieDescrizione = it },
                    label = { Text("Serie o minuti") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = riposo,
                    onValueChange = { riposo = it },
                    label = { Text("Recupero") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notePT,
                    onValueChange = { notePT = it },
                    label = { Text("Note PT") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newEsercizio = Esercizio(
                        name = esercizioName,
                        serie = "$serie x $ripetizioni",
                        riposo = riposo,
                        notePT = notePT
                    )
                    onEsercizioAdded(newEsercizio)
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
fun EditEsercizioDialog(
    esercizio: Esercizio,
    onDismiss: () -> Unit,
    onEsercizioEdited: (Esercizio) -> Unit
) {
    var esercizioName by remember { mutableStateOf(esercizio.name) }
    var serie by remember { mutableStateOf<Int?>(3) }
    var ripetizioni by remember { mutableStateOf<Int?>(10) }
    var serieDescrizione by remember { mutableStateOf("") }
    var riposo by remember { mutableStateOf("") }
    var notePT by remember { mutableStateOf("") }

    // Analisi della serie
    val (parsedSerie, parsedRipetizioni, parsedDescrizione) = parseSerie(esercizio.serie)
    serie = parsedSerie
    ripetizioni = parsedRipetizioni
    serieDescrizione = parsedDescrizione

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifica Esercizio") },
        text = {
            Column {
                OutlinedTextField(
                    value = esercizioName,
                    onValueChange = { esercizioName = it },
                    label = { Text("Nome Esercizio") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (serieDescrizione.isEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    serie?.let {
                        Stepper(
                            value = it,
                            onValueChange = { serie = it },
                            range = 1..30,
                            label = "Serie"
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ripetizioni?.let {
                        Stepper(
                            value = it,
                            onValueChange = { ripetizioni = it },
                            range = 1..50,
                            label = "Ripetizioni"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = serieDescrizione,
                    onValueChange = { serieDescrizione = it },
                    label = { Text("Serie o minuti") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = riposo,
                    onValueChange = { riposo = it },
                    label = { Text("Recupero") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notePT,
                    onValueChange = { notePT = it },
                    label = { Text("Note PT") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newEsercizio = Esercizio(
                        name = esercizioName,
                        serie = "$serie x $ripetizioni",
                        riposo = riposo,
                        notePT = notePT
                    )
                    onEsercizioEdited(newEsercizio)
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

fun parseSerie(serie: String): Triple<Int?, Int?, String> {
    // Controlla il formato NxN
    val partsNxN = serie.split("x", "X", " x ", " X ")
    if (partsNxN.size == 2) {
        val serieValue = partsNxN.getOrNull(0)?.toIntOrNull()
        val ripetizioniValue = partsNxN.getOrNull(1)?.toIntOrNull()
        if (serieValue != null && ripetizioniValue != null) {
            return Triple(serieValue, ripetizioniValue, "")
        }
    }
    return Triple(null, null, serie)
}