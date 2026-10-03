package com.matthew.sportiliapp.avvisi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import com.matthew.sportiliapp.ui.theme.SportiliSpacing
import com.matthew.sportiliapp.ui.theme.sportiliStatusColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvvisiScreen() {
    val viewModel: AlertsFeedViewModel = viewModel(
        factory = AlertsFeedViewModelFactory(ManualInjection.getAlertsUseCase)
    )
    val state by viewModel.uiState.collectAsState()

    AvvisiContent(state = state, onRetry = viewModel::retry)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AvvisiContent(
    state: AlertsFeedUiState,
    onRetry: () -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Avvisi") },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { padding ->
        when (val uiState = state) {
            AlertsFeedUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Caricamento avvisi...")
                }
            }

            is AlertsFeedUiState.Error -> {
                ErrorScreen(
                    padding = padding,
                    onRetry = onRetry
                )
            }

            is AlertsFeedUiState.Success -> {
                val alerts = uiState.alerts
                if (alerts.isEmpty()) {
                    EmptyAlertsScreen(padding)
                } else {
                    val orderedAlerts = alerts.sortedWith(
                        compareByDescending<Avviso> { !it.isExpired() }
                            .thenByDescending { it.urgencyWeight() }
                            .thenBy { it.scadenza ?: Long.MAX_VALUE }
                    )
                    val activeAlerts = orderedAlerts.filterNot { it.isExpired() }
                    val expiredAlerts = orderedAlerts.filter { it.isExpired() }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = SportiliSpacing.standard),
                        verticalArrangement = Arrangement.spacedBy(SportiliSpacing.small),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (activeAlerts.isNotEmpty()) {
                            item {
                                AlertsSectionTitle(
                                    title = "Attivi",
                                    count = activeAlerts.size,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(activeAlerts, key = { it.id }) { alert ->
                                AlertCardClean(alert)
                            }
                        }

                        if (expiredAlerts.isNotEmpty()) {
                            item {
                                AlertsSectionTitle(
                                    title = "Scaduti",
                                    count = expiredAlerts.size,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            items(expiredAlerts, key = { it.id }) { alert ->
                                AlertCardClean(alert)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorScreen(
    padding: PaddingValues,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Errore durante il caricamento degli avvisi",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Controlla la connessione e riprova.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Riprova")
        }
    }
}

@Composable
fun EmptyAlertsScreen(padding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = "Informazioni",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Nessun avviso disponibile",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Controlla più tardi per nuovi aggiornamenti.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AlertsSectionTitle(title: String, count: Int, color: Color) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.headlineSmall,
        color = color,
        fontWeight = FontWeight.SemiBold
    )
    HorizontalDivider(
        modifier = Modifier.padding(top = 8.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AlertCardClean(alert: Avviso) {
    val weight = alert.urgencyWeight()
    val isExpired = alert.isExpired()
    val status = MaterialTheme.sportiliStatusColors
    val accent = when {
        isExpired -> MaterialTheme.colorScheme.onSurfaceVariant
        weight == 3 -> MaterialTheme.colorScheme.error
        weight == 2 -> status.onWarningContainer
        else -> status.onInfoContainer
    }
    val container = when {
        isExpired -> MaterialTheme.colorScheme.surfaceVariant
        weight == 3 -> MaterialTheme.colorScheme.errorContainer
        weight == 2 -> status.warningContainer
        else -> status.infoContainer
    }
    val icon = if (!isExpired && weight >= 2) Icons.Filled.Warning else Icons.Filled.Info
    val description = buildString {
        append(alert.titolo)
        append(". ")
        append(alert.descrizione.trim().trimEnd('.'))
        append(". ")
        append(if (isExpired) "Avviso scaduto. " else "Avviso attivo. ")
        alert.urgenza?.takeIf { it.isNotBlank() }?.let {
            append("Priorità $it. ")
        }
        alert.scadenza?.let { deadline ->
            val date = Instant.ofEpochMilli(deadline)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(alertDateFormatter)
            append(if (isExpired) "Scaduto il $date." else "Scade il $date.")
        }
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = description
            },
        border = BorderStroke(1.dp, accent.copy(alpha = if (isExpired) 0.25f else 0.45f)),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SportiliSpacing.standard),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(SportiliSpacing.compact))
                Text(
                    text = alert.titolo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = alert.descrizione,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(SportiliSpacing.compact),
                verticalArrangement = Arrangement.spacedBy(SportiliSpacing.compact)
            ) {
                if (isExpired) {
                    MetaChip(
                        text = "Scaduto",
                        accent = accent,
                        container = container
                    )
                }

                alert.scadenza?.let { deadline ->
                    val date = Instant.ofEpochMilli(deadline)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(alertDateFormatter)
                    MetaChip(
                        text = if (isExpired) "Scaduto il $date" else "Scade il $date",
                        accent = accent,
                        container = container
                    )
                }

                alert.urgenza?.takeIf { it.isNotBlank() }?.let { urgency ->
                    MetaChip(
                        text = "Priorità: ${urgency.replaceFirstChar { it.uppercase() }}",
                        accent = accent,
                        container = container
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, accent: Color, container: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = container,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = SportiliSpacing.extraSmall)
        )
    }
}

private val alertDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)

private val previewAlerts = listOf(
    Avviso(
        id = "urgent",
        titolo = "Chiusura straordinaria della sala pesi",
        descrizione = "La sala pesi chiude alle 19:00 per manutenzione.",
        urgenza = "alta",
        scadenza = System.currentTimeMillis() + 86_400_000L
    ),
    Avviso(
        id = "course",
        titolo = "Nuovo orario corso mobility",
        descrizione = "Da lunedì il corso inizierà alle 18:30.",
        urgenza = "media",
        scadenza = System.currentTimeMillis() + 604_800_000L
    ),
    Avviso(
        id = "expired",
        titolo = "Orario festivo",
        descrizione = "Comunicazione non più attiva.",
        urgenza = "bassa",
        scadenza = System.currentTimeMillis() - 86_400_000L
    )
)

@Preview(name = "Avvisi", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun AvvisiPreview() {
    SportiliAppTheme {
        AvvisiContent(AlertsFeedUiState.Success(previewAlerts), onRetry = {})
    }
}

@Preview(name = "Avvisi dark", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun AvvisiDarkPreview() {
    SportiliAppTheme(isDarkTheme = true) {
        AvvisiContent(AlertsFeedUiState.Success(previewAlerts), onRetry = {})
    }
}

@Preview(
    name = "Avvisi font grandi",
    showBackground = true,
    widthDp = 360,
    heightDp = 760,
    fontScale = 1.6f
)
@Composable
private fun AvvisiLargeFontPreview() {
    SportiliAppTheme {
        AvvisiContent(AlertsFeedUiState.Success(previewAlerts), onRetry = {})
    }
}

@Preview(name = "Avvisi vuoti", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun AvvisiEmptyPreview() {
    SportiliAppTheme {
        AvvisiContent(AlertsFeedUiState.Success(emptyList()), onRetry = {})
    }
}

@Preview(name = "Errore avvisi", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun AvvisiErrorPreview() {
    SportiliAppTheme {
        AvvisiContent(
            state = AlertsFeedUiState.Error(IllegalStateException("Permission denied")),
            onRetry = {}
        )
    }
}
