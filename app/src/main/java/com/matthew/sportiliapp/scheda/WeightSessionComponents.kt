package com.matthew.sportiliapp.scheda

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.matthew.sportiliapp.model.WorkoutWeightSession
import com.matthew.sportiliapp.model.WorkoutWeightSet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

internal data class WeightSessionEditorContext(
    val key: String, val name: String, val reps: List<Int>, val session: WorkoutWeightSession? = null,
    val id: String = session?.id ?: UUID.randomUUID().toString(),
    val timestamp: Long = session?.timestamp ?: System.currentTimeMillis()
)

private data class WeightSetDraft(val reps: String = "", val weight: String = "") {
    fun value(): WorkoutWeightSet? {
        val reps = reps.toIntOrNull() ?: return null
        val weight = parsedWeightInput(weight) ?: return null
        return WorkoutWeightSet(reps, weight).takeIf { it.isValid }
    }
}

@Composable
internal fun WeightSessionEditor(
    context: WeightSessionEditorContext,
    sessions: List<WorkoutWeightSession>,
    onDismiss: () -> Unit,
    onSave: (WorkoutWeightSession, () -> Unit, (String) -> Unit) -> Unit
) {
    val initial = remember(context.id) {
        context.session?.sets?.map { WeightSetDraft(it.reps.toString(), it.weight.toString()) }
            ?: context.reps.map { WeightSetDraft(reps = it.toString()) }.ifEmpty { listOf(WeightSetDraft()) }
    }
    var rows by remember(context.id) { mutableStateOf(initial) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmsDiscard by remember { mutableStateOf(false) }
    val reps = rows.mapNotNull { it.reps.toIntOrNull() }
    val previous = sessions.lastOrNull { it.id != context.id && reps.size == rows.size && it.matches(reps) }
    val close = {
        if (!saving) {
            if (rows != initial) confirmsDiscard = true else onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = close,
        properties = DialogProperties(dismissOnBackPress = !saving, dismissOnClickOutside = !saving),
        title = { Text(if (context.session == null) "Registra pesi per serie" else "Modifica allenamento") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(context.name, style = MaterialTheme.typography.titleMedium)
                Text("Ripetizioni e kg per ogni serie.", style = MaterialTheme.typography.bodySmall)
                if (context.session == null) {
                    if (previous != null) {
                        TextButton(onClick = {
                            rows = rows.mapIndexed { index, row -> row.copy(weight = previous.sets[index].weight.toString()) }
                            error = null
                        }, enabled = !saving) { Text("Usa gli ultimi pesi") }
                        Text("Ultimi carichi: ${sessionDate(previous.timestamp)}", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("Nessun allenamento precedente con queste ripetizioni. I vecchi pesi singoli restano nello storico.")
                    }
                }
                rows.forEachIndexed { index, row ->
                    Text("Serie ${index + 1}", style = MaterialTheme.typography.titleSmall)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = row.reps, onValueChange = { value ->
                        rows = rows.mapIndexed { i, item -> if (i == index) item.copy(reps = value) else item }
                    }, label = { Text("Rip.") }, singleLine = true,
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Ripetizioni serie ${index + 1}" },
                        enabled = !saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(value = row.weight, onValueChange = { value ->
                        rows = rows.mapIndexed { i, item -> if (i == index) item.copy(weight = value) else item }
                    }, label = { Text("Peso (kg)") }, singleLine = true,
                        modifier = Modifier.weight(1f).semantics { contentDescription = "Peso (kg) serie ${index + 1}" },
                        enabled = !saving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    if (rows.size > 1) {
                        IconButton(onClick = { rows = rows.filterIndexed { i, _ -> i != index } }, enabled = !saving) {
                            Icon(Icons.Filled.Close, contentDescription = "Rimuovi serie ${index + 1}")
                        }
                    }
                    }
                    HorizontalDivider()
                }
                if (rows.size < 20) {
                    TextButton(onClick = { rows = rows + WeightSetDraft() }, enabled = !saving) { Text("Aggiungi serie") }
                }
                Text("Pesi maggiori di zero; usa la virgola o il punto. Ripetizioni da 1 a 1000.", style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (saving) { CircularProgressIndicator(); Text("Salvataggio allenamento…") }
            }
        },
        confirmButton = {
            TextButton(enabled = !saving, onClick = {
                if (!saving) {
                    val sets = rows.mapNotNull { it.value() }
                    if (sets.size != rows.size) {
                        error = "Controlla ripetizioni e pesi di tutte le serie."
                    } else {
                        error = null
                        saving = true
                        onSave(WorkoutWeightSession(context.id, context.timestamp, sets), {
                            saving = false
                            onDismiss()
                        }, { saving = false; error = it })
                    }
                }
            }) { Text("Salva allenamento") }
        },
        dismissButton = { TextButton(onClick = close, enabled = !saving) { Text("Annulla") } }
    )
    if (confirmsDiscard) {
        AlertDialog(onDismissRequest = { confirmsDiscard = false }, title = { Text("Scartare i pesi inseriti?") },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Scarta modifiche") } },
            dismissButton = { TextButton(onClick = { confirmsDiscard = false }) { Text("Continua a modificare") } })
    }
}

internal fun sessionDate(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.ITALIAN).format(Date(timestamp))
private fun sessionWeight(weight: Double) = java.text.NumberFormat.getNumberInstance(Locale.ITALIAN).apply {
    maximumFractionDigits = 2
}.format(weight)

@Composable
internal fun WeightSessionHistoryCard(
    session: WorkoutWeightSession, previous: WorkoutWeightSession?,
    onEdit: () -> Unit,
    onDelete: (() -> Unit, (String) -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var confirmsDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(sessionDate(session.timestamp), style = MaterialTheme.typography.titleSmall)
            Text(session.sets.joinToString(" · ") { "${it.reps} × ${sessionWeight(it.weight)} kg" })
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Chiudi dettagli" else "Dettagli serie") }
            if (expanded) {
                session.sets.forEachIndexed { index, set ->
                    Text("Serie ${index + 1}: ${set.reps} rip. × ${sessionWeight(set.weight)} kg")
                    previous?.let {
                        Text("Precedente: ${sessionWeight(it.sets[index].weight)} kg · ${sessionDate(it.timestamp)}",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
                TextButton(onClick = onEdit, enabled = !deleting) { Text("Modifica allenamento") }
                TextButton(onClick = { confirmsDelete = true }, enabled = !deleting) { Text("Elimina allenamento") }
                if (deleting) CircularProgressIndicator()
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
    if (confirmsDelete) {
        AlertDialog(onDismissRequest = { confirmsDelete = false },
            title = { Text("Eliminare tutte le serie di questo allenamento?") },
            confirmButton = { TextButton(onClick = {
                confirmsDelete = false
                if (!deleting) {
                    deleting = true
                    error = null
                    onDelete({ deleting = false }, { deleting = false; error = it })
                }
            }) { Text("Elimina allenamento") } },
            dismissButton = { TextButton(onClick = { confirmsDelete = false }) { Text("Annulla") } })
    }
}
