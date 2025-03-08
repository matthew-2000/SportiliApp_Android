package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Utente

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    initialUser: Utente? = null,
    onSave: (Utente) -> Unit,
    onRemove: (() -> Unit)? = null,
    onCancel: () -> Unit,
    onEditWorkoutCard: (String) -> Unit
) {
    var nome by remember { mutableStateOf(initialUser?.nome ?: "") }
    var cognome by remember { mutableStateOf(initialUser?.cognome ?: "") }
    val isEditMode = initialUser != null

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (isEditMode) "Edit User" else "Add User") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("First Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = cognome,
                onValueChange = { cognome = it },
                label = { Text("Last Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = onCancel) { Text("Cancel") }
                Button(onClick = {
                    val userCode = initialUser?.code
                        ?: ((nome.take(3) + cognome.take(3)).uppercase() + (0..999).random())
                    val user = Utente(code = userCode, nome = nome, cognome = cognome)
                    onSave(user)
                }) { Text("Save") }
            }
            if (isEditMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { onEditWorkoutCard(initialUser!!.code) }) { Text("Edit Workout Card") }
                    onRemove?.let {
                        Button(onClick = it) { Text("Remove User") }
                    }
                }
            }
        }
    }
}
