// newadmin/ui/screens/EditWorkoutCardScreen.kt
package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Giorno

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutCardScreen(
    scheda: Scheda,
    onDaySelected: (String, Giorno) -> Unit,
    onSave: (Scheda) -> Unit,
    onCancel: () -> Unit
) {
    var startDate by remember { mutableStateOf(formatToDisplayDate(scheda.dataInizio)) }
    var duration by remember { mutableStateOf(scheda.durata.toString()) }
    // Convertiamo la mappa dei giorni in una lista
    val daysList = scheda.giorni.toList()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Workout Card") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            OutlinedTextField(
                value = startDate,
                onValueChange = { startDate = it },
                label = { Text("Start Date") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = duration,
                onValueChange = { duration = it },
                label = { Text("Duration (days)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Days", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(daysList) { (dayKey, giorno) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onDaySelected(dayKey, giorno) },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Text(
                            text = "Day: ${giorno.name}",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCancel) { Text("Cancel") }
                Button(onClick = {
                    // Costruisce una scheda aggiornata e la restituisce
                    val updatedScheda = scheda.copy(
                        dataInizio = startDate,
                        durata = duration.toIntOrNull() ?: scheda.durata
                    )
                    onSave(updatedScheda)
                }) { Text("Save") }
            }
        }
    }
}