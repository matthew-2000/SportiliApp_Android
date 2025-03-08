package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Esercizio

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseScreen(
    userCode: String,
    dayKey: String,
    groupKey: String,
    exerciseKey: String,
    onSave: (Esercizio) -> Unit,
    onCancel: () -> Unit
) {
    var exerciseName by remember { mutableStateOf(exerciseKey) } // Carica il nome reale
    // Altri campi come serie, riposo, note, ecc.

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Exercise") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            OutlinedTextField(
                value = exerciseName,
                onValueChange = { exerciseName = it },
                label = { Text("Exercise Name") },
                modifier = Modifier.fillMaxWidth()
            )
            // Aggiungi altri campi di editing se necessario
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCancel) { Text("Cancel") }
                Button(onClick = { onSave(Esercizio()) }) { Text("Save") }
            }
        }
    }
}
