package com.matthew.sportiliapp.admin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.GruppoMuscolare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEsercizioScreen(
    navController: NavController,
    gruppoMuscolare: GruppoMuscolare,
    onEsercizioAdded: (Esercizio) -> Unit
) {
    var esercizioName by remember { mutableStateOf("") }
    var serie by remember { mutableStateOf("") }
    var priorita by remember { mutableStateOf("") }
    var riposo by remember { mutableStateOf("") }
    var notePT by remember { mutableStateOf("") }
    var noteUtente by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aggiungi Esercizio") },
                actions = {
                    IconButton(onClick = {
                        if (esercizioName.isNotEmpty() && serie.isNotEmpty()) {
                            val newEsercizio = Esercizio(
                                name = esercizioName,
                                serie = serie,
                                priorita = priorita.toIntOrNull(),
                                riposo = riposo,
                                notePT = notePT,
                                noteUtente = noteUtente
                            )
                            onEsercizioAdded(newEsercizio)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Done, contentDescription = "Conferma")
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
            OutlinedTextField(
                value = esercizioName,
                onValueChange = { esercizioName = it },
                label = { Text("Nome Esercizio") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = serie,
                onValueChange = { serie = it },
                label = { Text("Serie") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = priorita,
                onValueChange = { priorita = it },
                label = { Text("Priorità (facoltativa)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = riposo,
                onValueChange = { riposo = it },
                label = { Text("Tempo di Riposo (facoltativo)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notePT,
                onValueChange = { notePT = it },
                label = { Text("Note PT (facoltative)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = noteUtente,
                onValueChange = { noteUtente = it },
                label = { Text("Note Utente (facoltative)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (esercizioName.isNotEmpty() && serie.isNotEmpty()) {
                    val newEsercizio = Esercizio(
                        name = esercizioName,
                        serie = serie,
                        priorita = priorita.toIntOrNull(),
                        riposo = riposo,
                        notePT = notePT,
                        noteUtente = noteUtente
                    )
                    onEsercizioAdded(newEsercizio)
                    navController.popBackStack()
                }
            }) {
                Text("Aggiungi Esercizio")
            }
        }
    }
}
