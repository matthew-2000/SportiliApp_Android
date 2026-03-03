package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifiche non salvate") },
        text = {
            Text(
                text = "Vuoi salvare le modifiche prima di uscire?",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Salva")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDiscard) {
                Text("Scarta")
            }
        }
    )
}
