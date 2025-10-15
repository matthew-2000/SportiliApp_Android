package com.matthew.sportiliapp.scheda

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

    var weightLogs by remember { mutableStateOf<List<WeightLogEntry>>(emptyList()) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }
    var isImageFullScreen by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(esercizio?.weightLogs) {
        val logs = esercizio?.weightLogs?.values?.mapNotNull { entry ->
            val weight = entry.weight
            val timestamp = entry.timestamp
            if (weight != null && timestamp != null) WeightLogEntry(weight, timestamp) else null
        }?.sortedBy { it.timestamp } ?: emptyList()
        weightLogs = logs
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
                Text(
                    text = "Note PT",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = esercizio.notePT?.takeIf { it.isNotEmpty() } ?: "Nessuna nota",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ——— MONITORAGGIO PESO ———
                Text(
                    text = "Andamento peso",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                val recentLogs = remember(weightLogs) {
                    weightLogs.sortedBy { it.timestamp ?: 0L }.takeLast(10)
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
                    WeightProgressChart(entries = recentLogs)
                    Spacer(modifier = Modifier.height(8.dp))
                    recentLogs.asReversed().take(3).forEach { entry ->
                        val dateLabel = entry.timestamp?.let { dateFormatter.format(Date(it)) } ?: "-"
                        val weightLabel = entry.weight?.let { formatWeight(it) } ?: "-"
                        Text(
                            text = "$dateLabel • $weightLabel kg",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ——— AZIONI ———
                Button(
                    onClick = { showWeightDialog = true },
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

                Spacer(modifier = Modifier.height(24.dp))

                // ——— DIALOG PESO ———
                if (showWeightDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showWeightDialog = false
                            weightInput = ""
                        },
                        title = { Text("Registra peso") },
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
                                if (parsed == null) {
                                    Toast.makeText(context, "Inserisci un peso valido", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addWeightEntry(
                                        giornoId, gruppoMuscolareId, esercizioId, parsed,
                                        onSuccess = { entry ->
                                            weightLogs = (weightLogs + entry).sortedBy { it.timestamp ?: 0L }
                                            Toast.makeText(context, "Peso salvato", Toast.LENGTH_SHORT).show()
                                            showWeightDialog = false
                                            weightInput = ""
                                        },
                                        onFailure = { err ->
                                            Toast.makeText(context, "Errore: $err", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }) { Text("Salva") }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = {
                                showWeightDialog = false
                                weightInput = ""
                            }) { Text("Annulla") }
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
