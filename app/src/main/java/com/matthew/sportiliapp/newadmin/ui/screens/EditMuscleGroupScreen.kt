package com.matthew.sportiliapp.newadmin.ui.screens

import android.annotation.SuppressLint
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

/** Stato per ciascun esercizio extra nel dialog (sia in aggiunta che in editing) */
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

    // Stato: nome del gruppo
    var groupName by remember { mutableStateOf(group.nome) }

    // *** NOVITÀ: se il gruppo è "Circuito", usa tutti gli esercizi da tutti i gruppi ***
    // Altrimenti, usa solo quelli del gruppo selezionato come prima
    val predefinitiGruppo = remember(predefiniti, groupName) {
        val isCircuito = groupName.equals("Circuito", ignoreCase = true)
        if (isCircuito) {
            predefiniti
                .flatMap { it.esercizi }
                .distinctBy { it.nome } // evita duplicati di nome
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
    // Stato per mostrare la sheet degli esercizi selezionati
    var showSelectedSheet by remember { mutableStateOf(false) }

    BackHandler {
        // Al back, salva l'ordine
        val exercisesMap = linkedMapOf<String, Esercizio>()
        selectedExercises.forEachIndexed { index, entry ->
            exercisesMap["esercizio${index + 1}"] = entry.exercise
        }
        val updatedGroup = group.copy(nome = groupName, esercizi = exercisesMap)
        onSave(updatedGroup)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
                actions = {
                    // Pulsante Info --> mostra la lista degli esercizi selezionati
                    IconButton(onClick = { showSelectedSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Visualizza Esercizi Aggiunti")
                    }
                    // Pulsante per aggiungere un nuovo esercizio
                    IconButton(onClick = {
                        // Passa null per indicare “aggiungi nuovo”
                        exerciseDialogInitial = Esercizio(
                            name = "",
                            serie = "",
                            riposo = null,
                            notePT = ""
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

            // Lista esercizi predefiniti (filtrati)
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
                    // Verifica se già selezionato
                    val isAlreadySelected = selectedExercises.any { it.exercise.name.contains(esercizioPredefinito.nome) }
                    PredefinedExerciseCard(
                        esercizioPredefinito = esercizioPredefinito,
                        isSelected = isAlreadySelected,
                        onClick = {
                            // Usa il dialog per aggiungere, inizializzando correttamente il nome dall'oggetto predefinito
                            exerciseDialogInitial = Esercizio(
                                name = esercizioPredefinito.nome, // Usa il nome del predefinito
                                serie = "",
                                riposo = null,
                                notePT = ""
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pulsanti Annulla / Visualizza
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
                    onClick = { showSelectedSheet = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Visualizza")
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

    // Bottom sheet per visualizzare gli esercizi aggiunti, con possibilità di editing via click
    if (showSelectedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSelectedSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            SelectedExercisesSheet(
                selectedExercises = selectedExercises,
                onClose = { showSelectedSheet = false },
                onSave = {
                    val exercisesMap = linkedMapOf<String, Esercizio>()
                    selectedExercises.forEachIndexed { index, entry ->
                        exercisesMap["esercizio${index + 1}"] = entry.exercise
                    }
                    val updatedGroup = group.copy(nome = groupName, esercizi = exercisesMap)
                    onSave(updatedGroup)
                },
                onEdit = { entry ->
                    // Quando si clicca su un item, apri il dialog in modalità editing
                    exerciseEntryInEdit = entry
                }
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
        Box(modifier = Modifier.padding(12.dp)) {
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
    onClose: () -> Unit,
    onSave: () -> Unit,
    onEdit: (ExerciseEntry) -> Unit
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
                    onItemClick = { onEdit(entry) },
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
                    onRemove = { selectedExercises.remove(entry) }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("Annulla")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f)
            ) {
                Text("Salva")
            }
        }
    }
}

@Composable
fun ExerciseReorderableItem(
    entry: ExerciseEntry,
    onItemClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
            .padding(4.dp)
    ) {
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

// ------------------- DIALOG PER AGGIUNGERE/EDITARE UN ESERCIZIO -------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsercizioDialog(
    initialExercise: Esercizio,
    onDismiss: () -> Unit,
    onConfirm: (Esercizio) -> Unit,
    predefiniti: List<EsercizioPredefinito>,
) {
    // Se l'esercizio è in editing, proviamo a precompilare i campi
    // Per il main usiamo la prima parte del nome e della serie (se nel formato "x + y + ...")
    val nameParts = initialExercise.name.split(" + ").map { it.trim() }
    val serieParts = initialExercise.serie.split(" + ").map { it.trim() }

    var mainExerciseName by remember { mutableStateOf(if (nameParts.isNotEmpty()) nameParts[0] else "") }
    var mainSerie by remember { mutableStateOf(3) }
    var mainRipetizioni by remember { mutableStateOf(10) }
    var mainCustomSerie by remember { mutableStateOf("") }

    // Se la prima parte della serie è nel formato "n x m" allora la parsiamo
    if (serieParts.isNotEmpty() && serieParts[0].contains("x")) {
        val parts = serieParts[0].split("x").map { it.trim() }
        mainSerie = parts.getOrNull(0)?.toIntOrNull() ?: 3
        mainRipetizioni = parts.getOrNull(1)?.toIntOrNull() ?: 10
    } else if (serieParts.isNotEmpty() && serieParts[0].isNotEmpty()){
        mainCustomSerie = serieParts[0]
    }

    // Precompiliamo la lista degli extra (se esistono)
    val extraExercisesInitial = remember { mutableStateListOf<ExerciseInputState>() }
    if (nameParts.size > 1 && serieParts.size > 1) {
        // Per ogni extra, la posizione corrisponde
        for (i in 1 until minOf(nameParts.size, serieParts.size)) {
            extraExercisesInitial.add(
                ExerciseInputState(
                    exerciseName = nameParts[i],
                    customSerieText = serieParts[i]
                )
            )
        }
    }

    // Riposo: se presente, proviamo a parsarlo nel formato m'ss" (semplice parsing)
    var includeRiposo by remember { mutableStateOf(initialExercise.riposo?.isNotBlank() == true) }
    var minutiRiposo by remember { mutableStateOf( if (includeRiposo) initialExercise.riposo?.substringBefore("'")?.toIntOrNull() ?: 1 else 1) }
    var secondiRiposo by remember { mutableStateOf( if (includeRiposo) initialExercise.riposo?.substringAfter("'")?.substringBefore("\"")?.toIntOrNull() ?: 0 else 0) }

    // Note (opzionale)
    var notePT by remember { mutableStateOf(initialExercise.notePT) }

    // Stati per esercizi extra (superserie)
    val extraExercises = remember { extraExercisesInitial.toMutableStateList() }
    // Stato per selezionare un esercizio predefinito per un extra
    var selectingPredefinedIndex by remember { mutableStateOf<Int?>(null) }
    var extraPredefSearchText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            val dialogTitle = if (mainExerciseName.isNotBlank()) mainExerciseName else "Esercizio Personalizzato"
            Text(dialogTitle, style = MaterialTheme.typography.titleLarge)
            SectionDivider()

            // ---- SEZIONE ESERCIZIO PRINCIPALE ----
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
                        onConfirm(nuovoEsercizio)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salva")
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

// ------------------- EXTRA EXERCISE CARD ------------------- //
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
                Text(text = "Esercizio Extra ${index + 1}", style = MaterialTheme.typography.titleSmall)
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
            OutlinedTextField(
                value = exerciseState.customSerieText,
                onValueChange = { newText -> onUpdate(exerciseState.copy(customSerieText = newText)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Divider con spazio verticale */
@Composable
fun SectionDivider() {
    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(modifier = Modifier.height(16.dp))
}

/** Titolo di sezione */
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
                if (value - step >= range.first) onValueChange(value - step)
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
                if (value + step <= range.last) onValueChange(value + step)
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Increment"
                )
            }
        }
    }
}
