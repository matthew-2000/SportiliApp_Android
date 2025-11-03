package com.matthew.sportiliapp.scheda

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.matthew.sportiliapp.model.SchedaViewModel
import com.matthew.sportiliapp.model.SchedaViewModelFactory
import com.matthew.sportiliapp.model.WeightLogEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class WeightLogRecord(
    val id: String,
    val weight: Double,
    val timestamp: Long
)

private sealed class WeightDialogMode {
    object Hidden : WeightDialogMode()
    object Create : WeightDialogMode()
    data class Edit(val record: WeightLogRecord) : WeightDialogMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsercizioScreen(
    navController: NavHostController,
    giornoId: String,
    gruppoMuscolareId: String,
    esercizioId: String,
) {
    val context = LocalContext.current
    val viewModel: SchedaViewModel = viewModel(factory = SchedaViewModelFactory(context))
    val esercizio = viewModel.scheda.value?.giorni?.get(giornoId)
        ?.gruppiMuscolari?.get(gruppoMuscolareId)?.esercizi?.get(esercizioId)

    val userExerciseData by viewModel.userExerciseData.observeAsState(initial = emptyMap())

    val exerciseParts = remember(esercizio) {
        esercizio?.name
            ?.split(" + ")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?: esercizio?.name?.takeIf { it.isNotEmpty() }?.let { listOf(it) }
            ?: emptyList()
    }

    val serieParts = remember(esercizio) {
        esercizio?.serie
            ?.split(" + ")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
    }

    var selectedPartIndex by remember { mutableStateOf(0) }
    LaunchedEffect(exerciseParts) {
        if (selectedPartIndex >= exerciseParts.size) {
            selectedPartIndex = 0
        }
    }

    val currentPartName = exerciseParts.getOrElse(selectedPartIndex) { esercizio?.name ?: "" }
    val exerciseKey = viewModel.exerciseKeyFromName(currentPartName)
    val currentData = userExerciseData[exerciseKey]
    val selectedSerie = serieParts.getOrNull(selectedPartIndex)

    var weightLogs by remember { mutableStateOf<List<WeightLogRecord>>(emptyList()) }
    var weightDialogMode by remember { mutableStateOf<WeightDialogMode>(WeightDialogMode.Hidden) }
    var weightInput by remember { mutableStateOf("") }
    var isImageFullScreen by remember { mutableStateOf(false) }
    var pendingDeletionRecord by remember { mutableStateOf<WeightLogRecord?>(null) }
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var noteInput by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(exerciseKey) {
        pendingDeletionRecord = null
        weightDialogMode = WeightDialogMode.Hidden
        weightInput = ""
    }

    LaunchedEffect(exerciseKey, currentData?.weightLogs) {
        val logs = currentData?.weightLogs?.mapNotNull { (id, entry) ->
            val weight = entry.weight
            val timestamp = entry.timestamp
            if (weight != null && timestamp != null) {
                WeightLogRecord(id, weight, timestamp)
            } else {
                null
            }
        }?.sortedBy { it.timestamp } ?: emptyList()
        weightLogs = logs
    }

    LaunchedEffect(exerciseKey, currentData?.noteUtente) {
        noteInput = currentData?.noteUtente ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->

        esercizio?.let { esercizio ->

            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = esercizio.name, style = MaterialTheme.typography.titleLarge)

                if (exerciseParts.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Seleziona esercizio",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(exerciseParts) { index, partName ->
                            FilterChip(
                                selected = index == selectedPartIndex,
                                onClick = { selectedPartIndex = index },
                                label = { Text(partName) }
                            )
                        }
                    }
                }
                // ——— IMMAGINE COMPATTA ———
                val painter = rememberAsyncImagePainter(
                    model = "https://firebasestorage.googleapis.com/v0/b/sportiliapp.appspot.com/o/${esercizio.name}.png?alt=media&token=cd00fa34-6a1f-4fa7-afa5-d80a1ef5cdaa"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isImageFullScreen = true }
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (painter.state is AsyncImagePainter.State.Error) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
                            Text(
                                "Immagine non disponibile",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ——— SERIE & RIPOSO ———
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = esercizio.serie,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    esercizio.riposo?.takeIf { it.isNotEmpty() }?.let {
                        Text(
                            text = "$it recupero",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ——— NOTE PT ———
                esercizio.notePT?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(
                        text = "Note PT",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ——— MONITORAGGIO PESO ———
                Text(
                    text = "Andamento peso",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                val recentLogs = remember(weightLogs) {
                    weightLogs.sortedBy { it.timestamp }.takeLast(10)
                }

                if (recentLogs.isEmpty()) {
                    Text(
                        text = "Nessun peso registrato",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    val chartEntries = remember(recentLogs) {
                        recentLogs.map { WeightLogEntry(weight = it.weight, timestamp = it.timestamp) }
                    }
                    WeightProgressChart(entries = chartEntries)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                val latestRecord = remember(weightLogs) { weightLogs.maxByOrNull { it.timestamp } }

                latestRecord?.let { record ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Ultimo peso registrato",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${dateFormatter.format(Date(record.timestamp))} • ${formatWeight(record.weight)} kg",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        weightDialogMode = WeightDialogMode.Edit(record)
                                        weightInput = record.weight.toString()
                                    },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("Modifica")
                                }
                                TextButton(
                                    onClick = { pendingDeletionRecord = record },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Elimina",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ——— AZIONI ———
                Button(
                    onClick = {
                        weightDialogMode = WeightDialogMode.Create
                        weightInput = ""
                    },
                    enabled = exerciseKey.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Registra peso")
                }

                OutlinedButton(
                    onClick = { scope.launch { sheetState.show() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Avvia timer di recupero")
                }

                Spacer(modifier = Modifier.height(20.dp))

                val savedNote = currentData?.noteUtente ?: ""
                val isNoteDirty = noteInput != savedNote

                Text(
                    text = "Note",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                )
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    placeholder = { Text("Aggiungi una nota") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    enabled = exerciseKey.isNotEmpty(),
                    minLines = 1
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            val sanitized = noteInput.trim()
                            viewModel.updateUserNote(
                                exerciseKey,
                                sanitized.takeIf { it.isNotEmpty() },
                                onSuccess = {
                                    noteInput = sanitized
                                    Toast.makeText(context, "Nota salvata", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    if (err.contains("Connessione internet assente")) {
                                        alertMessage = err
                                    } else {
                                        Toast.makeText(context, "Errore: $err", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        enabled = exerciseKey.isNotEmpty() && isNoteDirty,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text("Salva nota")
                    }

                    OutlinedButton(
                        onClick = { noteInput = savedNote },
                        enabled = isNoteDirty,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(5.dp)
                    ) {
                        Text("Annulla")
                    }
                }

                if (savedNote.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            viewModel.updateUserNote(
                                exerciseKey,
                                null,
                                onSuccess = {
                                    noteInput = ""
                                    Toast.makeText(context, "Nota rimossa", Toast.LENGTH_SHORT).show()
                                },
                                onFailure = { err ->
                                    if (err.contains("Connessione internet assente")) {
                                        alertMessage = err
                                    } else {
                                        Toast.makeText(context, "Errore: $err", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        },
                        enabled = exerciseKey.isNotEmpty(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Rimuovi nota", color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ——— DIALOG PESO ———
                if (weightDialogMode != WeightDialogMode.Hidden) {
                    val isEditing = weightDialogMode is WeightDialogMode.Edit
                    AlertDialog(
                        onDismissRequest = {
                            weightDialogMode = WeightDialogMode.Hidden
                            weightInput = ""
                        },
                        title = { Text(if (isEditing) "Modifica peso" else "Registra peso") },
                        text = {
                            Column {
                                OutlinedTextField(
                                    value = weightInput,
                                    onValueChange = { weightInput = it.replace(',', '.') },
                                    label = { Text("Peso (kg)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Data: ${dateFormatter.format(Date())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                val parsed = weightInput.toDoubleOrNull()
                                if (parsed == null || parsed <= 0) {
                                    Toast.makeText(context, "Inserisci un peso valido", Toast.LENGTH_SHORT).show()
                                } else if (exerciseKey.isEmpty()) {
                                    Toast.makeText(context, "Impossibile identificare l'esercizio", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    when (val mode = weightDialogMode) {
                                        WeightDialogMode.Create -> {
                                            viewModel.addWeightEntry(
                                                exerciseKey,
                                                parsed,
                                                onSuccess = { id, entry ->
                                                    val weight = entry.weight
                                                    val timestamp = entry.timestamp
                                                    if (weight != null && timestamp != null) {
                                                        Toast.makeText(context, "Peso salvato", Toast.LENGTH_SHORT).show()
                                                        weightDialogMode = WeightDialogMode.Hidden
                                                        weightInput = ""
                                                    } else {
                                                        Toast.makeText(context, "Errore nel salvataggio del peso", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onFailure = { err ->
                                                    if (err.contains("Connessione internet assente")) {
                                                        alertMessage = err
                                                    } else {
                                                        Toast.makeText(context, "Errore: $err", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }

                                        is WeightDialogMode.Edit -> {
                                            val record = mode.record
                                            viewModel.updateWeightEntry(
                                                exerciseKey,
                                                record.id,
                                                parsed,
                                                onSuccess = { entry ->
                                                    val weight = entry.weight
                                                    val timestamp = entry.timestamp
                                                    if (weight != null && timestamp != null) {
                                                        Toast.makeText(context, "Peso aggiornato", Toast.LENGTH_SHORT).show()
                                                        weightDialogMode = WeightDialogMode.Hidden
                                                        weightInput = ""
                                                    } else {
                                                        Toast.makeText(context, "Errore nell'aggiornamento del peso", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                onFailure = { err ->
                                                    if (err.contains("Connessione internet assente")) {
                                                        alertMessage = err
                                                    } else {
                                                        Toast.makeText(context, "Errore: $err", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
                                        }

                                        WeightDialogMode.Hidden -> Unit
                                    }
                                }
                            }) { Text(if (isEditing) "Salva modifiche" else "Salva") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = {
                                weightDialogMode = WeightDialogMode.Hidden
                                weightInput = ""
                            }) { Text("Annulla") }
                        }
                    )
                }

                pendingDeletionRecord?.let { record ->
                    AlertDialog(
                        onDismissRequest = { pendingDeletionRecord = null },
                        title = { Text("Elimina peso") },
                        text = { Text("Vuoi eliminare l'ultimo peso registrato?") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (exerciseKey.isEmpty()) {
                                        Toast.makeText(context, "Impossibile identificare l'esercizio", Toast.LENGTH_SHORT)
                                            .show()
                                    } else {
                                        viewModel.deleteWeightEntry(
                                            exerciseKey,
                                            record.id,
                                            onSuccess = {
                                                Toast.makeText(context, "Peso eliminato", Toast.LENGTH_SHORT).show()
                                                pendingDeletionRecord = null
                                            },
                                            onFailure = { err ->
                                                if (err.contains("Connessione internet assente")) {
                                                    alertMessage = err
                                                } else {
                                                    Toast.makeText(context, "Errore: $err", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Elimina")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { pendingDeletionRecord = null }) {
                                Text("Annulla")
                            }
                        }
                    )
                }

                if (sheetState.isVisible) {
                    ModalBottomSheet(
                        onDismissRequest = { scope.launch { sheetState.hide() } },
                        sheetState = sheetState
                    ) {
                        TimerSheet(riposo = esercizio.riposo ?: "")
                    }
                }
            }

            if (isImageFullScreen) {
                FullScreenImageDialog(
                    imageUrl = "https://firebasestorage.googleapis.com/v0/b/sportiliapp.appspot.com/o/${esercizio.name}.png?alt=media&token=cd00fa34-6a1f-4fa7-afa5-d80a1ef5cdaa",
                    onClose = { isImageFullScreen = false }
                )
            }
        }
    }

    alertMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            title = { Text("Connessione assente") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { alertMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun FullScreenImageDialog(imageUrl: String, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Pulsante di chiusura
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopEnd // Pulsante in alto a destra
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface, shape = CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Chiudi immagine",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Immagine centrata con bordi arrotondati
                Box(
                    modifier = Modifier
                        .aspectRatio(1f) // Manteniamo un rapporto di 3:2
                        .clip(RoundedCornerShape(16.dp)) // Bordi arrotondati
                        .background(MaterialTheme.colorScheme.surface) // Sfondo per immagine
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = imageUrl),
                        contentDescription = null,
                        contentScale = ContentScale.Fit, // L'immagine si adatta mantenendo le proporzioni
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun WeightProgressChart(entries: List<WeightLogEntry>) {
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorOutline = MaterialTheme.colorScheme.outline
    val colorText = MaterialTheme.colorScheme.onSurfaceVariant

    val plottedEntries = remember(entries) {
        entries.filter { it.weight != null && it.timestamp != null }
            .sortedBy { it.timestamp }
    }

    if (plottedEntries.isEmpty()) {
        Text(
            text = "Nessun peso registrato.",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        return
    }

    val weights = plottedEntries.map { it.weight!! }
    val minWeight = weights.minOrNull() ?: 0.0
    val maxWeight = weights.maxOrNull() ?: 0.0
    val weightRange = (maxWeight - minWeight).takeIf { it > 0.0 } ?: 1.0
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Grafico
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val width = size.width
            val height = size.height
            val horizontalPadding = 64f
            val verticalPadding = 32f
            val usableWidth = width - horizontalPadding * 2
            val usableHeight = height - verticalPadding * 2
            val stepX = if (plottedEntries.size > 1) usableWidth / (plottedEntries.size - 1) else 0f

            // Griglia orizzontale leggera
            val gridCount = 4
            repeat(gridCount + 1) { i ->
                val y = verticalPadding + (usableHeight / gridCount) * i
                drawLine(
                    color = colorOutline.copy(alpha = 0.3f),
                    start = Offset(horizontalPadding, y),
                    end = Offset(width - horizontalPadding, y),
                    strokeWidth = 1f
                )
            }

            // Linea asse Y
            drawLine(
                color = colorOutline.copy(alpha = 0.5f),
                start = Offset(horizontalPadding, verticalPadding),
                end = Offset(horizontalPadding, height - verticalPadding),
                strokeWidth = 2f
            )

            // Linea asse X
            drawLine(
                color = colorOutline.copy(alpha = 0.5f),
                start = Offset(horizontalPadding, height - verticalPadding),
                end = Offset(width - horizontalPadding, height - verticalPadding),
                strokeWidth = 2f
            )

            // Traccia linea peso
            val path = Path()
            plottedEntries.forEachIndexed { index, entry ->
                val weight = entry.weight!!
                val normalized = ((weight - minWeight) / weightRange).toFloat()
                val x = horizontalPadding + stepX * index
                val y = height - (verticalPadding + usableHeight * normalized)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }

            drawPath(
                path = path,
                color = colorPrimary,
                style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Punti e label del peso
            plottedEntries.forEachIndexed { index, entry ->
                val weight = entry.weight!!
                val normalized = ((weight - minWeight) / weightRange).toFloat()
                val x = horizontalPadding + stepX * index
                val y = height - (verticalPadding + usableHeight * normalized)

                drawCircle(
                    color = colorPrimary,
                    radius = 8f,
                    center = Offset(x, y)
                )

                // Peso sopra ogni punto
                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        "${formatWeight(weight)} kg",
                        x - 35,
                        y - 16,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.GRAY
                            textSize = 26f
                            textAlign = android.graphics.Paint.Align.LEFT
                            isAntiAlias = true
                        }
                    )
                }
            }

            // Etichette date sull’asse X
            plottedEntries.forEachIndexed { index, entry ->
                val x = horizontalPadding + stepX * index
                val y = height - verticalPadding + 28f
                val dateLabel = entry.timestamp?.let { dateFormatter.format(Date(it)) } ?: "-"

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        dateLabel,
                        x - 40,
                        y + 10,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 26f
                            textAlign = android.graphics.Paint.Align.LEFT
                            isAntiAlias = true
                        }
                    )
                }
            }
        }

        // Range peso (min/max)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Min: ${formatWeight(minWeight)} kg",
                style = MaterialTheme.typography.labelSmall,
                color = colorText
            )
            Text(
                text = "Max: ${formatWeight(maxWeight)} kg",
                style = MaterialTheme.typography.labelSmall,
                color = colorText
            )
        }
    }
}

private fun formatWeight(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        weight.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", weight)
    }
}
