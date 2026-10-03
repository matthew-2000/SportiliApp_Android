package com.matthew.sportiliapp.scheda
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.SchedaViewModel
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiornoScreen(navController: NavHostController, giornoId: String, viewModel: SchedaViewModel) {
    val scheda by viewModel.scheda.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(true)
    val giorno = scheda?.giorni?.get(giornoId)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(text = giorno?.name ?: "Giorno") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        content = { padding ->
            if (giorno != null) {
                LazyColumn(
                    modifier = Modifier.padding(padding),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(giorno.gruppiMuscolari.entries.toList()) { (gruppoId, gruppo) ->
                        GruppoSection(gruppo = gruppo, navController, gruppoId, giornoId)
                    }
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
                            Text("Caricamento esercizi...")
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Text(
                                text = "Questo giorno non è disponibile al momento.",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            OutlinedButton(onClick = { navController.popBackStack() }) {
                                Text("Torna alla scheda")
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun GruppoSection(gruppo: GruppoMuscolare, navController: NavHostController, gruppoId: String, giornoId: String) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = gruppo.nome,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column {
            gruppo.esercizi.forEach { (esercizioId, esercizio) ->
                EsercizioRow(esercizio = esercizio) {
                    // Passa solo gli ID nella route
                    navController.navigate("esercizio/$giornoId/$gruppoId/$esercizioId")
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
    }
}

@Composable
fun EsercizioRow(esercizio: Esercizio, onClick: () -> Unit) {
    val parts = exerciseNameParts(esercizio.name)
    val largeText = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .clickable(role = Role.Button, onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (parts.size > 1) {
                Text("Superset · ${parts.size} parti", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text("Esegui tutte le parti in combinazione.", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(esercizio.name, style = MaterialTheme.typography.titleMedium)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Serie e ripetizioni", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(esercizio.serie, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                esercizio.riposo?.takeIf { it.isNotBlank() }?.let {
                    Text("Recupero $it", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            parts.forEachIndexed { index, part ->
                if (index > 0) Text("+", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                if (parts.size > 1) {
                    if (largeText) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExercisePartLabel(part, index, parts.size)
                            ExerciseThumbnail(part)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ExerciseThumbnail(part)
                            Column(Modifier.weight(1f)) { ExercisePartLabel(part, index, parts.size) }
                        }
                    }
                } else ExerciseThumbnail(part)
            }
            Text("Apri esercizio", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Presentation only: the selected names still go through the existing key resolver. */
internal fun exerciseNameParts(name: String): List<String> = name.split("+")
    .map { it.trim() }.filter { it.isNotEmpty() }
    .ifEmpty { listOf(name.trim()).filter { it.isNotEmpty() } }

@Composable
private fun ExercisePartLabel(part: String, index: Int, count: Int) {
    Text("Parte ${index + 1} di $count", style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(part, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun ExerciseThumbnail(name: String) {
    val painter = rememberAsyncImagePainter(model = exerciseImageUrl(name))
    Box(Modifier.size(64.dp).clip(RoundedCornerShape(10.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        Image(painter, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        when (painter.state) {
            is AsyncImagePainter.State.Success -> Unit
            is AsyncImagePainter.State.Error -> Icon(Icons.Filled.Warning,
                contentDescription = "Immagine non disponibile", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> CircularProgressIndicator(Modifier.size(24.dp))
        }
    }
}

internal fun exerciseImageUrl(name: String): String =
    "https://firebasestorage.googleapis.com/v0/b/sportiliapp.appspot.com/o/$name.png?alt=media&token=cd00fa34-6a1f-4fa7-afa5-d80a1ef5cdaa"

@Preview(name = "Giorno nome lungo", showBackground = true, widthDp = 360)
@Preview(
    name = "Giorno superset dark font grande",
    showBackground = true,
    widthDp = 360,
    fontScale = 1.6f,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ExerciseRowPreview() {
    SportiliAppTheme {
        EsercizioRow(
            esercizio = Esercizio(
                name = "Distensioni su panca inclinata + Croci ai cavi dal basso",
                serie = "4 × 8 + 3 × 12",
                riposo = "1' 30\""
            ),
            onClick = {}
        )
    }
}
