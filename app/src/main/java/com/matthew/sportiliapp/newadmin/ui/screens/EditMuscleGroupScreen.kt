package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.EsercizioPredefinito
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.EserciziPredefinitiViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMuscleGroupScreen(
    userCode: String,
    dayKey: String,
    group: GruppoMuscolare,
    onSave: (GruppoMuscolare) -> Unit,
    onCancel: () -> Unit
) {
    var groupName by remember { mutableStateOf(group.nome) }
    // Mappa per tenere traccia degli esercizi predefiniti selezionati: key = id esercizio, value = Esercizio con dati completi
    val selectedExercises = remember { mutableStateMapOf<String, Esercizio>() }

    // Inizializzazione del ViewModel (viene creato una sola volta)
    val viewModel = remember { EserciziPredefinitiViewModel() }
    val predefiniti by viewModel.gruppiMuscolariPredefiniti.observeAsState(emptyList())
    // Filtriamo il gruppo predefinito in base al nome
    val predefinitiGruppo: List<EsercizioPredefinito> =
        predefiniti.firstOrNull { it.nome == group.nome }?.esercizi ?: emptyList()

    BackHandler {
        // TODO: Handle save group
        onCancel()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Modifica Gruppo") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(padding)
        ) {
            // Campo per modificare il nome del gruppo
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                label = { Text("Nome del Gruppo") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Seleziona gli esercizi predefiniti",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Griglia degli esercizi predefiniti
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(predefinitiGruppo) { esercizioPredefinito ->
                    // Verifichiamo se l'esercizio è selezionato
                    val isSelected = selectedExercises.containsKey(esercizioPredefinito.id)
                    Card(
                        modifier = Modifier
                            .padding(4.dp)
                            .fillMaxWidth()
                            .clickable {
                                // Al click, alterniamo lo stato di selezione
                                if (isSelected) {
                                    selectedExercises.remove(esercizioPredefinito.id)
                                } else {
                                    // Aggiungiamo l'esercizio con serie vuota (l'utente potrà completare i dati)
                                    selectedExercises[esercizioPredefinito.id] =
                                        Esercizio(name = esercizioPredefinito.nome, serie = "")
                                }
                            },
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(8.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = esercizioPredefinito.nome,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            // Se l'esercizio è selezionato, mostra il campo per inserire le informazioni (ad es. serie)
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = selectedExercises[esercizioPredefinito.id]?.serie ?: "",
                                    onValueChange = { newSerie ->
                                        selectedExercises[esercizioPredefinito.id] =
                                            selectedExercises[esercizioPredefinito.id]?.copy(serie = newSerie)
                                                ?: Esercizio(name = esercizioPredefinito.nome, serie = newSerie)
                                    },
                                    label = { Text("Serie (es. 3x8)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Pulsanti per annullare o salvare il gruppo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Annulla")
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = {
                        // Costruiamo la mappa degli esercizi da inserire nel gruppo
                        val exercisesMap = LinkedHashMap<String, Esercizio>()
                        var index = 1
                        selectedExercises.values.forEach { esercizio ->
                            exercisesMap["esercizio$index"] = esercizio
                            index++
                        }
                        // Creiamo il gruppo aggiornato e chiamiamo la callback onSave
                        val updatedGroup = group.copy(nome = groupName, esercizi = exercisesMap)
                        onSave(updatedGroup)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Salva")
                }
            }
        }
    }
}