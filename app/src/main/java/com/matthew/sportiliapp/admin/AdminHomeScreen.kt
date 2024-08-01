package com.matthew.sportiliapp.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.matthew.sportiliapp.model.GymViewModel
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.scheda.convertDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(navController: NavHostController) {
    val gymViewModel = GymViewModel()
    val users by gymViewModel.users.observeAsState(initial = emptyList())
    var searchText by remember { mutableStateOf("") }
    var showAddUserView by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = { showAddUserView = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add User")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchBar(text = searchText, onTextChange = { searchText = it })
            UserList(users = users, searchText = searchText)
        }
    }

    if (showAddUserView) {
        AddUserView(gymViewModel = gymViewModel, onDismiss = { showAddUserView = false })
    }
}

@Composable
fun SearchBar(text: String, onTextChange: (String) -> Unit) {
    TextField(
        value = text,
        onValueChange = onTextChange,
        label = { Text("Search") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}

@Composable
fun UserList(users: List<Utente>, searchText: String) {
    val filteredUsers = users.filter {
        it.nome.contains(searchText, ignoreCase = true) ||
                it.cognome.contains(searchText, ignoreCase = true) ||
                it.code.contains(searchText, ignoreCase = true)
    }

    LazyColumn {
        items(filteredUsers) { utente ->
            UserRow(utente)
        }
    }
}

@Composable
fun UserRow(utente: Utente) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${utente.nome} ${utente.cognome}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (utente.scheda == null) {
                Text("Scheda mancante!", color = MaterialTheme.colorScheme.error)
            } else {
                // Assume this is implemented within Utente class
                if (utente.scheda?.getDurataScheda() == null) {
                    Text("Scheda scaduta!", color = MaterialTheme.colorScheme.error)
                }
            }
            Divider(color = Color.LightGray, thickness = 1.dp)
        }
    }
}

@Composable
fun AddUserView(gymViewModel: GymViewModel, onDismiss: () -> Unit) {
    var code by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    var cognome by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text("Aggiungi utente") },
        text = {
            Column {
                TextField(value = code, onValueChange = { code = it }, label = { Text("Codice") })
                TextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") })
                TextField(value = cognome, onValueChange = { cognome = it }, label = { Text("Cognome") })
            }
        },
        confirmButton = {
            Button(onClick = {
                gymViewModel.addUser(code, cognome, nome)
                onDismiss()
            }) {
                Text("Salva")
            }
        },
        dismissButton = {
            Button(onClick = { onDismiss() }) {
                Text("Annulla")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewAdminHomeView() {
    AdminHomeScreen(navController = rememberNavController())
}
