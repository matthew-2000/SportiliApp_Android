package com.matthew.sportiliapp.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GymViewModel
import com.matthew.sportiliapp.model.Scheda
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSchedaScreen(
    navController: NavController,
    gymViewModel: GymViewModel,
    utenteCode: String,
    onDismiss: () -> Unit
) {
    val users by gymViewModel.users.observeAsState(emptyList())
    val utente = users.firstOrNull { it.code == utenteCode } ?: return
    val scheda = utente.scheda ?: Scheda("", 0)

    var dataInizio by remember { mutableStateOf(scheda.dataInizio) }
    var durata by remember { mutableStateOf(scheda.durata.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifica Scheda") },
                actions = {
                    IconButton(onClick = {
                        val updatedScheda = Scheda(dataInizio, durata.toInt(), scheda.giorni)
                        gymViewModel.saveScheda(updatedScheda, utenteCode)
                        onDismiss()
                    }) {
                        Icon(Icons.Default.Done, contentDescription = "Salva")
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

            // DatePicker Field per la Data Inizio
            val context = LocalContext.current
            val calendar = Calendar.getInstance()
            val datePickerDialog = android.app.DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    dataInizio = "$dayOfMonth/${month + 1}/$year"
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            OutlinedTextField(
                value = dataInizio,
                onValueChange = {},
                label = { Text("Data Inizio") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(imageVector = Icons.Default.DateRange, contentDescription = "Select Date")
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = durata,
                onValueChange = { durata = it },
                label = { Text("Durata (giorni)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // UI per i giorni della scheda
            Text("Giorni della Scheda", style = MaterialTheme.typography.headlineSmall)
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            scheda.giorni.forEach { (dayName, giorno) ->
                GiornoItem(giorno = giorno) {
                    navController.navigate("addGruppoMuscolareScreen/${dayName}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                navController.navigate("addGiornoScreen")
            }) {
                Text("Aggiungi Giorno")
            }
        }
    }
}

@Composable
fun GiornoItem(giorno: Giorno, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onEdit),
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(text = giorno.name, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Gruppi Muscolari: ${giorno.gruppiMuscolari.size}")
        }
    }
}

