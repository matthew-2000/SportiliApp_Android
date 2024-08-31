package com.matthew.sportiliapp.admin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
    var serie by remember { mutableStateOf(3) }
    var ripetizioni by remember { mutableStateOf(10) }
    var serieDescrizione by remember { mutableStateOf("") }
    var riposo by remember { mutableStateOf("") }
    var notePT by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aggiungi Esercizio") },
                actions = {
                    IconButton(onClick = {
                        if (esercizioName.isNotEmpty()) {
                            val finalSerie = if (serieDescrizione.isNotEmpty()) serieDescrizione else "$serie x $ripetizioni"
                            val newEsercizio = Esercizio(
                                name = esercizioName,
                                serie = finalSerie,
                                riposo = riposo,
                                notePT = notePT
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
        ) {
            // Dettagli Esercizio
            Text("Dettagli Esercizio", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = esercizioName,
                onValueChange = { esercizioName = it },
                label = { Text("Nome Esercizio") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stepper per le Serie
            Stepper(
                value = serie,
                onValueChange = { serie = it },
                range = 1..30,
                label = "Serie"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stepper per le Ripetizioni
            Stepper(
                value = ripetizioni,
                onValueChange = { ripetizioni = it },
                range = 1..50,
                label = "Ripetizioni"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ripetizioni testuali
            Text("Ripetizioni testuali", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = serieDescrizione,
                onValueChange = { serieDescrizione = it },
                label = { Text("Serie o minuti") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Altro
            Text("Altro", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = riposo,
                onValueChange = { riposo = it },
                label = { Text("Riposo") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notePT,
                onValueChange = { notePT = it },
                label = { Text("Note PT") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (esercizioName.isNotEmpty()) {
                    val finalSerie = if (serieDescrizione.isNotEmpty()) serieDescrizione else "$serie x $ripetizioni"
                    val newEsercizio = Esercizio(
                        name = esercizioName,
                        serie = finalSerie,
                        riposo = riposo,
                        notePT = notePT,
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

@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label)

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (value > range.first) {
                    onValueChange(value - 1)
                }
            }) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Decrement")
            }

            Text(text = value.toString(), style = MaterialTheme.typography.bodyLarge)

            IconButton(onClick = {
                if (value < range.last) {
                    onValueChange(value + 1)
                }
            }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Increment")
            }
        }
    }
}
