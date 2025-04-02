package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.EsercizioPredefinito
import com.matthew.sportiliapp.model.EserciziPredefinitiViewModel
import com.matthew.sportiliapp.model.GruppoMuscolare
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

/** Stato per ciascun esercizio all’interno del dialog (sia principale che extra) */
data class ExerciseInputState(
    var exerciseName: String = "",
    var numeroSerie: Int = 3,
    var numeroRipetizioni: Int = 10,
    var customSerieText: String = ""
)

// ----------------- MAIN SCREEN -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMuscleGroupScreen(
    userCode: String,
    dayKey: String,
    group: GruppoMuscolare,
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

    // Trova gli esercizi predefiniti associati a questo gruppo, se esistono
    val predefinitiGruppo = remember(predefiniti) {
        predefiniti.firstOrNull { it.nome == group.nome }?.esercizi ?: emptyList()
    }

    // Stato: nome del gruppo
    var groupName by remember { mutableStateOf(group.nome) }

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

    // Dialog per aggiungere esercizio: se non null, indica l'EsercizioPredefinito (o custom) che stiamo inserendo
    var predefinitoToAdd by remember { mutableStateOf<EsercizioPredefinito?>(null) }

    // BottomSheet con lista esercizi selezionati
    var showSelectedSheet by remember { mutableStateOf(false) }

    BackHandler {
        // Salva l'ordine
        val exercisesMap = linkedMapOf<String, Esercizio>()
        selectedExercises.forEachIndexed { index, entry ->
            exercisesMap["esercizio${index + 1}"] = entry.exercise
        }
        val updatedGroup = group.copy(nome = groupName, esercizi = exercisesMap)
        onSave(updatedGroup)
        onCancel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
                actions = {
                    // Pulsante Info --> bottom sheet con la lista di esercizi selezionati
                    IconButton(onClick = { showSelectedSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Visualizza Esercizi Aggiunti")
                    }
                    // Pulsante per aggiungere un nuovo esercizio
                    IconButton(onClick = {
                        predefinitoToAdd = EsercizioPredefinito(
                            id = "Custom",
                            nome = "",
                            imageurl = ""
                        )
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Aggiungi Esercizio")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Cerca esercizio...") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 2) Lista esercizi predefiniti (filtrati)
            val filteredExercises = remember(searchText, predefinitiGruppo) {
                if (searchText.isBlank()) predefinitiGruppo
                else predefinitiGruppo.filter {
                    it.nome.contains(searchText, ignoreCase = true)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredExercises) { esercizioPredefinito ->
                    // Verifichiamo se è già nella lista selezionata
                    val isAlreadySelected = selectedExercises.any { it.exercise.name.contains(esercizioPredefinito.nome) }
                    PredefinedExerciseCard(
                        esercizioPredefinito = esercizioPredefinito,
                        isSelected = isAlreadySelected,
                        onClick = {
                            // Apri il dialog per aggiungere esercizi (con possibilità di più esercizi, inclusa selezione predefinita per extra)
                            predefinitoToAdd = esercizioPredefinito
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4) Pulsanti di Annulla/Salva
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
                        // Salva l'ordine
                        val exercisesMap = linkedMapOf<String, Esercizio>()
                        selectedExercises.forEachIndexed { index, entry ->
                            exercisesMap["esercizio${index + 1}"] = entry.exercise
                        }
                        val updatedGroup = group.copy(nome = groupName, esercizi = exercisesMap)
                        onSave(updatedGroup)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salva")
                }
            }
        }
    }

    // Dialog Stepper per aggiungere esercizio (con possibilità di inserire extra esercizi)
    if (predefinitoToAdd != null) {
        AddEsercizioDialog(
            esercizioPredefinito = predefinitoToAdd!!,
            predefiniti = predefinitiGruppo,
            onDismiss = { predefinitoToAdd = null },
            onEsercizioAdded = { esercizio ->
                // Aggiungi a selectedExercises
                selectedExercises.add(ExerciseEntry(exercise = esercizio))
                predefinitoToAdd = null
            }
        )
    }

    // Bottom sheet con la lista di esercizi selezionati (riordino/rimozione)
    if (showSelectedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSelectedSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SelectedExercisesSheet(
                selectedExercises = selectedExercises,
                onClose = { showSelectedSheet = false }
            )
        }
    }
}

// ----------------- PREDEFINED EXERCISE CARD -----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredefinedExerciseCard(
    esercizioPredefinito: EsercizioPredefinito,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val highlightBorder = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        border = highlightBorder
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
        ) {
            Text(
                text = esercizioPredefinito.nome,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

// ----------------- SELECTED EXERCISES BOTTOM SHEET -----------------
@Composable
fun SelectedExercisesSheet(
    selectedExercises: MutableList<ExerciseEntry>,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text("Esercizi Aggiunti", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1F, fill = false)
        ) {
            items(selectedExercises, key = { it.id }) { entry ->
                ExerciseReorderableItem(
                    entry = entry,
                    onMoveUp = {
                        val currentIndex = selectedExercises.indexOf(entry)
                        if (currentIndex > 0) {
                            selectedExercises.swap(currentIndex, currentIndex - 1)
                        }
                    },
                    onMoveDown = {
                        val currentIndex = selectedExercises.indexOf(entry)
                        if (currentIndex < selectedExercises.size - 1) {
                            selectedExercises.swap(currentIndex, currentIndex + 1)
                        }
                    },
                    onRemove = {
                        selectedExercises.remove(entry)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Chiudi")
        }
    }
}

@Composable
fun ExerciseReorderableItem(
    entry: ExerciseEntry,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.exercise.name, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Text(text = "Serie: ${entry.exercise.serie}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (entry.exercise.riposo?.isNotBlank() == true) {
                    Text(text = "Riposo: ${entry.exercise.riposo}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row {
                IconButton(onClick = onMoveUp) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Move Up")
                }
                IconButton(onClick = onMoveDown) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Move Down")
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }
    }
}

// ------------------ ADD / EDIT EXERCISE DIALOG CON MULTIPLE ESERCIZI ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEsercizioDialog(
    esercizioPredefinito: EsercizioPredefinito,
    predefiniti: List<EsercizioPredefinito> = emptyList(),
    onDismiss: () -> Unit,
    onEsercizioAdded: (Esercizio) -> Unit
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Stati per l'esercizio principale
    var mainExerciseName by remember { mutableStateOf(esercizioPredefinito.nome) }
    var mainSerie by remember { mutableStateOf(3) }
    var mainRipetizioni by remember { mutableStateOf(10) }
    var mainCustomSerie by remember { mutableStateOf("") }

    // Lista di esercizi extra (opzionali)
    val extraExercises = remember { mutableStateListOf<ExerciseInputState>() }

    // Stati per il riposo
    var includeRiposo by remember { mutableStateOf(false) }
    var minutiRiposo by remember { mutableStateOf(1) }
    var secondiRiposo by remember { mutableStateOf(0) }

    var notePT by remember { mutableStateOf("") }

    // Stato per selezionare un esercizio predefinito per un extra (salva l'indice dell'entry in editing)
    var selectingPredefinedIndex by remember { mutableStateOf<Int?>(null) }
    var extraPredefSearchText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val dialogTitle = if (mainExerciseName.isNotBlank()) mainExerciseName else "Aggiungi Esercizio Personalizzato"
            Text(dialogTitle, style = MaterialTheme.typography.titleLarge)
            SectionDivider()

            // ---- SEZIONE ESERCIZIO PRINCIPALE ----
            SectionTitle("Esercizio Principale")
            OutlinedTextField(
                value = mainExerciseName,
                onValueChange = { mainExerciseName = it },
                label = { Text("Nome Esercizio") },
                placeholder = { Text("Panca piana, Trazioni, etc.") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (mainCustomSerie.isEmpty()) {
                Stepper(
                    value = mainSerie,
                    onValueChange = { mainSerie = it },
                    range = 1..30,
                    label = "Serie"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Stepper(
                    value = mainRipetizioni,
                    onValueChange = { mainRipetizioni = it },
                    range = 1..50,
                    label = "Ripetizioni"
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = mainCustomSerie,
                onValueChange = { mainCustomSerie = it },
                label = { Text("Formato Serie (opzionale)") },
                placeholder = { Text("Esempio: 4x8, 2 minuti, etc.") },
                modifier = Modifier.fillMaxWidth()
            )

            SectionDivider()

            // ---- SEZIONE ESERCIZI EXTRA (SUPERSERIE) ----
            SectionTitle("Superserie")
            extraExercises.forEachIndexed { index, exerciseState ->
                ExtraExerciseCard(
                    index = index,
                    exerciseState = exerciseState,
                    onUpdate = { updatedState -> extraExercises[index] = updatedState },
                    onRemove = { extraExercises.removeAt(index) },
                    onSelectPredefinito = { selectingPredefinedIndex = index }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(
                onClick = { extraExercises.add(ExerciseInputState()) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Aggiungi esercizio extra")
            }

            SectionDivider()

            // ---- RIPOSO (opzionale) ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = includeRiposo,
                    onCheckedChange = { includeRiposo = it }
                )
                Text("Includi Riposo", style = MaterialTheme.typography.bodyLarge)
            }
            if (includeRiposo) {
                Spacer(modifier = Modifier.height(8.dp))
                SectionTitle("Tempo di Riposo")
                Spacer(modifier = Modifier.height(8.dp))
                Stepper(
                    value = minutiRiposo,
                    onValueChange = { minutiRiposo = it },
                    range = 0..10,
                    label = "Minuti"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Stepper(
                    value = secondiRiposo,
                    onValueChange = { secondiRiposo = it },
                    range = 0..55 step 5,
                    label = "Secondi"
                )
            }

            SectionDivider()

            // ---- BOTTONI FINALI ----
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Annulla")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        // Combina i dati dell'esercizio principale e degli extra
                        val mainSerieString = if (mainCustomSerie.isNotBlank()) mainCustomSerie else "$mainSerie x $mainRipetizioni"
                        val extraSerieStrings = extraExercises.map {
                            if (it.customSerieText.isNotBlank()) it.customSerieText else "${it.numeroRipetizioni}"
                        }
                        val finalSerie = listOf(mainSerieString)
                            .plus(extraSerieStrings)
                            .joinToString(" + ")
                        val finalName = listOf(if (mainExerciseName.isNotBlank()) mainExerciseName else "Esercizio Personalizzato")
                            .plus(extraExercises.map { if (it.exerciseName.isNotBlank()) it.exerciseName else "Esercizio" })
                            .joinToString(" + ")
                        val riposoString = if (!includeRiposo || (minutiRiposo == 0 && secondiRiposo == 0)) {
                            ""
                        } else {
                            if (secondiRiposo < 10) "${minutiRiposo}'0${secondiRiposo}\"" else "${minutiRiposo}'${secondiRiposo}\""
                        }
                        val nuovoEsercizio = Esercizio(
                            name = finalName,
                            serie = finalSerie,
                            riposo = riposoString,
                            notePT = notePT
                        )
                        onEsercizioAdded(nuovoEsercizio)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Aggiungi")
                }
            }
        }
    }

    // Dialog per selezionare un esercizio predefinito per un extra
    if (selectingPredefinedIndex != null) {
        AlertDialog(
            onDismissRequest = {
                selectingPredefinedIndex = null
                extraPredefSearchText = ""
            },
            title = { Text("Seleziona Esercizio Predefinito") },
            text = {
                Column {
                    OutlinedTextField(
                        value = extraPredefSearchText,
                        onValueChange = { extraPredefSearchText = it },
                        label = { Text("Cerca esercizio...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val filtered = predefiniti.filter { it.nome.contains(extraPredefSearchText, ignoreCase = true) }
                    LazyColumn {
                        items(filtered) { esercizioPredef ->
                            Text(
                                text = esercizioPredef.nome,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectingPredefinedIndex?.let { idx ->
                                            val current = extraExercises[idx]
                                            extraExercises[idx] = current.copy(exerciseName = esercizioPredef.nome)
                                        }
                                        selectingPredefinedIndex = null
                                        extraPredefSearchText = ""
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    selectingPredefinedIndex = null
                    extraPredefSearchText = ""
                }) {
                    Text("Annulla")
                }
            }
        )
    }
}

// ------------------- EXTRA EXERCISE CARD (per esercizi extra nel dialog) ------------------- //
@Composable
fun ExtraExerciseCard(
    index: Int,
    exerciseState: ExerciseInputState,
    onUpdate: (ExerciseInputState) -> Unit,
    onRemove: () -> Unit,
    onSelectPredefinito: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Esercizio Extra ${index + 1}", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Delete, contentDescription = "Rimuovi")
                }
            }
            OutlinedTextField(
                value = exerciseState.exerciseName,
                onValueChange = { newName -> onUpdate(exerciseState.copy(exerciseName = newName)) },
                label = { Text("Nome Esercizio") },
                placeholder = { Text("Esempio: Trazioni") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onSelectPredefinito) {
                Text("Seleziona da predefiniti")
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (exerciseState.customSerieText.isEmpty()) {
                Stepper(
                    value = exerciseState.numeroRipetizioni,
                    onValueChange = { newVal -> onUpdate(exerciseState.copy(numeroRipetizioni = newVal)) },
                    range = 1..50,
                    label = "Ripetizioni"
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = exerciseState.customSerieText,
                onValueChange = { newVal -> onUpdate(exerciseState.copy(customSerieText = newVal)) },
                label = { Text("Formato Serie (opzionale)") },
                placeholder = { Text("Esempio: 10") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Divider con un po' di spazio verticale */
@Composable
fun SectionDivider() {
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))
}

/** Titolo di sezione personalizzato */
@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
}

// ------------------- STEPPER COMPOSABLE ------------------- //
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntProgression,
    label: String
) {
    val step = range.step

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                if (value - step >= range.first) {
                    onValueChange(value - step)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Decrement"
                )
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge
            )
            IconButton(onClick = {
                if (value + step <= range.last) {
                    onValueChange(value + step)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Increment"
                )
            }
        }
    }
}
