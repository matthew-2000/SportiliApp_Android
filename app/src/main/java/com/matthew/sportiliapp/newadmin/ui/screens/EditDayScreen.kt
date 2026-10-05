package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
    userCode: String = "",
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
        if (isSaving) return
        showExitDialog = false
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

    BackHandler {
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
        modifier = Modifier.imePadding(),
        topBar = {
            AdminContextAppBar(
                title = "Modifica giorno",
                context = "Utente $userCode · Scheda · ${day.name}",
                enabled = !isSaving,
                onBack = { requestExit() }
            )
        },
        bottomBar = {
            AdminEditorBottomBar(
                isDirty = isDirty,
                isSaving = isSaving,
                onCancel = { requestExit() },
                onSave = { validateAndSave(onSave) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            item {
                if (isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                }
                errorMessage?.let { message ->
                    AdminEditorErrorBanner(message, onRetry = { validateAndSave(onSave) })
                    Spacer(modifier = Modifier.height(12.dp))
                }

                AdminEditorSection(
                    title = "Dati giorno",
                    supportingText = "Il nome identifica il giorno nella scheda dell’utente."
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dayName,
                    onValueChange = {
                        dayName = it
                        if (dayNameError != null) dayNameError = null
                    },
                    label = { Text("Nome del giorno") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    isError = dayNameError != null,
                    supportingText = dayNameError?.let { { Text(it) } }
                )

                Spacer(modifier = Modifier.height(16.dp))
                AdminEditorSection(
                    title = "Gruppi muscolari",
                    supportingText = "Ordina i gruppi. Aprire un gruppo salva prima la bozza del giorno."
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showAddGroupDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Aggiungi gruppi")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (groupsList.isEmpty()) {
                item { Text("Nessun gruppo. Aggiungi i gruppi per questo giorno.", modifier = Modifier.padding(vertical = 12.dp)) }
            }
            itemsIndexed(groupsList) { index, (groupKey, muscleGroup) ->
                MuscleGroupItem(
                    muscleGroup = muscleGroup,
                    enabled = !isSaving,
                    position = index + 1,
                    total = groupsList.size,
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
    }

    if (showAddGroupDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddGroupDialog = false
                selectedGruppi.keys.forEach { selectedGruppi[it] = false }
            },
            title = { Text("Aggiungi gruppi muscolari") },
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
                        val selectedNames = selectedGruppi.filterValues { it }.keys

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

@Composable
fun MuscleGroupItem(
    muscleGroup: GruppoMuscolare,
    enabled: Boolean = true,
    position: Int = 1,
    total: Int = 1,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    onEdit: () -> Unit
) = AdminOrderedItem(muscleGroup.nome, "Esercizi: ${muscleGroup.esercizi.size}",
    "gruppo", position, total, enabled, onMoveUp, onMoveDown, onRemove, onEdit)
