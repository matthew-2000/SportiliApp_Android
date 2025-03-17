package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
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

    // Dialog per aggiungere esercizio: se non null, indica l'esercizioPredefinito che stiamo inserendo
    var predefinitoToAdd by remember { mutableStateOf<EsercizioPredefinito?>(null) }

    // BottomSheet con lista esercizi selezionati
    var showSelectedSheet by remember { mutableStateOf(false) }

    BackHandler { onCancel() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
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
                    // Verifichiamo se è già nella lista selezionati
                    val isAlreadySelected = selectedExercises.any { it.exercise.name == esercizioPredefinito.nome }
                    PredefinedExerciseCard(
                        esercizioPredefinito = esercizioPredefinito,
                        isSelected = isAlreadySelected,
                        onClick = {
                            // Apri la dialog con Stepper e campi vari
                            predefinitoToAdd = esercizioPredefinito
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3) Pulsante per aprire la bottom sheet degli esercizi selezionati
            Button(
                onClick = { showSelectedSheet = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Visualizza Esercizi Aggiunti")
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

    // Dialog Stepper (se cliccato su un esercizio predefinito)
    if (predefinitoToAdd != null) {
        AddEsercizioDialog(
            esercizioBaseName = predefinitoToAdd!!.nome,
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
            onDismissRequest = { showSelectedSheet = false }
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
            Column {
                Text(text = entry.exercise.name, style = MaterialTheme.typography.bodyMedium)
                Text(text = "Serie: ${entry.exercise.serie}")
                Text(text = "Riposo: ${entry.exercise.riposo.orEmpty()}")
                // Se hai note PT o altro, potresti visualizzare qui.
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEsercizioDialog(
    esercizioBaseName: String,           // Nome dell'esercizio predefinito (o una stringa qualunque)
    onDismiss: () -> Unit,               // Azione da eseguire quando si chiude (senza aggiungere)
    onEsercizioAdded: (Esercizio) -> Unit // Azione da eseguire quando si conferma la creazione dell'esercizio
) {
    // STEP 1) Serie e Ripetizioni (con Stepper)
    var numeroSerie by remember { mutableStateOf(3) }
    var numeroRipetizioni by remember { mutableStateOf(10) }

    // STEP 2) Campo personalizzato per sostituire serie x ripetizioni (es: "2 minuti", "4x8", ecc.)
    var customSerieText by remember { mutableStateOf("") }

    // STEP 3) Riposo
    var includeRiposo by remember { mutableStateOf(false) }
    var minutiRiposo by remember { mutableStateOf(0) }
    var secondiRiposo by remember { mutableStateOf(0) }

    // STEP 4) Note PT
    var notePT by remember { mutableStateOf("") }

    // Creiamo un AlertDialog con i campi necessari
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(esercizioBaseName, style = MaterialTheme.typography.bodyMedium) },
        text = {
            Column {
                // -- Serie e Ripetizioni --
                if (customSerieText.isEmpty()) {
                    // Mostriamo i due Stepper solo se il campo custom è vuoto
                    Stepper(
                        value = numeroSerie,
                        onValueChange = { numeroSerie = it },
                        range = 1..30,
                        label = "Serie"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Stepper(
                        value = numeroRipetizioni,
                        onValueChange = { numeroRipetizioni = it },
                        range = 1..50,
                        label = "Ripetizioni"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // -- Campo personalizzato (override) --
                OutlinedTextField(
                    value = customSerieText,
                    onValueChange = { customSerieText = it },
                    label = { Text("Formato Serie (opzionale)") },
                    placeholder = { Text("Esempio: 4x8, 2 minuti, etc.") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // -- Include Riposo --
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = includeRiposo,
                        onCheckedChange = { includeRiposo = it }
                    )
                    Text("Includi Riposo", style = MaterialTheme.typography.bodyLarge)
                }

                if (includeRiposo) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Riposo", style = MaterialTheme.typography.labelLarge)
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
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    // Prepara il testo finale per la sezione "serie"
                    val finalSerie = if (customSerieText.isNotBlank()) {
                        customSerieText
                    } else {
                        // Formato di default: "3 x 10" (se l'utente non ha inserito nulla)
                        "$numeroSerie x $numeroRipetizioni"
                    }

                    // Calcolo riposo (se incluso)
                    val riposoString = if (!includeRiposo || (minutiRiposo == 0 && secondiRiposo == 0)) {
                        ""
                    } else {
                        // Esempio: 1'05"  (1 minuto e 5 secondi)
                        if (secondiRiposo < 10) {
                            "${minutiRiposo}'0${secondiRiposo}\""
                        } else {
                            "${minutiRiposo}'${secondiRiposo}\""
                        }
                    }

                    // Crea l'oggetto Esercizio
                    val nuovoEsercizio = Esercizio(
                        name = esercizioBaseName,
                        serie = finalSerie,
                        riposo = riposoString,
                        notePT = notePT
                    )

                    // Callback
                    onEsercizioAdded(nuovoEsercizio)
                }
            ) { Text("Aggiungi") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
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
