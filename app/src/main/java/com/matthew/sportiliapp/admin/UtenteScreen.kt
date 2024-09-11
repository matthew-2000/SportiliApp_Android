package com.matthew.sportiliapp.admin

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtenteScreen(
    navController: NavController,
    utenteCode: String,
    gymViewModel: GymViewModel = viewModel() // Utilizziamo viewModel() per ottenere l'istanza
) {
    val users by gymViewModel.users.observeAsState(initial = emptyList())
    val context = LocalContext.current

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

                Button(onClick = {
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
                    TextButton(onClick = {
                        editedCognome = editedCognome.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(
                            Locale.ROOT) else it.toString() }
                        editedNome = editedNome.trim().replaceFirstChar {  if (it.isLowerCase()) it.titlecase(
                            Locale.ROOT) else it.toString() }
                        val updatedUtente = Utente(code = utente.code, cognome = editedCognome, nome = editedNome, scheda = utente.scheda)
                        gymViewModel.updateUser(utente = updatedUtente)
                        Toast.makeText(context, "Utente aggiornato", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Salva modifiche")
                    }

                    TextButton(
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
                                navController.popBackStack()
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
                val giornoName = backStackEntry.arguments?.getString("giornoName") ?: return@composable
                val giorno = scheda.giorni[giornoName] ?: Giorno(giornoName)

                AddGruppoMuscolareScreen(
                    navController = internalNavController,
                    giorno = giorno,
                    onGruppoMuscolareAdded = { newGruppoMuscolare ->
                        val updatedGruppiMuscolari = giorno.gruppiMuscolari.toMutableMap()
                        updatedGruppiMuscolari["gruppo${giorno.gruppiMuscolari.size + 1}"] = newGruppoMuscolare
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
                        val updatedGruppiMuscolari2 = updatedGruppiMuscolari.mapIndexed { index, entry ->
                            "gruppo${index + 1}" to entry.second
                        }.toMap()
                        giorno.gruppiMuscolari = updatedGruppiMuscolari2
                        val updatedGiorni = scheda.giorni.toMutableMap()
                        updatedGiorni[giornoName] = giorno
                        scheda.giorni = updatedGiorni
                        gymViewModel.saveScheda(scheda, utenteCode)
                    },
                    onGruppoMuscolareDeleted = { index ->
                        val updatedGruppiMuscolari = giorno.gruppiMuscolari.toList().toMutableList()

                        // Rimuove il gruppo muscolare all'indice specificato
                        updatedGruppiMuscolari.removeAt(index)

                        // Rinumerazione delle chiavi rimanenti
                        val renumberedGruppiMuscolari = updatedGruppiMuscolari.mapIndexed { newIndex, entry ->
                            "gruppo${newIndex + 1}" to entry.second
                        }.toMap()

                        // Aggiorna i gruppi muscolari e salva la scheda
                        giorno.gruppiMuscolari = renumberedGruppiMuscolari
                        val updatedGiorni = scheda.giorni.toMutableMap()
                        updatedGiorni[giornoName] = giorno
                        scheda.giorni = updatedGiorni
                        gymViewModel.saveScheda(scheda, utenteCode)
                    }
                )
            }
            composable("editGruppoMuscolareScreen/{nomeGruppo}/{nomeGiorno}") { backStackEntry ->
                val nomeGruppo = backStackEntry.arguments?.getString("nomeGruppo") ?: return@composable
                val nomeGiorno = backStackEntry.arguments?.getString("nomeGiorno") ?: return@composable

                // Cerca il gruppo muscolare all'interno della scheda dell'utente

                val giorno = scheda.giorni.values.firstOrNull { it.name == nomeGiorno }

                if (giorno == null) {
                    return@composable
                }

                val gruppo = giorno.gruppiMuscolari.values
                    .firstOrNull { it.nome == nomeGruppo }

                if (gruppo != null) {
                    EditGruppoMuscolareScreen(
                        navController = internalNavController,
                        gruppoMuscolare = gruppo,
                        onEsercizioAdded = { newEsercizio ->
                            // Aggiungi l'esercizio al gruppo muscolare
                            val updatedEsercizi = gruppo.esercizi.toMutableMap()
                            updatedEsercizi["esercizio${gruppo.esercizi.size+1}"] = newEsercizio
                            gruppo.esercizi = updatedEsercizi

                            // Aggiorna la scheda dell'utente con il nuovo gruppo muscolare
                            scheda.giorni.forEach { (giornoName, giorno) ->
                                if (giorno.gruppiMuscolari.containsKey(nomeGruppo)) {
                                    val updatedGruppiMuscolari = giorno.gruppiMuscolari.toMutableMap()
                                    updatedGruppiMuscolari[nomeGruppo] = gruppo
                                    giorno.gruppiMuscolari = updatedGruppiMuscolari
                                    val updatedGiorni = scheda.giorni.toMutableMap()
                                    updatedGiorni[giornoName] = giorno
                                    scheda.giorni = updatedGiorni
                                }
                            }

                            // Salva la scheda aggiornata
                            gymViewModel.saveScheda(scheda, utenteCode)
                        },
                        onEsercizioMoved = { oldIndex, newIndex ->
                            // Sposta l'esercizio all'interno del gruppo muscolare
                            val updatedEsercizi = gruppo.esercizi.toList().toMutableList()
                            val movedItem = updatedEsercizi.removeAt(oldIndex)
                            updatedEsercizi.add(newIndex, movedItem)
                            val updatedEserciziMap = updatedEsercizi.mapIndexed { index, entry -> "esercizio${index + 1}" to entry.second }.toMap()
                            gruppo.esercizi = updatedEserciziMap

                            // Aggiorna la scheda dell'utente con il nuovo gruppo muscolare
                            scheda.giorni.forEach { (giornoName, giorno) ->
                                if (giorno.gruppiMuscolari.containsKey(nomeGruppo)) {
                                    val updatedGruppiMuscolari = giorno.gruppiMuscolari.toMutableMap()
                                    updatedGruppiMuscolari[nomeGruppo] = gruppo
                                    giorno.gruppiMuscolari = updatedGruppiMuscolari
                                    val updatedGiorni = scheda.giorni.toMutableMap()
                                    updatedGiorni[giornoName] = giorno
                                    scheda.giorni = updatedGiorni
                                }
                            }

                            // Salva la scheda aggiornata
                            gymViewModel.saveScheda(scheda, utenteCode)
                        },
                        onEsercizioDeleted = { index ->
                            // Rimuovi l'esercizio dal gruppo muscolare
                            val updatedEsercizi = gruppo.esercizi.toMutableMap()
                            updatedEsercizi.remove("esercizio${index + 1}")
                            gruppo.esercizi = updatedEsercizi

                            // Aggiorna la scheda dell'utente con il nuovo gruppo muscolare
                            scheda.giorni.forEach { (giornoName, giorno) ->
                                if (giorno.gruppiMuscolari.containsKey(nomeGruppo)) {
                                    val updatedGruppiMuscolari = giorno.gruppiMuscolari.toMutableMap()
                                    updatedGruppiMuscolari[nomeGruppo] = gruppo
                                    giorno.gruppiMuscolari = updatedGruppiMuscolari
                                    val updatedGiorni = scheda.giorni.toMutableMap()
                                    updatedGiorni[giornoName] = giorno
                                    scheda.giorni = updatedGiorni
                                }
                            }

                            // Salva la scheda aggiornata
                            gymViewModel.saveScheda(scheda, utenteCode)
                        },
                        onEsercizioEdited = { index, esercizio ->
                            // Modifica l'esercizio esistente nel gruppo muscolare
                            val updatedEsercizi = gruppo.esercizi.toMutableMap()
                            updatedEsercizi["esercizio${index + 1}"] = esercizio
                            gruppo.esercizi = updatedEsercizi

                            // Aggiorna la scheda dell'utente con il nuovo gruppo muscolare
                            scheda.giorni.forEach { (giornoName, giorno) ->
                                if (giorno.gruppiMuscolari.containsKey(nomeGruppo)) {
                                    val updatedGruppiMuscolari = giorno.gruppiMuscolari.toMutableMap()
                                    updatedGruppiMuscolari[nomeGruppo] = gruppo
                                    giorno.gruppiMuscolari = updatedGruppiMuscolari
                                    val updatedGiorni = scheda.giorni.toMutableMap()
                                    updatedGiorni[giornoName] = giorno
                                    scheda.giorni = updatedGiorni
                                }
                            }

                            // Salva la scheda aggiornata
                            gymViewModel.saveScheda(scheda, utenteCode)
                        }
                    )
                }
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
