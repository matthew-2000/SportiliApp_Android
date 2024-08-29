package com.matthew.sportiliapp.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGruppoMuscolareScreen(
    navController: NavController,
    giorno: Giorno,
    onGruppoMuscolareAdded: (GruppoMuscolare) -> Unit
) {
    var gruppoName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aggiungi Gruppo Muscolare") },
                actions = {
                    IconButton(onClick = {
                        if (gruppoName.isNotEmpty()) {
                            val newGruppoMuscolare = GruppoMuscolare(gruppoName)
                            onGruppoMuscolareAdded(newGruppoMuscolare)
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.Done, contentDescription = "Conferma")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = gruppoName,
                onValueChange = { gruppoName = it },
                label = { Text("Nome Gruppo Muscolare") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (gruppoName.isNotEmpty()) {
                    val newGruppoMuscolare = GruppoMuscolare(gruppoName)
                    onGruppoMuscolareAdded(newGruppoMuscolare)
                    navController.popBackStack()
                }
            }) {
                Text("Aggiungi Gruppo Muscolare")
            }
        }
    }
}
