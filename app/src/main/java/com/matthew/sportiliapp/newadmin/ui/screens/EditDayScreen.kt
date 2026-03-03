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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import java.util.LinkedHashMap

private fun buildUpdatedDay(
    originalDay: Giorno,
    dayName: String,
    groupsList: List<Pair<String, GruppoMuscolare>>
): Giorno = originalDay.copy(
    name = dayName.trim(),
    gruppiMuscolari = LinkedHashMap(groupsList.toMap())
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDayScreen(
    dayKey: String,
    day: Giorno,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onSave: (Giorno) -> Unit,
    onCancel: () -> Unit,
    onMuscleGroupSelected: (String, GruppoMuscolare, Giorno) -> Unit
) {
    val initialGroupsList = remember(day) { day.gruppiMuscolari.toList() }

    var dayName by rememberSaveable(dayKey) { mutableStateOf(day.name) }
    var groupsList by remember(dayKey, day) { mutableStateOf(initialGroupsList) }
    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var dayNameError by rememberSaveable(dayKey) { mutableStateOf<String?>(null) }

    val gruppiMuscolari = remember {
        listOf(
            "Riscaldamento",
            "Addominali",
            "Cardio",
            "Circuito",
            "Pettorali",
            "Dorsali",
            "Gambe e Glutei",
            "Spalle",
            "Bicipiti",
            "Tricipiti",
            "Polpacci",
            "Defaticamento"
        )
    }
    val selectedGruppi = remember {
        mutableStateMapOf<String, Boolean>().apply {
            gruppiMuscolari.forEach { put(it, false) }
        }
    }

    val currentDay = remember(day, dayName, groupsList) {
        buildUpdatedDay(day, dayName, groupsList)
    }
    val isDirty = remember(dayName, groupsList, day) {
        dayName.trim() != day.name || groupsList != initialGroupsList
    }

    fun validateAndSave(onValidated: (Giorno) -> Unit) {
        dayNameError = if (dayName.trim().isBlank()) {
            "Inserisci il nome del giorno"
        } else {
            null
        }
        if (dayNameError != null) return
        onValidated(currentDay)
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

    if (showExitDialog) {
        UnsavedChangesDialog(
            onSave = { validateAndSave(onSave) },
            onDiscard = {
                showExitDialog = false
                onCancel()
            },
            onDismiss = { showExitDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifica Giorno") },
                actions = {
                    IconButton(onClick = { showAddGroupDialog = true }, enabled = !isSaving) {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi Gruppo")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            if (isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = dayName,
                onValueChange = {
                    dayName = it
                    if (dayNameError != null) dayNameError = null
                },
                label = { Text("Nome del Giorno") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                isError = dayNameError != null,
                supportingText = dayNameError?.let { { Text(it) } }
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Gruppi Muscolari", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(groupsList) { (groupKey, muscleGroup) ->
                    MuscleGroupItem(
                        muscleGroup = muscleGroup,
                        enabled = !isSaving,
                        onMoveUp = {
                            val index = groupsList.indexOfFirst { it.first == groupKey }
                            if (index > 0) {
                                groupsList = groupsList.toMutableList().apply {
                                    val previous = this[index - 1]
                                    this[index - 1] = this[index]
                                    this[index] = previous
                                }.mapIndexed { position, pair ->
                                    "gruppo${position + 1}" to pair.second
                                }
                            }
                        },
                        onMoveDown = {
                            val index = groupsList.indexOfFirst { it.first == groupKey }
                            if (index in 0 until groupsList.lastIndex) {
                                groupsList = groupsList.toMutableList().apply {
                                    val next = this[index + 1]
                                    this[index + 1] = this[index]
                                    this[index] = next
                                }.mapIndexed { position, pair ->
                                    "gruppo${position + 1}" to pair.second
                                }
                            }
                        },
                        onRemove = {
                            groupsList = groupsList
                                .filterNot { it.first == groupKey }
                                .mapIndexed { position, pair ->
                                    "gruppo${position + 1}" to pair.second
                                }
                        },
                        onEdit = {
                            validateAndSave { updatedDay ->
                                onMuscleGroupSelected(groupKey, muscleGroup, updatedDay)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { requestExit() },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) { Text("Annulla") }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { validateAndSave(onSave) },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving
                ) { Text("Salva") }
            }
        }
    }

    if (showAddGroupDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddGroupDialog = false
                selectedGruppi.keys.forEach { selectedGruppi[it] = false }
            },
            title = { Text("Aggiungi Gruppi Muscolari") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    gruppiMuscolari.forEach { gruppo ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedGruppi[gruppo] == true,
                                onCheckedChange = { isChecked ->
                                    selectedGruppi[gruppo] = isChecked
                                },
                                enabled = !isSaving
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(gruppo)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val existingNames = groupsList.map { it.second.nome }.toSet()
                        val selectedNames = selectedGruppi.filterValues { it }.keys
                            .filterNot { it in existingNames }

                        selectedNames.forEachIndexed { offset, groupName ->
                            val newKey = "gruppo${groupsList.size + offset + 1}"
                            groupsList = groupsList + (newKey to GruppoMuscolare(nome = groupName))
                        }
                        selectedGruppi.keys.forEach { selectedGruppi[it] = false }
                        showAddGroupDialog = false
                    },
                    enabled = !isSaving
                ) { Text("Aggiungi") }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showAddGroupDialog = false
                        selectedGruppi.keys.forEach { selectedGruppi[it] = false }
                    },
                    enabled = !isSaving
                ) { Text("Annulla") }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuscleGroupItem(
    muscleGroup: GruppoMuscolare,
    enabled: Boolean = true,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) {
    var showRemoveDialog by remember { mutableStateOf(false) }

    if (showRemoveDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text("Conferma Rimozione") },
            text = { Text("Sei sicuro di voler rimuovere questo gruppo?") },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveDialog = false
                        onRemove()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Conferma") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRemoveDialog = false }) { Text("Annulla") }
            },
            shape = RoundedCornerShape(8.dp)
        )
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = enabled) { onEdit() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = muscleGroup.nome,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Esercizi: ${muscleGroup.esercizi.count()}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row {
                IconButton(onClick = onMoveUp, enabled = enabled) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowUp, contentDescription = "Sposta Su")
                }
                IconButton(onClick = onMoveDown, enabled = enabled) {
                    Icon(imageVector = Icons.Filled.KeyboardArrowDown, contentDescription = "Sposta Giù")
                }
                IconButton(onClick = { showRemoveDialog = true }, enabled = enabled) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Rimuovi")
                }
            }
        }
    }
}
