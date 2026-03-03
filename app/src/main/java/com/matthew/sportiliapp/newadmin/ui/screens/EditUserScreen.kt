package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Utente
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val displayDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private val saveDateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
private val inputDateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

fun formatToDisplayDate(dateString: String): String {
    val parsedDate = runCatching { saveDateFormatter.parse(dateString) }.getOrNull() ?: Date()
    return displayDateFormatter.format(parsedDate)
}

fun formatToSaveDate(dateString: String): String {
    val parsedDate = runCatching { inputDateFormatter.parse(dateString) }.getOrNull() ?: Date()
    return saveDateFormatter.format(parsedDate)
}

fun getCurrentFormattedDate(): String = saveDateFormatter.format(Date())

private fun normalizePersonName(rawValue: String): String =
    rawValue
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { token ->
            token.lowercase(Locale.getDefault()).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
        }

private fun normalizedLettersOnly(value: String): String =
    value.filter { it.isLetter() }

private fun generateUserCode(nome: String, cognome: String): String {
    val baseName = normalizedLettersOnly(nome).take(3).padEnd(3, 'x')
    val baseSurname = normalizedLettersOnly(cognome).take(3).padEnd(3, 'x')
    val suffix = (0..9999).random().toString().padStart(4, '0')
    return (baseName + baseSurname + suffix).lowercase(Locale.getDefault())
}

private fun defaultWorkoutCard(): Scheda =
    Scheda(
        dataInizio = getCurrentFormattedDate(),
        durata = 7,
        giorni = linkedMapOf(
            "giorno1" to Giorno("A"),
            "giorno2" to Giorno("B"),
            "giorno3" to Giorno("C")
        )
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserScreen(
    initialUser: Utente? = null,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onSave: (Utente) -> Unit,
    onRemove: (() -> Unit)? = null,
    onCancel: () -> Unit,
    onEditWorkoutCard: (String) -> Unit
) {
    val isEditMode = initialUser != null
    val initialNome = initialUser?.nome.orEmpty()
    val initialCognome = initialUser?.cognome.orEmpty()

    var nome by rememberSaveable(initialUser?.code) { mutableStateOf(initialNome) }
    var cognome by rememberSaveable(initialUser?.code) { mutableStateOf(initialCognome) }
    var showEditFields by rememberSaveable(initialUser?.code) { mutableStateOf(!isEditMode) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showScheduleSheet by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var nomeError by rememberSaveable(initialUser?.code) { mutableStateOf<String?>(null) }
    var cognomeError by rememberSaveable(initialUser?.code) { mutableStateOf<String?>(null) }

    val normalizedNome = remember(nome) { normalizePersonName(nome) }
    val normalizedCognome = remember(cognome) { normalizePersonName(cognome) }
    val isDirty = remember(nome, cognome, initialNome, initialCognome, isEditMode) {
        if (isEditMode) {
            normalizedNome != initialNome || normalizedCognome != initialCognome
        } else {
            normalizedNome.isNotBlank() || normalizedCognome.isNotBlank()
        }
    }

    fun discardChanges() {
        showExitDialog = false
        onCancel()
    }

    fun validateAndSave() {
        val nomeLetters = normalizedLettersOnly(normalizedNome)
        val cognomeLetters = normalizedLettersOnly(normalizedCognome)

        nomeError = when {
            normalizedNome.isBlank() -> "Inserisci il nome"
            nomeLetters.length < 2 -> "Il nome deve contenere almeno 2 lettere"
            else -> null
        }
        cognomeError = when {
            normalizedCognome.isBlank() -> "Inserisci il cognome"
            cognomeLetters.length < 2 -> "Il cognome deve contenere almeno 2 lettere"
            else -> null
        }

        if (nomeError != null || cognomeError != null) return

        val user = Utente(
            code = initialUser?.code ?: generateUserCode(normalizedNome, normalizedCognome),
            nome = normalizedNome,
            cognome = normalizedCognome,
            scheda = initialUser?.scheda ?: defaultWorkoutCard()
        )
        showExitDialog = false
        onSave(user)
    }

    fun requestExit() {
        if (isSaving) return
        if (isDirty) {
            showExitDialog = true
        } else {
            onCancel()
        }
    }

    BackHandler(enabled = !isSaving) {
        requestExit()
    }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveDialog = false
                        onRemove?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Conferma") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveDialog = false }) { Text("Annulla") }
            },
            title = { Text("Conferma Rimozione") },
            text = { Text("Sei sicuro di voler rimuovere l'utente?") },
            shape = RoundedCornerShape(8.dp)
        )
    }

    if (showExitDialog) {
        UnsavedChangesDialog(
            onSave = { validateAndSave() },
            onDiscard = { discardChanges() },
            onDismiss = { showExitDialog = false }
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
            if (isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            initialUser?.let { user ->
                Text(text = "Codice: ${user.code}", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(
                onClick = { showEditFields = !showEditFields },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving
            ) {
                Text(
                    text = if (!showEditFields) "Modifica Dati Utente" else "Nascondi Modifica"
                )
            }

            if (showEditFields) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = {
                        nome = it
                        if (nomeError != null) nomeError = null
                    },
                    label = { Text("Nome") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = nomeError != null,
                    supportingText = nomeError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                OutlinedTextField(
                    value = cognome,
                    onValueChange = {
                        cognome = it
                        if (cognomeError != null) cognomeError = null
                    },
                    label = { Text("Cognome") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = cognomeError != null,
                    supportingText = cognomeError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { requestExit() },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("Annulla")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { validateAndSave() },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        Text("Salva")
                    }
                }
            }

            if (initialUser?.scheda != null) {
                Text(text = "Gestione scheda", style = MaterialTheme.typography.titleLarge)
                Card(
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSaving) { showScheduleSheet = true }
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

            if (isEditMode) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = { onEditWorkoutCard(initialUser!!.code) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    ) {
                        Text("Modifica Scheda")
                    }
                    Button(
                        onClick = { showRemoveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
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
            text = "Durata: ${scheda.durata} settimane",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Giorni di Allenamento:", style = MaterialTheme.typography.titleSmall)

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(scheda.giorni.toList()) { (_, giorno) ->
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
                        giorno.gruppiMuscolari.forEach { (_, gruppo) ->
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
