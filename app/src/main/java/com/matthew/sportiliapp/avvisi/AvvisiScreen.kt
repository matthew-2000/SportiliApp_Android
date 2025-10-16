package com.matthew.sportiliapp.avvisi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun AvvisiScreen() {
    val viewModel: AlertsFeedViewModel = viewModel(
        factory = AlertsFeedViewModelFactory(ManualInjection.getAlertsUseCase)
    )
    val state by viewModel.uiState.collectAsState()

    when (val uiState = state) {
        AlertsFeedUiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Caricamento avvisi...")
            }
        }

        is AlertsFeedUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Impossibile caricare gli avvisi")
            }
        }

        is AlertsFeedUiState.Success -> {
            val alerts = uiState.alerts
            if (alerts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Nessun avviso disponibile")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(alerts, key = { it.id }) { alert ->
                        AlertCard(alert = alert)
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: Avviso) {
    val urgencyColor = when (alert.urgencyWeight()) {
        3 -> MaterialTheme.colorScheme.error
        2 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(alert.titolo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            alert.scadenza?.let { deadline ->
                val date = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate()
                Text(
                    text = "Fino al ${date.format(DateTimeFormatter.ISO_LOCAL_DATE)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(alert.descrizione, style = MaterialTheme.typography.bodyMedium)
            alert.urgenza?.takeIf { it.isNotBlank() }?.let { urgency ->
                RowWithIcon(
                    icon = if (alert.urgencyWeight() >= 3) Icons.Default.Warning else Icons.Default.Info,
                    text = "Urgenza: ${urgency.replaceFirstChar { it.uppercase() }}",
                    tint = urgencyColor
                )
            }
        }
    }
}

@Composable
private fun RowWithIcon(icon: ImageVector, text: String, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(text, style = MaterialTheme.typography.bodySmall, color = tint)
    }
}
