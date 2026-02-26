package com.matthew.sportiliapp.scheda

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val scheda by viewModel.scheda.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(true)
    val esercizio = scheda?.giorni?.get(giornoId)
        ?.gruppiMuscolari?.get(gruppoMuscolareId)?.esercizi?.get(esercizioId)

    val userExerciseData by viewModel.userExerciseData.observeAsState(initial = emptyMap())

    val exerciseParts = remember(esercizio) {
        esercizio?.name
            ?.split("+")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }
            ?: esercizio?.name?.takeIf { it.isNotEmpty() }?.let { listOf(it) }
            ?: emptyList()
    }

    var selectedPartIndex by remember { mutableStateOf(0) }
    LaunchedEffect(exerciseParts) {
        if (selectedPartIndex >= exerciseParts.size) selectedPartIndex = 0
    }

    val currentPartName = exerciseParts.getOrElse(selectedPartIndex) { esercizio?.name ?: "" }
    val exerciseKey = if (currentPartName.isBlank()) {
        ""
    } else {
        viewModel.exerciseKeyFromName(currentPartName)
    }
    val currentData = userExerciseData[exerciseKey]

    val canManageData = esercizio != null && exerciseKey.isNotEmpty()
    val topBarTitle = when {
        esercizio != null -> esercizio.name
        isLoading -> "Caricamento esercizio"
        else -> "Esercizio non disponibile"
    }

    // Logs + Note (synced with current key)
    var weightLogs by remember { mutableStateOf<List<WeightLogRecord>>(emptyList()) }
    var noteInput by remember { mutableStateOf("") }
    var dialogExerciseKey by remember { mutableStateOf(exerciseKey) }

    // UI states
    var weightDialogMode by remember { mutableStateOf<WeightDialogMode>(WeightDialogMode.Hidden) }
    var weightInput by remember { mutableStateOf("") }
    var showTimerSheet by remember { mutableStateOf(false) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var isImageFullScreen by remember { mutableStateOf(false) }
    var pendingDeletionRecord by remember { mutableStateOf<WeightLogRecord?>(null) }
    var alertMessage by remember { mutableStateOf<String?>(null) }

    val sheetDateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val summaryDateFormatter = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }

    LaunchedEffect(exerciseKey) {
        // reset transients when switching exercise variation
        pendingDeletionRecord = null
        weightDialogMode = WeightDialogMode.Hidden
        weightInput = ""
        dialogExerciseKey = exerciseKey
    }

    LaunchedEffect(exerciseKey, currentData?.weightLogs) {
        val logs = currentData?.weightLogs?.mapNotNull { (id, entry) ->
            val w = entry.weight
            val ts = entry.timestamp
            if (w != null && ts != null) WeightLogRecord(id, w, ts) else null
        }?.sortedBy { it.timestamp } ?: emptyList()
        weightLogs = logs
    }

    LaunchedEffect(exerciseKey, currentData?.noteUtente) {
        noteInput = currentData?.noteUtente ?: ""
    }

    val sortedLogs = remember(weightLogs) { weightLogs.sortedBy { it.timestamp } }
    val recentLogs = remember(sortedLogs) { sortedLogs.takeLast(10) }
    val savedNote = currentData?.noteUtente ?: ""
    val isNoteDirty = noteInput != savedNote

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    val riposo = esercizio?.riposo.orEmpty()
                    if (riposo.isNotBlank()) {
                        IconButton(onClick = { showTimerSheet = true }) {
                            Icon(Icons.Filled.Notifications, contentDescription = "Avvia timer recupero")
                        }
                    }
                    if (esercizio != null) {
                        IconButton(
                            onClick = {
                                dialogExerciseKey = exerciseKey
                                weightDialogMode = WeightDialogMode.Create
                                weightInput = ""
                            },
                            enabled = canManageData
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Registra peso")
                        }
                    }
                }
            )
        }
    ) { padding ->

        if (esercizio != null) {
            val ex = esercizio

            val heroSubtitle = if (exerciseParts.size > 1) currentPartName else null
            val imageUrl =
                "https://firebasestorage.googleapis.com/v0/b/sportiliapp.appspot.com/o/${currentPartName}.png?alt=media&token=cd00fa34-6a1f-4fa7-afa5-d80a1ef5cdaa"

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ExerciseTitleBlock(
                        title = ex.name,
                        subtitle = heroSubtitle
                    )
                }

                item {
                    ExerciseHeroHeader(
                        imageUrl = imageUrl,
                        title = ex.name,
                        subtitle = heroSubtitle,
                        onTap = { isImageFullScreen = true }
                    )
                }

                if (exerciseParts.size > 1) {
                    item {
                        SectionHeader(title = "Variazioni")
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                ExerciseVariationPicker(
                                    parts = exerciseParts,
                                    selectedIndex = selectedPartIndex,
                                    onSelected = { selectedPartIndex = it }
                                )
                            }
                        }
                    }
                }

                item {
                    SectionHeader(title = "Programma")
                    ElevatedCard(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ExerciseSerieRow(serie = ex.serie)

                            ex.riposo?.takeIf { it.isNotBlank() }?.let { rip ->
                                Divider(modifier = Modifier.padding(vertical = 12.dp))
                                LabeledRow(
                                    label = "Recupero",
                                    icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                                    value = rip
                                )
                            }

                            ex.notePT?.takeIf { it.isNotBlank() }?.let { note ->
                                Divider(modifier = Modifier.padding(vertical = 12.dp))
                                CoachNotesRow(text = note)
                            }
                        }
                    }
                }

                item {
                    SectionHeader(title = "Note personali")
                    ElevatedCard(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (savedNote.isBlank()) {
                                Text(
                                    text = "Nessuna nota salvata.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = savedNote,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            NotesPreviewRow(
                                isDirty = isNoteDirty,
                                enabled = canManageData,
                                onTap = { showNotesSheet = true }
                            )
                        }
                    }
                }

                item {
                    SectionHeader(title = "Progressi")
                }

                item {
                    if (recentLogs.isEmpty()) {
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            EmptyStateCard(
                                title = "Nessun peso registrato",
                                message = "Registra il tuo primo peso cliccando sul + in alto per visualizzare i progressi."
                            )
                        }
                    } else {
                        val chartEntries = remember(recentLogs) {
                            recentLogs.map { WeightLogEntry(weight = it.weight, timestamp = it.timestamp) }
                        }
                        ElevatedCard(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange, // “proxy” icon (no chart icon in base set)
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Andamento (ultimi 10)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                WeightProgressChart(entries = chartEntries)
                            }
                        }
                    }
                }

                if (sortedLogs.isNotEmpty()) {
                    items(sortedLogs.asReversed(), key = { it.id }) { record ->
                        WeightLogSwipeCard(
                            record = record,
                            dateText = summaryDateFormatter.format(Date(record.timestamp)),
                            enabled = canManageData,
                            onEdit = {
                                dialogExerciseKey = exerciseKey
                                weightDialogMode = WeightDialogMode.Edit(record)
                                weightInput = editingString(record.weight)
                            },
                            onDelete = { pendingDeletionRecord = record }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            if (isImageFullScreen) {
                FullScreenImageDialog(
                    imageUrl = imageUrl,
                    onClose = { isImageFullScreen = false }
                )
            }

            if (showTimerSheet) {
                val timerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = { showTimerSheet = false },
                    sheetState = timerSheetState
                ) {
                    // TimerSheet è già presente nel progetto
                    TimerSheet(riposo = ex.riposo ?: "")
                }
            }

            if (showNotesSheet) {
                val notesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                ModalBottomSheet(
                    onDismissRequest = {
                        // comportamento iOS: chiudendo, ripristina se non salvato
                        noteInput = savedNote
                        showNotesSheet = false
                    },
                    sheetState = notesSheetState
                ) {
                    NotesEditorSheet(
                        title = "Note personali",
                        text = noteInput,
                        savedText = savedNote,
                        canManage = canManageData,
                        isDirty = isNoteDirty,
                        onTextChange = { noteInput = it },
                        onClose = {
                            noteInput = savedNote
                            showNotesSheet = false
                        },
                        onSave = {
                            val sanitized = noteInput.trim()
                            viewModel.updateUserNote(
                                exerciseKey,
                                sanitized.takeIf { it.isNotEmpty() },
                                onSuccess = {
                                    noteInput = sanitized
                                    Toast.makeText(context, "Nota salvata", Toast.LENGTH_SHORT).show()
                                    showNotesSheet = false
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
                        onDelete = {
                            viewModel.updateUserNote(
                                exerciseKey,
                                null,
                                onSuccess = {
                                    noteInput = ""
                                    Toast.makeText(context, "Nota rimossa", Toast.LENGTH_SHORT).show()
                                    showNotesSheet = false
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
                    )
                }
            }

            if (weightDialogMode != WeightDialogMode.Hidden) {
                val weightSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                val sheetTitle = when (weightDialogMode) {
                    WeightDialogMode.Create -> "Registra peso"
                    is WeightDialogMode.Edit -> "Modifica peso"
                    WeightDialogMode.Hidden -> ""
                }

                val dateLabel = when (weightDialogMode) {
                    is WeightDialogMode.Edit -> "Ultimo aggiornamento"
                    WeightDialogMode.Create -> "Data"
                    WeightDialogMode.Hidden -> "Data"
                }

                val dateValue = when (val mode = weightDialogMode) {
                    is WeightDialogMode.Edit -> sheetDateFormatter.format(Date(mode.record.timestamp))
                    WeightDialogMode.Create -> sheetDateFormatter.format(Date())
                    WeightDialogMode.Hidden -> sheetDateFormatter.format(Date())
                }

                ModalBottomSheet(
                    onDismissRequest = {
                        weightDialogMode = WeightDialogMode.Hidden
                        weightInput = ""
                    },
                    sheetState = weightSheetState
                ) {
                    WeightEntrySheet(
                        title = sheetTitle,
                        weightInput = weightInput,
                        canManage = canManageData,
                        onWeightChange = { weightInput = it.replace(',', '.') },
                        dateLabel = dateLabel,
                        dateValue = dateValue,
                        onCancel = {
                            weightDialogMode = WeightDialogMode.Hidden
                            weightInput = ""
                        },
                        onConfirm = {
                            val parsed = weightInput.toDoubleOrNull()
                            if (parsed == null || parsed <= 0) {
                                Toast.makeText(context, "Inserisci un peso valido", Toast.LENGTH_SHORT).show()
                                return@WeightEntrySheet
                            }
                            if (dialogExerciseKey.isEmpty()) {
                                Toast.makeText(context, "Impossibile identificare l'esercizio", Toast.LENGTH_SHORT).show()
                                return@WeightEntrySheet
                            }

                            when (val mode = weightDialogMode) {
                                WeightDialogMode.Create -> {
                                    viewModel.addWeightEntry(
                                        dialogExerciseKey,
                                        parsed,
                                        onSuccess = { _, entry ->
                                            val w = entry.weight
                                            val ts = entry.timestamp
                                            if (w != null && ts != null) {
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
                                        dialogExerciseKey,
                                        record.id,
                                        parsed,
                                        onSuccess = { entry ->
                                            val w = entry.weight
                                            val ts = entry.timestamp
                                            if (w != null && ts != null) {
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
                    )
                }
            }

            pendingDeletionRecord?.let { record ->
                AlertDialog(
                    onDismissRequest = { pendingDeletionRecord = null },
                    title = { Text("Elimina peso") },
                    text = {
                        Text(
                            "Vuoi eliminare il peso registrato il ${
                                summaryDateFormatter.format(Date(record.timestamp))
                            }?"
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (exerciseKey.isEmpty()) {
                                    Toast.makeText(context, "Impossibile identificare l'esercizio", Toast.LENGTH_SHORT).show()
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
                            }
                        ) { Text("Elimina") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingDeletionRecord = null }) {
                            Text("Annulla")
                        }
                    }
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Caricamento esercizio...")
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = "Questo esercizio non è disponibile.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        OutlinedButton(onClick = { navController.popBackStack() }) {
                            Text("Torna indietro")
                        }
                    }
                }
            }
        }
    }

    alertMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            title = { Text("Connessione assente") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { alertMessage = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ExerciseTitleBlock(title: String, subtitle: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExerciseHeroHeader(
    imageUrl: String,
    title: String,
    subtitle: String?,
    onTap: () -> Unit
) {
    val painter = rememberAsyncImagePainter(model = imageUrl)
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(shape)
            .clickable {
                if (painter.state is AsyncImagePainter.State.Success) onTap()
            }
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.05f),
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )
        when (painter.state) {
            is AsyncImagePainter.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Immagine non disponibile",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {}
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExerciseVariationPicker(
    parts: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        parts.forEachIndexed { index, label ->
            SegmentedButton(
                selected = index == selectedIndex,
                onClick = { onSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = parts.size),
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
private fun ExerciseSerieRow(serie: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Serie",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = serie,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LabeledRow(
    label: String,
    icon: @Composable (() -> Unit)? = null,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    icon()
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CoachNotesRow(text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "Note del coach",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EmptyStateCard(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun NotesPreviewRow(
    isDirty: Boolean,
    enabled: Boolean,
    onTap: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onTap),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Apri editor note",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isDirty) {
                        Text(
                            text = "  • modifiche",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Filled.ArrowBack, // chevron-like workaround without extra icons
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.Transparent)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightLogSwipeCard(
    record: WeightLogRecord,
    dateText: String,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { false }, // no full swipe
        positionalThreshold = { it * 0.35f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = enabled,
        backgroundContent = {
            val bg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(bg),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        IconButton(onClick = onEdit, enabled = enabled) {
                            Icon(Icons.Filled.Edit, contentDescription = "Modifica")
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ) {
                        IconButton(onClick = onDelete, enabled = enabled) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Elimina",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${formatWeight(record.weight)} kg",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // spazio “clean” (come iOS)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesEditorSheet(
    title: String,
    text: String,
    savedText: String,
    canManage: Boolean,
    isDirty: Boolean,
    onTextChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                TextButton(onClick = onClose) { Text("Chiudi") }
            },
            actions = {
                TextButton(
                    onClick = onSave,
                    enabled = canManage && isDirty
                ) { Text("Salva") }
            }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Aggiungi una nota per questo esercizio…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                enabled = canManage,
                minLines = 8
            )

            if (savedText.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onDelete,
                    enabled = canManage,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Elimina nota", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeightEntrySheet(
    title: String,
    weightInput: String,
    canManage: Boolean,
    onWeightChange: (String) -> Unit,
    dateLabel: String,
    dateValue: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = {
                TextButton(onClick = onCancel) { Text("Annulla") }
            },
            actions = {
                TextButton(onClick = onConfirm, enabled = canManage) { Text("Salva") }
            }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Peso",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = weightInput,
                onValueChange = onWeightChange,
                label = { Text("Peso (kg)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = canManage,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Dettagli",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = dateValue,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
fun FullScreenImageDialog(imageUrl: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black)
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp)
                    .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.16f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Chiudi",
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(18.dp))
                )
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            val width = size.width
            val height = size.height
            val horizontalPadding = 48f
            val verticalPadding = 28f
            val usableWidth = width - horizontalPadding * 2
            val usableHeight = height - verticalPadding * 2
            val stepX = if (plottedEntries.size > 1) usableWidth / (plottedEntries.size - 1) else 0f

            // grid
            val gridCount = 4
            repeat(gridCount + 1) { i ->
                val y = verticalPadding + (usableHeight / gridCount) * i
                drawLine(
                    color = colorOutline.copy(alpha = 0.22f),
                    start = Offset(horizontalPadding, y),
                    end = Offset(width - horizontalPadding, y),
                    strokeWidth = 1f
                )
            }

            // line path
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
                style = Stroke(width = 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // points
            plottedEntries.forEachIndexed { index, entry ->
                val weight = entry.weight!!
                val normalized = ((weight - minWeight) / weightRange).toFloat()
                val x = horizontalPadding + stepX * index
                val y = height - (verticalPadding + usableHeight * normalized)
                drawCircle(color = colorPrimary, radius = 5.5f, center = Offset(x, y))
            }

            // x labels (every label if <= 6 else every 2)
            val step = if (plottedEntries.size <= 6) 1 else 2
            plottedEntries.forEachIndexed { index, entry ->
                if (index % step != 0) return@forEachIndexed
                val x = horizontalPadding + stepX * index
                val y = height - verticalPadding + 26f
                val dateLabel = entry.timestamp?.let { dateFormatter.format(Date(it)) } ?: "-"

                drawContext.canvas.nativeCanvas.drawText(
                    dateLabel,
                    x - 34,
                    y,
                    android.graphics.Paint().apply {
                        color = colorText.copy(alpha = 0.9f).toArgb()
                        textSize = 24f
                        textAlign = android.graphics.Paint.Align.LEFT
                        isAntiAlias = true
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
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
    return if (weight % 1.0 == 0.0) weight.toInt().toString()
    else String.format(Locale.getDefault(), "%.1f", weight)
}

private fun editingString(weight: Double): String {
    return if (weight % 1.0 == 0.0) {
        String.format(Locale.getDefault(), "%.0f", weight)
    } else {
        String.format(Locale.getDefault(), "%.2f", weight)
    }
}
