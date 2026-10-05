package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.EsercizioPredefinito

/** Full-screen local draft. Confirm only updates its parent group draft, never the repository. */
@Composable
fun EsercizioDialog(
    initialExercise: Esercizio,
    onDismiss: () -> Unit,
    onConfirm: (Esercizio) -> Unit,
    predefiniti: List<EsercizioPredefinito>
) {
    val names = remember(initialExercise) { initialExercise.name.split(" + ") }
    val prescriptions = remember(initialExercise) { initialExercise.serie.split(" + ") }
    val initialMain = prescriptions.firstOrNull().orEmpty()
    val standard = remember(initialExercise) { Regex("^(\\d+)\\s*x\\s*(\\d+)$").matchEntire(initialMain.trim()) }
    var name by remember(initialExercise) { mutableStateOf(names.firstOrNull().orEmpty()) }
    var sets by remember(initialExercise) { mutableIntStateOf(standard?.groupValues?.get(1)?.toIntOrNull() ?: 3) }
    var reps by remember(initialExercise) { mutableIntStateOf(standard?.groupValues?.get(2)?.toIntOrNull() ?: 10) }
    var custom by remember(initialExercise) { mutableStateOf(if (standard == null) initialMain else "") }
    var prescriptionChanged by remember(initialExercise) { mutableStateOf(false) }
    val extras = remember(initialExercise) {
        mutableStateListOf<ExerciseInputState>().apply {
            for (i in 1 until maxOf(names.size, prescriptions.size)) {
                add(ExerciseInputState(exerciseName = names.getOrNull(i).orEmpty(), customSerieText = prescriptions.getOrNull(i).orEmpty()))
            }
        }
    }
    var includeRest by remember(initialExercise) { mutableStateOf(!initialExercise.riposo.isNullOrBlank()) }
    var minutes by remember(initialExercise) { mutableIntStateOf(initialExercise.riposo?.substringBefore("'")?.toIntOrNull() ?: 1) }
    var seconds by remember(initialExercise) { mutableIntStateOf(initialExercise.riposo?.substringAfter("'")?.substringBefore("\"")?.toIntOrNull() ?: 0) }
    var restChanged by remember(initialExercise) { mutableStateOf(false) }
    var trainer by remember(initialExercise) { mutableStateOf(initialExercise.notePT.orEmpty()) }
    var trainerChanged by remember(initialExercise) { mutableStateOf(false) }
    var pickerIndex by remember { mutableStateOf<Int?>(null) }
    var query by remember { mutableStateOf("") }
    var discard by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    var removeExtra by remember { mutableStateOf<Int?>(null) }
    val formScroll = rememberScrollState()

    fun draft(): Esercizio {
        val main = if (!prescriptionChanged && initialMain.isNotBlank()) initialMain
            else custom.takeIf { it.isNotBlank() } ?: "$sets x $reps"
        val rest = if (!restChanged) initialExercise.riposo else if (!includeRest || (minutes == 0 && seconds == 0)) ""
            else "$minutes'${seconds.toString().padStart(2, '0')}\""
        return initialExercise.copy(
            name = (listOf(name) + extras.map { it.exerciseName }).joinToString(" + "),
            serie = (listOf(main) + extras.map { it.customSerieText.takeIf(String::isNotBlank) ?: "${it.numeroRipetizioni}" }).joinToString(" + "),
            riposo = rest, notePT = if (trainerChanged) trainer else initialExercise.notePT)
    }
    // Empty new exercises have a default prescription, but that alone is not a user modification.
    val dirty = name != names.firstOrNull().orEmpty() || prescriptionChanged || restChanged || trainerChanged ||
        extras.map { it.exerciseName } != names.drop(1) || extras.map { it.customSerieText } != prescriptions.drop(1)
    fun exit() { if (pickerIndex != null) { pickerIndex = null; query = "" } else if (dirty) discard = true else onDismiss() }
    fun confirm() {
        validating = true
        if (name.isBlank() || extras.any { it.exerciseName.isBlank() }) return
        val updated = draft()
        onConfirm(updated.copy(name = updated.name.split(" + ").joinToString(" + ") { it.capitalizeExerciseName() }))
    }
    Dialog(onDismissRequest = { exit() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.imePadding(),
                topBar = { AdminContextAppBar(if (pickerIndex == null) "Esercizio" else "Scegli dal catalogo",
                    if (pickerIndex == null) "Bozza del gruppo · ${extras.size + 1} parti" else "Parte ${pickerIndex!! + 2} · Esercizio extra",
                    true, onBack = { exit() }) },
                bottomBar = {
                    if (pickerIndex == null) Surface(tonalElevation = 3.dp) {
                        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp)) {
                            Text("Conferma nella bozza; salva il gruppo per applicare le modifiche.", style = MaterialTheme.typography.bodySmall)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = { exit() }, modifier = Modifier.weight(1f)) { Text("Annulla") }
                                Button(onClick = { confirm() }, modifier = Modifier.weight(1f)) { Text("Salva") }
                            }
                        }
                    }
                }
            ) { padding ->
                if (pickerIndex != null) {
                    Column(Modifier.padding(padding).padding(16.dp).fillMaxSize()) {
                        OutlinedTextField(query, { query = it }, label = { Text("Cerca nel catalogo") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        val filtered = predefiniti.filter { it.nome.contains(query.trim(), ignoreCase = true) }
                        LazyColumn(Modifier.weight(1f)) {
                            if (filtered.isEmpty()) item { Text("Nessun esercizio trovato", Modifier.padding(vertical = 16.dp)) }
                            items(filtered) { predefined ->
                                Text(predefined.nome, Modifier.fillMaxWidth().clickable {
                                    val index = pickerIndex!!
                                    extras[index] = extras[index].copy(exerciseName = predefined.nome)
                                    pickerIndex = null; query = ""
                                }.padding(vertical = 16.dp), style = MaterialTheme.typography.bodyLarge)
                                HorizontalDivider()
                            }
                        }
                    }
                } else Column(Modifier.padding(padding).fillMaxSize().verticalScroll(formScroll).padding(16.dp)) {
                    SectionTitle("Parte principale")
                    OutlinedTextField(name, { name = it }, label = { Text("Nome Esercizio") }, modifier = Modifier.fillMaxWidth(),
                        isError = validating && name.isBlank(), supportingText = { if (validating && name.isBlank()) Text("Inserisci il nome dell'esercizio") })
                    SectionDivider(); SectionTitle("Prescrizione")
                    if (custom.isEmpty()) {
                        Stepper(sets, { sets = it; prescriptionChanged = true }, 1..30, "Serie")
                        Stepper(reps, { reps = it; prescriptionChanged = true }, 1..50, "Ripetizioni")
                    }
                    OutlinedTextField(custom, { custom = it; prescriptionChanged = true }, label = { Text("Formato Serie (opzionale)") },
                        supportingText = { Text("Lascia vuoto per usare serie e ripetizioni. Esempio: 4x8 oppure 2 minuti.") }, modifier = Modifier.fillMaxWidth())
                    SectionDivider(); SectionTitle("Recupero")
                    Row(Modifier.fillMaxWidth().clickable { includeRest = !includeRest; restChanged = true }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(includeRest, { includeRest = it; restChanged = true }); Text("Includi Riposo", Modifier.weight(1f))
                    }
                    if (includeRest) {
                        Stepper(minutes, { minutes = it; restChanged = true }, 0..10, "Minuti")
                        Stepper(seconds, { seconds = it; restChanged = true }, 0..55 step 5, "Secondi")
                    }
                    SectionDivider(); SectionTitle("Parti del superset")
                    Text("Le parti sono eseguite insieme, nell'ordine indicato.", style = MaterialTheme.typography.bodySmall)
                    extras.forEachIndexed { index, state ->
                        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Parte ${index + 2}", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                                    IconButton(onClick = { removeExtra = index }) { Icon(Icons.Default.Delete, "Rimuovi parte ${index + 2}") }
                                }
                                OutlinedTextField(state.exerciseName, { extras[index] = state.copy(exerciseName = it) },
                                    label = { Text("Nome parte ${index + 2}") }, modifier = Modifier.fillMaxWidth(),
                                    isError = validating && state.exerciseName.isBlank(),
                                    supportingText = { if (validating && state.exerciseName.isBlank()) Text("Inserisci il nome della parte") })
                                TextButton(onClick = { pickerIndex = index }) { Text("Scegli parte ${index + 2} dal catalogo") }
                                OutlinedTextField(state.customSerieText, { extras[index] = state.copy(customSerieText = it) },
                                    label = { Text("Prescrizione parte ${index + 2}") }, supportingText = { Text("Se vuoto: ${state.numeroRipetizioni} ripetizioni") }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                    OutlinedButton(onClick = { extras.add(ExerciseInputState()) }, modifier = Modifier.fillMaxWidth()) { Text("Aggiungi esercizio extra") }
                    SectionDivider(); SectionTitle("Istruzioni del trainer")
                    OutlinedTextField(trainer, { trainer = it; trainerChanged = true }, label = { Text("Istruzioni del trainer") },
                        minLines = 3, modifier = Modifier.fillMaxWidth(), supportingText = { Text("Visibili nel dettaglio dell'esercizio. Puoi lasciarle vuote.") })
                }
            }
            if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Scartare la bozza dell'esercizio?") },
                text = { Text("Le modifiche a questo esercizio non saranno aggiunte alla bozza del gruppo.") },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Scarta modifiche") } },
                dismissButton = { TextButton(onClick = { discard = false }) { Text("Continua modifica") } })
            removeExtra?.let { index -> AlertDialog(onDismissRequest = { removeExtra = null }, title = { Text("Rimuovere parte ${index + 2}?") },
                text = { Text("La parte sarà rimossa dalla bozza dell'esercizio. Puoi ancora scartare tutte le modifiche prima di confermare.") },
                confirmButton = { TextButton(onClick = { extras.removeAt(index); removeExtra = null }) { Text("Rimuovi parte") } },
                dismissButton = { TextButton(onClick = { removeExtra = null }) { Text("Annulla") } }) }
        }
    }
}

@Composable
fun SectionDivider() { Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(16.dp)) }

@Composable
fun SectionTitle(text: String) { Text(text, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp)) }

@Composable
fun Stepper(value: Int, onValueChange: (Int) -> Unit, range: IntProgression, label: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(enabled = value - range.step >= range.first, onClick = { onValueChange(value - range.step) }) {
                Icon(Icons.Default.KeyboardArrowDown, "Diminuisci $label")
            }
            Text(value.toString(), style = MaterialTheme.typography.bodyLarge)
            IconButton(enabled = value + range.step <= range.last, onClick = { onValueChange(value + range.step) }) {
                Icon(Icons.Default.KeyboardArrowUp, "Aumenta $label")
            }
        }
    }
}
