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
    val isEditMode = initialUser != null

    // Variabili di stato per nome e cognome
    var nome by remember { mutableStateOf(initialUser?.nome ?: "") }
    var cognome by remember { mutableStateOf(initialUser?.cognome ?: "") }

    // Variabile per mostrare/nascondere la sezione di modifica dei campi utente
    var showEditFields by remember { mutableStateOf(!isEditMode) }

    // Stato per mostrare il dialog di rimozione
    var showRemoveDialog by remember { mutableStateOf(false) }
    // Stato per mostrare il bottom sheet con i dettagli della scheda
    var showScheduleSheet by remember { mutableStateOf(false) }

    // Dialog di conferma rimozione
    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            confirmButton = {
                Button(onClick = {
                    showRemoveDialog = false
                    onRemove?.invoke()
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Conferma") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveDialog = false }) { Text("Annulla") }
            },
            title = { Text("Conferma Rimozione") },
            text = { Text("Sei sicuro di voler rimuovere l'utente?") },
            shape = RoundedCornerShape(8.dp)
        )
    }

    if (showScheduleSheet && initialUser?.scheda != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showScheduleSheet = false },
            sheetState = sheetState,
            modifier = Modifier.fillMaxWidth()
        ) {
            WorkoutCardSheet(
                scheda = initialUser.scheda!!,
                onClose = { showScheduleSheet = false }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Modifica Utente" else "Aggiungi Utente") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Se stiamo modificando un utente, mostriamo il suo code
            if (initialUser != null) {
                Text(text = "Codice: ${initialUser.code}", style = MaterialTheme.typography.titleMedium)
            }

            // Pulsante per mostrare/nascondere la sezione di modifica
            OutlinedButton(
                onClick = { showEditFields = !showEditFields },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (!showEditFields) "Modifica Dati Utente" else "Nascondi Modifica"
                )
            }

            // Se showEditFields è true, mostriamo la sezione di modifica
            if (showEditFields) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cognome,
                    onValueChange = { cognome = it },
                    label = { Text("Cognome") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Pulsanti di Annulla/Salva
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Annulla")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            // Logica di salvataggio
                            val trimmedNome = nome.trim()
                            val trimmedCognome = cognome.trim()

                            val name = trimmedNome.filter { it.isLetter() }
                            val surname = trimmedCognome.filter { it.isLetter() }
                            // Generiamo (o recuperiamo) un code
                            val userCode = initialUser?.code
                                ?: ((name.take(3) + surname.take(3)).lowercase() + (0..999).random())

                            // Se non esiste una scheda, ne creiamo una di default
                            val scheda = initialUser?.scheda
                                ?: Scheda(
                                    dataInizio = getCurrentFormattedDate(),
                                    durata = 7
                                ).apply {
                                    if (giorni.isEmpty()) {
                                        giorni = mutableMapOf(
                                            "giorno1" to Giorno("A"),
                                            "giorno2" to Giorno("B"),
                                            "giorno3" to Giorno("C")
                                        )
                                    }
                                }

                            val user = Utente(
                                code = userCode,
                                nome = trimmedNome,
                                cognome = trimmedCognome,
                                scheda = scheda
                            )
                            onSave(user)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salva")
                    }
                }
            }

            // Se l'utente ha già una scheda, visualizziamo una card di riepilogo + pulsante
            if (initialUser?.scheda != null) {
                Text(text = "Gestione scheda", style = MaterialTheme.typography.titleLarge)
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
                            text = "Durata: ${initialUser.scheda!!.durata} settimane",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Se siamo in modalità modifica, mostriamo i pulsanti "Modifica Scheda" e "Rimuovi Utente"
            if (isEditMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween, // Spazio tra i pulsanti aumentato
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pulsante per modificare la scheda
                    Button(
                        onClick = { onEditWorkoutCard(initialUser!!.code) },
                        modifier = Modifier.fillMaxWidth(1f)
                    ) {
                        Text("Modifica Scheda")
                    }
                    // Pulsante rosso per rimuovere l'utente
                    Button(
                        onClick = { showRemoveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(1f)
                    ) {
                        Text("Rimuovi Utente", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutCardSheet(
    scheda: Scheda,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Dettagli Scheda", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Data Inizio: ${formatToDisplayDate(scheda.dataInizio)}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Durata: ${scheda.durata} giorni",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Giorni di Allenamento:", style = MaterialTheme.typography.titleSmall)

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(scheda.giorni.toList()) { (dayKey, giorno) ->
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
            onClick = onClose,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Chiudi")
        }
    }
}
