package com.matthew.sportiliapp.scheda


import android.os.Bundle
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
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
                    GruppoSection(gruppo = gruppo, navController)
                }
            }
        }
    )
}

@Composable
fun GruppoSection(gruppo: GruppoMuscolare, navController: NavHostController) {
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
                EsercizioRow(esercizio = esercizio.value) {
                    val bundle = Bundle()
                    bundle.putParcelable("esercizio", esercizio.value)
                    navController.navigate(
                        route = "esercizio",
                        args = bundle
                    )
                }
            }
        }
    }
}

@Composable
fun EsercizioRow(esercizio: Esercizio, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 16.dp)
            .clickable { onClick() }
    ) {

        val painter = rememberAsyncImagePainter(
            model = "https://firebasestorage.googleapis.com/v0/b/sportiliapp.appspot.com/o/${esercizio.name}.png?alt=media&token=cd00fa34-6a1f-4fa7-afa5-d80a1ef5cdaa"
        )

        Box(
            modifier = Modifier
                .size(100.dp)
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
                    // Display a placeholder while the image loads
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                    )
                }
                is AsyncImagePainter.State.Error -> {
                    // Display a placeholder or error icon if the image fails to load
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
                    // Do nothing, the image will be displayed
                }
            }
        }

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

