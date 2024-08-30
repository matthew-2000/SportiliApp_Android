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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.GymViewModel
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Utente

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtenteScreen(
    navController: NavController,
    utenteCode: String,
    gymViewModel: GymViewModel = viewModel() // Utilizziamo viewModel() per ottenere l'istanza
) {
    val users by gymViewModel.users.observeAsState(initial = emptyList())

    val utente = users.firstOrNull { utente -> utente.code == utenteCode }

    if (utente != null) {
        var editedNome by remember { mutableStateOf(utente.nome) }
        var editedCognome by remember { mutableStateOf(utente.cognome) }
        var showEliminaAlert by remember { mutableStateOf(false) }

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
                CustomTextField("Nome", editedNome) { editedNome = it }
                CustomTextField("Cognome", editedCognome) { editedCognome = it }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = {
                    navController.navigate("editScheda")
                }) {
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
            }
        }
    } else {
        // Gestione del caso in cui l'utente non viene trovato o la lista è vuota
        Text("Utente non trovato")
    }
}


@Composable
fun UtenteNavHost(
    navController: NavController,
    utenteCode: String,
    gymViewModel: GymViewModel = viewModel() // Utilizziamo viewModel() per ottenere l'istanza
) {
    val internalNavController = rememberNavController()
    val users by gymViewModel.users.observeAsState(initial = emptyList())

    // Controlla se la lista users è vuota o se contiene l'utente cercato
    val utente = users.firstOrNull { utente -> utente.code == utenteCode }

    if (utente != null) {
        val scheda = utente.scheda ?: Scheda()

        NavHost(
            navController = internalNavController,
            startDestination = "utenteScreen"
        ) {
            composable("utenteScreen") {
                UtenteScreen(navController = internalNavController, utenteCode)
            }
            composable("editScheda") {
                EditSchedaScreen(
                    navController = internalNavController,
                    GymViewModel(),
                    utenteCode
                ) {
                    internalNavController.popBackStack()
                }
            }
            composable("addGruppoMuscolareScreen/{giornoName}") { backStackEntry ->
                val giornoName = backStackEntry.arguments?.getString("giornoName") ?: "A"
                val giorno = scheda.giorni[giornoName] ?: Giorno(giornoName ?: "")

                AddGruppoMuscolareScreen(
                    navController = internalNavController,
                    giorno = giorno,
                    onGruppoMuscolareAdded = { newGruppoMuscolare ->
                        val updatedGruppiMuscolari = giorno.gruppiMuscolari.toMutableMap()
                        updatedGruppiMuscolari["gruppo${giorno.gruppiMuscolari.size}"] = newGruppoMuscolare
                        giorno.gruppiMuscolari = updatedGruppiMuscolari
                        val updatedGiorni = scheda.giorni.toMutableMap()
                        updatedGiorni[giornoName] = giorno
                        scheda.giorni = updatedGiorni
                        gymViewModel.saveScheda(scheda, utenteCode)
                    },
                    onGruppoMuscolareMoved = { oldIndex, newIndex ->
                        val updatedGruppiMuscolari = giorno.gruppiMuscolari.toList().toMutableList()
                        val movedItem = updatedGruppiMuscolari.removeAt(oldIndex)
                        updatedGruppiMuscolari.add(newIndex, movedItem)
                        val updatedGruppiMuscolari2 = updatedGruppiMuscolari.mapIndexed { index, entry -> "gruppo${index + 1}" to entry.second }.toMap()
                        giorno.gruppiMuscolari = updatedGruppiMuscolari2
                        val updatedGiorni = scheda.giorni.toMutableMap()
                        updatedGiorni[giornoName] = giorno
                        scheda.giorni = updatedGiorni
                        gymViewModel.saveScheda(scheda, utenteCode)
                    }
                )
            }

            composable("addEsercizioScreen/{gruppoName}") { backStackEntry ->
                val gruppoName = backStackEntry.arguments?.getString("gruppoName") ?: "Nome"
                val gruppo = scheda.giorni.values
                    .flatMap { it.gruppiMuscolari.values }
                    .firstOrNull { it.nome == gruppoName }
                    ?: GruppoMuscolare(gruppoName ?: "")

                AddEsercizioScreen(internalNavController, gruppo, onEsercizioAdded = { newEsercizio ->
                    // Converti la mappa esistente in una mappa mutabile
                    val updatedEsercizi = gruppo.esercizi.toMutableMap()
                    // Aggiungi o aggiorna l'esercizio
                    updatedEsercizi["esercizio${gruppo.esercizi.size}"] = newEsercizio
                    // Riassegna la mappa aggiornata
                    gruppo.esercizi = updatedEsercizi
                    // Aggiorna il gruppo muscolare nella scheda
                    scheda.giorni.forEach { (giornoName, giorno) ->
                        if (giorno.gruppiMuscolari.containsKey(gruppoName)) {
                            val updatedGruppiMuscolari = giorno.gruppiMuscolari.toMutableMap()
                            updatedGruppiMuscolari[gruppoName] = gruppo
                            giorno.gruppiMuscolari = updatedGruppiMuscolari
                            val updatedGiorni = scheda.giorni.toMutableMap()
                            updatedGiorni[giornoName] = giorno
                            scheda.giorni = updatedGiorni
                        }
                    }
                    gymViewModel.saveScheda(scheda, utenteCode)
                })
            }

        }
    }
}

@Composable
fun CustomTextField(label: String, value: String, onValueChange: (String) -> Unit) {
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
