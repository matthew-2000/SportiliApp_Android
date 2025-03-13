package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.GruppoMuscolare
import java.util.LinkedHashMap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMuscleGroupScreen(
    userCode: String,
    dayKey: String,
    group: GruppoMuscolare,
    onSave: (GruppoMuscolare) -> Unit,
    onCancel: () -> Unit
) {
    var groupName by remember { mutableStateOf(group.nome) }
    // Convertiamo la mappa degli esercizi in una lista modificabile per gestire l'ordine
    var exercisesList by remember { mutableStateOf(group.esercizi.toList().toMutableStateList()) }

    // Stato per mostrare il dialog per aggiungere un nuovo esercizio
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var newExerciseName by remember { mutableStateOf("") }
    var newExerciseSerie by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Modifica Gruppo") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            // Campo per modificare il nome del gruppo
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Nome del Gruppo") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Esercizi", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(exercisesList) { (exerciseKey, exercise) ->
                    ExerciseItem(
                        exerciseKey = exerciseKey,
                        exercise = exercise,
                        onMoveUp = {
                            val index = exercisesList.indexOfFirst { it.first == exerciseKey }
                            if (index > 0) {
                                val temp = exercisesList[index - 1]
                                exercisesList[index - 1] = exercisesList[index]
                                exercisesList[index] = temp
                                exercisesList = exercisesList.mapIndexed { i, pair ->
                                    "esercizio${i + 1}" to pair.second
                                }.toMutableStateList()
                            }
                        },
                        onMoveDown = {
                            val index = exercisesList.indexOfFirst { it.first == exerciseKey }
                            if (index < exercisesList.size - 1) {
                                val temp = exercisesList[index + 1]
                                exercisesList[index + 1] = exercisesList[index]
                                exercisesList[index] = temp
                                exercisesList = exercisesList.mapIndexed { i, pair ->
                                    "esercizio${i + 1}" to pair.second
                                }.toMutableStateList()
                            }
                        },
                        onRemove = {
                            exercisesList.removeAll { it.first == exerciseKey }
                            exercisesList = exercisesList.mapIndexed { i, pair ->
                                "esercizio${i + 1}" to pair.second
                            }.toMutableStateList()
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Annulla") }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = { showAddExerciseDialog = true }, modifier = Modifier.weight(1f)) { Text("Aggiungi Esercizio") }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val updatedGroup = GruppoMuscolare(
                        nome = groupName,
                        esercizi = LinkedHashMap(exercisesList.toMap())
                    )
                    onSave(updatedGroup)
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Salva Gruppo") }
        }
    }
    if (showAddExerciseDialog) {
        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Aggiungi Esercizio") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newExerciseName,
                        onValueChange = { newExerciseName = it },
                        label = { Text("Nome Esercizio") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newExerciseSerie,
                        onValueChange = { newExerciseSerie = it },
                        label = { Text("Serie (es. 3x8)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val newKey = "esercizio${exercisesList.size + 1}"
                    val newExercise = Esercizio(name = newExerciseName, serie = newExerciseSerie)
                    exercisesList.add(newKey to newExercise)
                    newExerciseName = ""
                    newExerciseSerie = ""
                    showAddExerciseDialog = false
                }) { Text("Aggiungi") }
            },
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) { Text("Annulla") }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
fun ExerciseItem(
    exerciseKey: String,
    exercise: Esercizio,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                Text(text = exercise.name, style = MaterialTheme.typography.bodyLarge)
                Text(text = "Serie: ${exercise.serie}", style = MaterialTheme.typography.bodySmall)
            }
            Row {
                IconButton(onClick = onMoveUp) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = "Sposta Su")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Sposta Giù")
                }
                IconButton(onClick = onRemove) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Rimuovi")
                }
            }
        }
    }
}