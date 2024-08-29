package com.matthew.sportiliapp.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onGruppoMuscolareAdded: (GruppoMuscolare) -> Unit
) {
    var showAddGruppoMuscolareDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gruppi Muscolari - ${giorno.name}") },
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

            giorno.gruppiMuscolari.forEach { gruppo ->
                GruppoMuscolareItem(gruppo = gruppo.value) {
                    navController.navigate("addEsercizioScreen/${gruppo.value.nome}")
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
                        onGruppoMuscolareAdded(newGruppo)
                        showAddGruppoMuscolareDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun GruppoMuscolareItem(gruppo: GruppoMuscolare, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onEdit),
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text = gruppo.nome, style = MaterialTheme.typography.headlineMedium)

            // Visualizzazione degli esercizi
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
            text = "Serie: ${esercizio.serie}",
            style = MaterialTheme.typography.headlineSmall
        )
        Divider(modifier = Modifier.padding(vertical = 8.dp))
    }
}


@Composable
fun AddGruppoMuscolareDialog(
    onDismiss: () -> Unit,
    onGruppoMuscolareAdded: (GruppoMuscolare) -> Unit
) {
    var gruppoName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aggiungi Gruppo Muscolare") },
        text = {
            OutlinedTextField(
                value = gruppoName,
                onValueChange = { gruppoName = it },
                label = { Text("Nome Gruppo Muscolare") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (gruppoName.isNotBlank()) {
                        onGruppoMuscolareAdded(GruppoMuscolare(gruppoName))
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
