package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.EsercizioPredefinito
import com.matthew.sportiliapp.model.EserciziPredefinitiViewModel
import com.matthew.sportiliapp.model.GruppoMuscolarePredefinito
import com.matthew.sportiliapp.model.GruppoMuscolare
import java.util.Locale
import java.util.UUID

// ----------------- Data classes & Utilities -----------------
data class ExerciseEntry(
    val id: String = UUID.randomUUID().toString(),
    var exercise: Esercizio
)

fun <T> MutableList<T>.swap(index1: Int, index2: Int) {
    val tmp = this[index1]
    this[index1] = this[index2]
    this[index2] = tmp
}

/** Stato per ciascun esercizio extra nel dialog (sia in aggiunta che in editing) */
data class ExerciseInputState(
    var exerciseName: String = "",
    var numeroSerie: Int = 3,
    var numeroRipetizioni: Int = 10,
    var customSerieText: String = ""
)

private fun String.normalizeForExerciseComparison(): String =
    trim().lowercase(Locale.getDefault())

internal fun String.capitalizeExerciseName(): String {
    val trimmed = trim()
    if (trimmed.isEmpty()) return ""
    val lower = trimmed.lowercase(Locale.getDefault())
    return lower.replaceFirstChar { char ->
        if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
    }
}

private fun formatCompositeExerciseName(rawName: String): String {
    if (rawName.isBlank()) return rawName
    return rawName.split(" + ").joinToString(" + ") { part -> part.capitalizeExerciseName() }
}

private fun Esercizio.containsExerciseName(targetName: String): Boolean {
    val normalizedTarget = targetName.normalizeForExerciseComparison()
    if (normalizedTarget.isEmpty()) return false
    return name.split(" + ")
        .map { it.normalizeForExerciseComparison() }
        .any { it == normalizedTarget }
}

// ----------------- MAIN SCREEN -----------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditMuscleGroupScreen(
    userCode: String,
    dayKey: String,
    group: GruppoMuscolare,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onSave: (GruppoMuscolare) -> Unit,
    onCancel: () -> Unit
) {
    val viewModel: EserciziPredefinitiViewModel = viewModel()

    // Osserva i gruppi predefiniti
    val predefiniti by viewModel.gruppiMuscolariPredefiniti.observeAsState(emptyList())
    LaunchedEffect(predefiniti) {
        if (predefiniti.isEmpty()) {
            viewModel.fetchWorkoutData()
        }
    }

    EditMuscleGroupContent(userCode, dayKey, group, predefiniti, isSaving, errorMessage, onSave, onCancel)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun EditMuscleGroupContent(
    userCode: String,
    dayKey: String,
    group: GruppoMuscolare,
    predefiniti: List<GruppoMuscolarePredefinito>,
    isSaving: Boolean = false,
    errorMessage: String? = null,
    onSave: (GruppoMuscolare) -> Unit,
    onCancel: () -> Unit
) {
    // Stato: nome del gruppo
    var groupName by remember { mutableStateOf(group.nome) }

    // *** NOVITÀ: se il gruppo è "Circuito", usa tutti gli esercizi da tutti i gruppi ***
    // Altrimenti, usa solo quelli del gruppo selezionato come prima
    val predefinitiGruppo = remember(predefiniti, groupName) {
        val isCircuito = groupName.equals("Circuito", ignoreCase = true)

        if (isCircuito) {
            filterCircuitExerciseGroups(predefiniti, "")
                // Combina tutti gli esercizi in ordine
                .flatMap { gruppo ->
                    gruppo.esercizi.map { esercizio ->
                        esercizio.copy(nome = "${gruppo.nome}: ${esercizio.nome}")
                    }
                }

        } else {
            predefiniti.firstOrNull { it.nome.equals(groupName, ignoreCase = true) }?.esercizi
                ?: emptyList()
        }
    }

    // Stato: barra di ricerca
    var searchText by remember { mutableStateOf("") }

    // Lista di esercizi selezionati
    val selectedExercises = remember { mutableStateListOf<ExerciseEntry>() }

    // Inizializza con gli esercizi esistenti
    LaunchedEffect(group) {
        selectedExercises.clear()
        group.esercizi.forEach { (_, ex) ->
            selectedExercises.add(ExerciseEntry(exercise = ex))
        }
    }

    // Stato per aprire il dialog in modalità “aggiungi”
    var exerciseDialogInitial by remember { mutableStateOf<Esercizio?>(null) }
    // Stato per aprire il dialog in modalità “modifica”
    var exerciseEntryInEdit by remember { mutableStateOf<ExerciseEntry?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    val initialExercises = remember(group) {
        group.esercizi.values.map { exercise ->
            exercise.copy(name = formatCompositeExerciseName(exercise.name))
        }
    }
    val currentExercises = remember(selectedExercises.toList()) {
        selectedExercises.map { entry ->
            entry.exercise.copy(name = formatCompositeExerciseName(entry.exercise.name))
        }
    }
    val isDirty = remember(groupName, currentExercises, group) {
        groupName.trim() != group.nome || currentExercises != initialExercises
    }

    fun buildUpdatedGroup(): GruppoMuscolare {
        val exercisesMap = linkedMapOf<String, Esercizio>()
        currentExercises.forEachIndexed { index, exercise ->
            exercisesMap["esercizio${index + 1}"] = exercise.copy(ordine = index)
        }
        return group.copy(nome = groupName.trim(), esercizi = exercisesMap)
    }

    fun requestExit() {
        if (isSaving) return
        if (isDirty) {
            showExitDialog = true
        } else {
            onCancel()
        }
    }

    fun startNewExercise() {
        exerciseDialogInitial = Esercizio(
            name = "",
            serie = "",
            riposo = null,
            notePT = ""
        )
    }

    BackHandler {
        requestExit()
    }

    if (showExitDialog) {
        UnsavedChangesDialog(
            onSave = {
                showExitDialog = false
                onSave(buildUpdatedGroup())
            },
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
                title = groupName, context = "Utente $userCode · Scheda · $dayKey · Gruppo",
                enabled = !isSaving, onBack = { requestExit() }
            )
        },
        bottomBar = {
            AdminEditorBottomBar(
                isDirty = isDirty,
                isSaving = isSaving,
                onCancel = { requestExit() },
                onSave = { onSave(buildUpdatedGroup()) }
            )
        }
    ) { paddingValues ->
        val filteredExercises = remember(searchText, predefinitiGruppo) {
            predefinitiGruppo.filter { it.nome.contains(searchText.trim(), ignoreCase = true) }
        }
        val circuitGroups = remember(searchText, predefiniti) {
            filterCircuitExerciseGroups(predefiniti, searchText)
        }
        val noResults = if (groupName.equals("Circuito", ignoreCase = true)) {
            circuitGroups.isEmpty()
        } else filteredExercises.isEmpty()
        LazyColumn(
            modifier = Modifier.padding(paddingValues).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                if (isSaving) LinearProgressIndicator(Modifier.fillMaxWidth())
                errorMessage?.let { AdminEditorErrorBanner(it, onRetry = { onSave(buildUpdatedGroup()) }) }
                AdminEditorSection("Esercizi selezionati", "${selectedExercises.size} esercizi nel gruppo. Modifica e riordina la bozza, poi salva il gruppo.")
            }
            if (selectedExercises.isEmpty()) item { Text("Nessun esercizio aggiunto. Scegli dal catalogo o crea un esercizio personalizzato.") }
            items(selectedExercises, key = { it.id }) { entry ->
                val index = selectedExercises.indexOfFirst { it.id == entry.id }
                AdminOrderedItem(
                    name = entry.exercise.name,
                    summary = "Prescrizione: ${entry.exercise.serie}" +
                        (entry.exercise.riposo?.takeIf { it.isNotBlank() }?.let { "\nRecupero: $it" } ?: "") +
                        (if (entry.exercise.notePT.isNullOrBlank()) "" else "\nIstruzioni trainer presenti"),
                    kind = "esercizio", position = index + 1, total = selectedExercises.size,
                    enabled = !isSaving,
                    onMoveUp = { if (index > 0) selectedExercises.swap(index, index - 1) },
                    onMoveDown = { if (index < selectedExercises.lastIndex) selectedExercises.swap(index, index + 1) },
                    onRemove = { selectedExercises.removeAll { it.id == entry.id } },
                    onEdit = { exerciseEntryInEdit = entry },
                    editLabel = "Modifica",
                    removalSupportingText = "La rimozione sarà applicata quando salvi il gruppo. Puoi annullarla uscendo e scartando la bozza."
                )
            }
            item {
                Button(onClick = { startNewExercise() }, modifier = Modifier.fillMaxWidth(), enabled = !isSaving) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Nuovo esercizio")
                }
                Spacer(Modifier.height(16.dp))
                AdminEditorSection("Catalogo esercizi", "Cerca e aggiungi alla bozza. Gli esercizi già presenti possono essere aggiunti di nuovo.")
                OutlinedTextField(searchText, { searchText = it }, label = { Text("Cerca esercizio...") },
                    modifier = Modifier.fillMaxWidth(), enabled = !isSaving, singleLine = true,
                    trailingIcon = { if (searchText.isNotEmpty()) TextButton(onClick = { searchText = "" }, enabled = !isSaving) { Text("Cancella") } })
            }
            if (noResults) {
                item { Text("Nessun esercizio trovato", modifier = Modifier.padding(12.dp)) }
            } else if (groupName.equals("Circuito", ignoreCase = true)) {
                circuitGroups.forEach { gruppo ->
                    // Sticky header per ogni gruppo muscolare
                    stickyHeader {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = gruppo.nome,
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Lista degli esercizi del gruppo
                    items(gruppo.esercizi) { esercizioPredefinito ->
                        val isAlreadySelected = selectedExercises.any {
                            it.exercise.containsExerciseName(esercizioPredefinito.nome)
                        }
                        PredefinedExerciseCard(
                            esercizioPredefinito = esercizioPredefinito,
                            isSelected = isAlreadySelected,
                            enabled = !isSaving,
                            onClick = {
                                exerciseDialogInitial = Esercizio(
                                    name = esercizioPredefinito.nome,
                                    serie = "",
                                    riposo = null,
                                    notePT = ""
                                )
                            }
                        )
                    }
                }
            } else {
                items(filteredExercises) { esercizioPredefinito ->
                    val isAlreadySelected = selectedExercises.any {
                        it.exercise.containsExerciseName(esercizioPredefinito.nome)
                    }
                    PredefinedExerciseCard(
                        esercizioPredefinito = esercizioPredefinito,
                        isSelected = isAlreadySelected,
                        enabled = !isSaving,
                        onClick = {
                            exerciseDialogInitial = Esercizio(
                                name = esercizioPredefinito.nome,
                                serie = "",
                                riposo = null,
                                notePT = ""
                            )
                        }
                    )
                }
            }
        }
    }

    // Dialog per Aggiungere un nuovo esercizio o modificare uno esistente
    if (exerciseDialogInitial != null || exerciseEntryInEdit != null) {
        // Se in editing, recupera i dati dall'entry; altrimenti, usa quelli passati in aggiunta
        val initialExercise = exerciseEntryInEdit?.exercise ?: exerciseDialogInitial!!
        EsercizioDialog(
            initialExercise = initialExercise,
            onDismiss = {
                exerciseDialogInitial = null
                exerciseEntryInEdit = null
            },
            onConfirm = { updatedExercise ->
                if (exerciseEntryInEdit != null) {
                    // Aggiorna la voce in editing
                    val index = selectedExercises.indexOfFirst { it.id == exerciseEntryInEdit!!.id }
                    if (index != -1) {
                        selectedExercises[index] = selectedExercises[index].copy(exercise = updatedExercise)
                    }
                    exerciseEntryInEdit = null
                } else {
                    // Aggiungi nuova voce
                    selectedExercises.add(ExerciseEntry(exercise = updatedExercise))
                    exerciseDialogInitial = null
                }
            },
            // *** Passiamo l'elenco determinato sopra: per "Circuito" conterrà tutti i predefiniti ***
            predefiniti = predefinitiGruppo
        )
    }

}

// ----------------- PREDEFINED EXERCISE CARD -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredefinedExerciseCard(
    esercizioPredefinito: EsercizioPredefinito,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val highlightBorder = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClickLabel = "Aggiungi ${esercizioPredefinito.nome}", onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
        border = highlightBorder
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = esercizioPredefinito.nome,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(if (isSelected) "Già presente · Aggiungi ancora" else "Aggiungi alla bozza",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
