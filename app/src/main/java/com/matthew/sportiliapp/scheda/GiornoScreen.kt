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
    val useCompactAffordance = LocalDensity.current.fontScale < 1.3f
    val prescription = buildString {
        append("Serie ${esercizio.serie}")
        esercizio.riposo?.takeIf { it.isNotBlank() }?.let { append(", recupero $it") }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "${esercizio.name}. $prescription. Apri dettaglio esercizio"
            }
            .clickable(role = Role.Button, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {

            val painter = rememberAsyncImagePainter(
                model = "https://firebasestorage.googleapis.com/v0/b/sportiliapp.appspot.com/o/${esercizio.name}.png?alt=media&token=cd00fa34-6a1f-4fa7-afa5-d80a1ef5cdaa"
            )

            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                )

                when (painter.state) {
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                        )
                    }
                    is AsyncImagePainter.State.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null,
                                modifier = Modifier.align(Alignment.Center),
                            )
                        }
                    }
                    else -> {
                        // L'immagine è decorativa: nome e prescrizione sono annunciati dalla riga.
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = esercizio.name,
                    style = MaterialTheme.typography.bodyLarge,
                    minLines = 1,
                    maxLines = 3,
                    fontWeight = FontWeight.Bold,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = esercizio.serie,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                esercizio.riposo?.let { riposo ->
                    if (riposo.isNotEmpty()) {
                        Text(
                            text = "$riposo recupero",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (useCompactAffordance) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

        }
    }
}

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
