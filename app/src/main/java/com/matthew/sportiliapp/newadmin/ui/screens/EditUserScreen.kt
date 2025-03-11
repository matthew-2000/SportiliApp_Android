package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Utente
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatToDisplayDate(dateString: String): String {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(parser.parse(dateString) ?: Date())
}

fun formatToSaveDate(dateString: String): String {
    val parser = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
    return formatter.format(parser.parse(dateString) ?: Date())
}

fun getCurrentFormattedDate(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
    return dateFormat.format(Date())
}

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
                    val scheda = initialUser?.scheda ?: Scheda(dataInizio = getCurrentFormattedDate(), 7)
                    if (scheda.giorni.isEmpty()) {
                        val updatedGiorni = scheda.giorni.toMutableMap()
                        updatedGiorni["giorno1"] = Giorno("A")
                        updatedGiorni["giorno2"] = Giorno("B")
                        updatedGiorni["giorno3"] = Giorno("C")
                        scheda.giorni = updatedGiorni
                    }
                    val user = Utente(code = userCode, nome = nome, cognome = cognome, scheda = scheda)
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
