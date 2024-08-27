package com.matthew.sportiliapp.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.matthew.sportiliapp.model.GymViewModel
import com.matthew.sportiliapp.model.Utente

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtenteScreen(
    navController: NavController,
    utenteCode: String
) {
    val gymViewModel = GymViewModel()
    val users by gymViewModel.users.observeAsState(initial = emptyList())

    // Controlla se la lista users è vuota o se contiene l'utente cercato
    val utente = users.firstOrNull { utente -> utente.code == utenteCode }

    if (utente != null) {
        var editedNome by remember { mutableStateOf(utente.nome) }
        var editedCognome by remember { mutableStateOf(utente.cognome) }
        var showEliminaAlert by remember { mutableStateOf(false) }
        var showAddSchedaView by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Modifica Utente") })
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(8.dp)
            ) {
                FormSection(title = "Codice", content = utente.code)
                Spacer(modifier = Modifier.height(16.dp))
                TextField("Nome", editedNome) { editedNome = it }
                TextField("Cognome", editedCognome) { editedCognome = it }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = { showAddSchedaView = true }) {
                    Text(if (utente.scheda != null) "Modifica scheda" else "Aggiungi scheda")
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.CenterHorizontally)
                ) {
                    Button(onClick = {
                        val updatedUtente = Utente(code = utente.code, cognome = editedCognome, nome = editedNome, scheda = utente.scheda)
                        gymViewModel.updateUser(utente = updatedUtente)
                    }) {
                        Text("Salva modifiche")
                    }

                    Button(
                        onClick = { showEliminaAlert = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Elimina", color = MaterialTheme.colorScheme.onError)
                    }
                }

                if (showEliminaAlert) {
                    AlertDialog(
                        onDismissRequest = { showEliminaAlert = false },
                        title = { Text("Conferma Eliminazione") },
                        text = { Text("Sei sicuro di voler eliminare questo utente?") },
                        confirmButton = {
                            Button(onClick = {
                                gymViewModel.removeUser(code = utente.code)
                                showEliminaAlert = false
                            }) {
                                Text("Elimina")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showEliminaAlert = false }) {
                                Text("Annulla")
                            }
                        }
                    )
                }

                if (showAddSchedaView) {
                    // AddSchedaView(userCode = utente.code, gymViewModel = gymViewModel, scheda = utente.scheda, onDismiss = { showAddSchedaView = false })
                }
            }
        }
    } else {
        // Qui puoi gestire il caso in cui l'utente non viene trovato o la lista è vuota
        Text("Utente non trovato")
    }
}


@Composable
fun TextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun FormSection(title: String, content: String) {
    Column {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(content)
        Divider()
    }
}
