package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.*

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.GruppoMuscolare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMuscleGroupScreen(
    userCode: String,
    dayKey: String,
    groupKey: String,
    onSave: (GruppoMuscolare) -> Unit,
    onCancel: () -> Unit
) {
    // Schermata di editing semplificata per il gruppo muscolare
    Scaffold(
        topBar = { TopAppBar(title = { Text("Edit Muscle Group") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = groupKey,
                onValueChange = {},
                label = { Text("Group Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = onCancel) { Text("Cancel") }
                Button(onClick = { onSave(GruppoMuscolare()) }) { Text("Save") }
            }
        }
    }
}
