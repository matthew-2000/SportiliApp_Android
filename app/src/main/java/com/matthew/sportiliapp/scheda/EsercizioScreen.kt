package com.matthew.sportiliapp.scheda

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.matthew.sportiliapp.model.Esercizio
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EsercizioScreen(esercizio: Esercizio, navController: NavHostController) {
    val notaState = remember { mutableStateOf(TextFieldValue(esercizio.noteUtente ?: "")) }
    val showingAlertState = remember { mutableStateOf(false) }

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
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                Text(
                    text = esercizio.name,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 16.dp) // Padding per distanziare dal resto del contenuto
                )

                val painter = rememberAsyncImagePainter(
                    model = "https://firebasestorage.googleapis.com/v0/b/sportiliapp.appspot.com/o/${esercizio.name}.png?alt=media&token=cd00fa34-6a1f-4fa7-afa5-d80a1ef5cdaa"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
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
                        }
                        is AsyncImagePainter.State.Error -> {
                            // Display a placeholder or error icon if the image fails to load
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = "Immagine non ancora disponibile",
                                    modifier = Modifier.align(Alignment.Center),
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        else -> {
                            // Do nothing, the image will be displayed
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                ) {
                    Text(
                        text = esercizio.serie,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    esercizio.riposo?.let {
                        if (it.isNotEmpty()) {
                            Text(text = "$it recupero", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Note:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = esercizio.notePT?.takeIf { it.isNotEmpty() } ?: "Nessuna nota.",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

            }
        }
    )
}

