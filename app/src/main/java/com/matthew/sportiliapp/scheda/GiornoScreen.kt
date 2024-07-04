package com.matthew.sportiliapp.scheda


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiornoScreen(navController: NavHostController, giorno: Giorno) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = giorno.name) }
            )
        },
        content = { padding ->
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(giorno.gruppiMuscolari.entries.toList()) { (_, gruppo) ->
                    GruppoSection(gruppo = gruppo)
                }
            }
        }
    )
}

@Composable
fun GruppoSection(gruppo: GruppoMuscolare) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = gruppo.nome,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Column {
            gruppo.esercizi.forEach { esercizio ->
                EsercizioRow(esercizio = esercizio.value)
            }
        }
    }
}

@Composable
fun EsercizioRow(esercizio: Esercizio) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        
        Box(modifier = Modifier
            .clip(shape = RoundedCornerShape(15.dp))
            .size(100.dp)
            .background(color = Color.Gray)
            )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = esercizio.name,
                style = MaterialTheme.typography.titleSmall,
                minLines = 1,
                maxLines = 4,
                fontWeight = FontWeight.Bold,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = esercizio.serie,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            esercizio.riposo?.let { riposo ->
                if (riposo.isNotEmpty()) {
                    Text(
                        text = "$riposo riposo",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

