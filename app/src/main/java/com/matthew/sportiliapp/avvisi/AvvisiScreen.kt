package com.matthew.sportiliapp.avvisi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvvisiScreen() {
    val viewModel: AlertsFeedViewModel = viewModel(
        factory = AlertsFeedViewModelFactory(ManualInjection.getAlertsUseCase)
    )
    val state by viewModel.uiState.collectAsState()

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
                    message = uiState.throwable.localizedMessage,
                    onRetry = viewModel::retry
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
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (activeAlerts.isNotEmpty()) {
                            item {
                                AlertsSectionTitle(
                                    title = "Da leggere",
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
    message: String?,
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
        if (!message.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
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
private fun AlertsSectionTitle(title: String, color: Color) {
    Text(
        text = title,
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

@Composable
fun AlertCardClean(alert: Avviso) {
    val weight = alert.urgencyWeight()
    val isExpired = alert.isExpired()
    val accent = when {
        isExpired -> MaterialTheme.colorScheme.onSurfaceVariant
        weight == 3 -> MaterialTheme.colorScheme.error
        weight == 2 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val icon = if (!isExpired && weight >= 3) Icons.Filled.Warning else Icons.Filled.Info
    val description = buildString {
        append(alert.titolo)
        append(". ")
        append(if (isExpired) "Avviso scaduto. " else "Avviso attivo. ")
        alert.urgenza?.takeIf { it.isNotBlank() }?.let {
            append("Urgenza $it. ")
        }
        alert.scadenza?.let { deadline ->
            val date = Instant.ofEpochMilli(deadline)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
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
                .padding(16.dp)
                .alpha(if (isExpired) 0.82f else 1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
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

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isExpired) {
                    MetaChip(
                        text = "Scaduto",
                        accent = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                alert.scadenza?.let { deadline ->
                    val date = Instant.ofEpochMilli(deadline)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    MetaChip(
                        text = if (isExpired) "Scaduto il $date" else "Scade il $date",
                        accent = accent
                    )
                }

                alert.urgenza?.takeIf { it.isNotBlank() }?.let { urgency ->
                    MetaChip(
                        text = "Urgenza: ${urgency.replaceFirstChar { it.uppercase() }}",
                        accent = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, accent: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = accent.copy(alpha = 0.10f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
