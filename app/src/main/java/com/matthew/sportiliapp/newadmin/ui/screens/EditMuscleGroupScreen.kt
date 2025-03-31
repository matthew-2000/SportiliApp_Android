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
                            // Apri la stessa dialog (stavolta per un esercizio predefinito)
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

    // Dialog Stepper (sia per predefinito che custom)
    if (predefinitoToAdd != null) {
        AddEsercizioDialog(
            esercizioPredefinito = predefinitoToAdd!!,
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
            modifier = Modifier.weight(1f, fill = false)
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
                if(entry.exercise.riposo?.isNotBlank() == true) {
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

// ------------------ ADD / EDIT EXERCISE DIALOG (con superset) ------------------
// ------------------ ADD / EDIT EXERCISE DIALOG (con Superserie e UI migliorata) ------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEsercizioDialog(
    esercizioPredefinito: EsercizioPredefinito,
    onDismiss: () -> Unit,
    onEsercizioAdded: (Esercizio) -> Unit
) {
    // Stato del bottom sheet
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Name can be empty if custom
    var exerciseName by remember { mutableStateOf(esercizioPredefinito.nome) }

    // --- Stati primo esercizio ---
    var numeroSerie by remember { mutableStateOf(3) }
    var numeroRipetizioni by remember { mutableStateOf(10) }
    var customSerieText by remember { mutableStateOf("") }

    // --- Stati per superserie ---
    var enableSuperserie by remember { mutableStateOf(false) }

    var exerciseName2 by remember { mutableStateOf("") }
    var numeroSerie2 by remember { mutableStateOf(3) }
    var numeroRipetizioni2 by remember { mutableStateOf(10) }
    var customSerieText2 by remember { mutableStateOf("") }

    // Altre info
    var includeRiposo by remember { mutableStateOf(false) }
    var minutiRiposo by remember { mutableStateOf(1) }
    var secondiRiposo by remember { mutableStateOf(0) }

    var notePT by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState
    ) {
        // Per scorrere se il contenuto è molto
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Titolo in alto
            val dialogTitle = exerciseName.ifBlank { "Aggiungi Esercizio Personalizzato" }
            Text(dialogTitle, style = MaterialTheme.typography.titleLarge)

            SectionDivider()

            // ---- SEZIONE ESERCIZIO 1 ----
            SectionTitle("Esercizio")

            OutlinedTextField(
                value = exerciseName,
                onValueChange = { exerciseName = it },
                label = { Text("Nome Esercizio") },
                placeholder = { Text("Panca piana, Trazioni, etc.") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (customSerieText.isEmpty()) {
                Stepper(
                    value = numeroSerie,
                    onValueChange = { numeroSerie = it },
                    range = 1..30,
                    label = "Serie"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Stepper(
                    value = numeroRipetizioni,
                    onValueChange = { numeroRipetizioni = it },
                    range = 1..50,
                    label = "Ripetizioni"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = customSerieText,
                onValueChange = { customSerieText = it },
                label = { Text("Formato Serie (opzionale)") },
                placeholder = { Text("Esempio: 4x8, 2 minuti, etc.") },
                modifier = Modifier.fillMaxWidth()
            )

            SectionDivider()

            // ---- OPZIONE SUPERSERIE ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = enableSuperserie,
                    onCheckedChange = { enableSuperserie = it }
                )
                Text("Abilita Superserie", style = MaterialTheme.typography.bodyLarge)
            }

            // ---- SEZIONE ESERCIZIO 2 (visibile solo se la Superserie è abilitata) ----
            if (enableSuperserie) {
                SectionTitle("Esercizio 2")

                OutlinedTextField(
                    value = exerciseName2,
                    onValueChange = { exerciseName2 = it },
                    label = { Text("Nome Esercizio 2") },
                    placeholder = { Text("Croci, Trazioni inverse, etc.") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (customSerieText2.isEmpty()) {
                    Stepper(
                        value = numeroSerie2,
                        onValueChange = { numeroSerie2 = it },
                        range = 1..30,
                        label = "Serie"
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Stepper(
                        value = numeroRipetizioni2,
                        onValueChange = { numeroRipetizioni2 = it },
                        range = 1..50,
                        label = "Ripetizioni"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customSerieText2,
                    onValueChange = { customSerieText2 = it },
                    label = { Text("Formato Serie 2 (opzionale)") },
                    placeholder = { Text("Esempio: 4x8, 2 minuti, etc.") },
                    modifier = Modifier.fillMaxWidth()
                )
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
                        // Calcolo SERIE per il primo esercizio
                        val firstSerie = if (customSerieText.isNotBlank()) {
                            customSerieText
                        } else {
                            "$numeroSerie x $numeroRipetizioni"
                        }

                        // Calcolo SERIE per il secondo esercizio (se superserie)
                        val secondSerie = if (enableSuperserie) {
                            if (customSerieText2.isNotBlank()) {
                                customSerieText2
                            } else {
                                "$numeroSerie2 x $numeroRipetizioni2"
                            }
                        } else {
                            ""
                        }

                        // Costruisci nome e serie finali
                        val finalName = if (!enableSuperserie || exerciseName2.isBlank()) {
                            // Caso normale, o superset disabilitata
                            exerciseName.ifBlank { "Esercizio Personalizzato" }
                        } else {
                            // Caso superset: "Esercizio1 + Esercizio2"
                            "${exerciseName.ifBlank { "Esercizio Personalizzato" }} + ${exerciseName2}"
                        }

                        val finalSerie = if (!enableSuperserie || secondSerie.isBlank()) {
                            firstSerie
                        } else {
                            "$firstSerie + $secondSerie"
                        }

                        // Calcolo riposo (se incluso)
                        val riposoString = if (!includeRiposo || (minutiRiposo == 0 && secondiRiposo == 0)) {
                            ""
                        } else {
                            if (secondiRiposo < 10) {
                                "${minutiRiposo}'0${secondiRiposo}\""
                            } else {
                                "${minutiRiposo}'${secondiRiposo}\""
                            }
                        }

                        // Crea l'oggetto Esercizio
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
            // Decrement
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
            // Increment
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
