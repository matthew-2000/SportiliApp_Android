package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

    // Stato per mostrare il dialog di rimozione
    var showRemoveDialog by remember { mutableStateOf(false) }
    // Stato per mostrare il bottom sheet con i dettagli della scheda
    var showScheduleSheet by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveDialog = false
                    onRemove?.invoke()
                }) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) { Text("Annulla") }
            },
            title = { Text("Conferma Rimozione") },
            text = { Text("Sei sicuro di voler rimuovere l'utente?") },
            shape = RoundedCornerShape(8.dp)
        )
    }

// BottomSheet per mostrare i dettagli della scheda
    if (showScheduleSheet && initialUser?.scheda != null) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            sheetState = sheetState,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Dettagli Scheda", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Data Inizio: ${formatToDisplayDate(initialUser.scheda!!.dataInizio)}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Durata: ${initialUser.scheda!!.durata} giorni", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Giorni di Allenamento:", style = MaterialTheme.typography.titleSmall)

                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(initialUser.scheda!!.giorni.toList()) { (dayKey, giorno) ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = "- ${giorno.name}",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            if (giorno.gruppiMuscolari.isEmpty()) {
                                Text(
                                    text = "   Nessun gruppo muscolare",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                giorno.gruppiMuscolari.forEach { (groupKey, gruppo) ->
                                    val eserciziText = gruppo.esercizi.values.joinToString(separator = ", ") {
                                        "${it.name} - ${it.serie}"
                                    }
                                    Text(
                                        text = "   - ${gruppo.nome}: $eserciziText",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showScheduleSheet = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Chiudi")
                }
            }
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text(if (isEditMode) "Modifica Utente" else "Aggiungi Utente") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            if (initialUser != null) {
                Text(text = initialUser.code, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = cognome,
                onValueChange = { cognome = it },
                label = { Text("Cognome") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Annulla") }
                Spacer(modifier = Modifier.width(12.dp))
                Button(onClick = {
                    val userCode = initialUser?.code ?: ((nome.take(3) + cognome.take(3)).uppercase() + (0..999).random())
                    val scheda = initialUser?.scheda ?: Scheda(dataInizio = getCurrentFormattedDate(), durata = 7)
                    if (scheda.giorni.isEmpty()) {
                        val updatedGiorni = scheda.giorni.toMutableMap()
                        updatedGiorni["giorno1"] = Giorno("A")
                        updatedGiorni["giorno2"] = Giorno("B")
                        updatedGiorni["giorno3"] = Giorno("C")
                        scheda.giorni = updatedGiorni
                    }
                    val user = Utente(code = userCode, nome = nome, cognome = cognome, scheda = scheda)
                    onSave(user)
                }, modifier = Modifier.weight(1f)) { Text("Salva") }
            }
            Spacer(modifier = Modifier.height(24.dp))
            // Se l'utente ha già una scheda, visualizza il riepilogo in una Card cliccabile per mostrare i dettagli
            if (initialUser?.scheda != null) {
                Text(text = "Gestione scheda", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showScheduleSheet = true }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Scheda di Allenamento", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Data Inizio: ${formatToDisplayDate(initialUser.scheda!!.dataInizio)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Durata: ${initialUser.scheda!!.durata} giorni",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            if (isEditMode) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween, // Spazio tra i pulsanti aumentato
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { onEditWorkoutCard(initialUser!!.code) },
                        modifier = Modifier.fillMaxWidth(0.9f) // Il pulsante occupa il 90% della larghezza
                    ) {
                        Text("Modifica Scheda")
                    }
                    Button(
                        onClick = { showRemoveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), // Bottone rosso
                        modifier = Modifier.fillMaxWidth(0.9f) // Il pulsante occupa il 90% della larghezza
                    ) {
                        Text("Rimuovi Utente", color = MaterialTheme.colorScheme.onError) // Testo bianco per leggibilità
                    }
                }
            }
        }
    }
}