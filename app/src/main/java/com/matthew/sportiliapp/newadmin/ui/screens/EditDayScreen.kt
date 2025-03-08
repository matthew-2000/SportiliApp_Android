// newadmin/ui/screens/EditDayScreen.kt
package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDayScreen(
    dayKey: String,
    day: Giorno,
    onMuscleGroupSelected: (String, GruppoMuscolare) -> Unit,
    onSave: (Giorno) -> Unit,
    onCancel: () -> Unit
) {
    // Per semplicità, supponiamo che Giorno abbia solo un campo "name"
    var dayName by remember { mutableStateOf(day.name) }
    // Supponiamo che day.gruppiMuscolari sia una Map<String, Any> per questo esempio
    val muscleGroupsList = day.gruppiMuscolari.toList()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Day") }) }
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
                label = { Text("Day Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Muscle Groups", style = MaterialTheme.typography.titleMedium)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(muscleGroupsList) { (groupKey, muscleGroup) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onMuscleGroupSelected(groupKey, muscleGroup) },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Text(
                            text = groupKey,
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
                    val updatedDay = day.copy(name = dayName)
                    onSave(updatedDay)
                }) { Text("Save") }
            }
        }
    }
}
