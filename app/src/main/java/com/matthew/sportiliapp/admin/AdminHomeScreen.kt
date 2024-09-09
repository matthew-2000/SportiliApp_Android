package com.matthew.sportiliapp.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.matthew.sportiliapp.model.GymViewModel
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.resetSharedPref
import com.matthew.sportiliapp.scheda.convertDateTime
import com.matthew.sportiliapp.scheda.navigate
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminHomeScreen(navController: NavHostController) {
    val gymViewModel = GymViewModel()
    val users by gymViewModel.users.observeAsState(initial = emptyList())
    var searchText by remember { mutableStateOf("") }
    var showAddUserView by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var isLoggedOut by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = { showAddUserView = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add User")
                    }
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SearchBar(text = searchText, onTextChange = { searchText = it })
            UserList(users = users, searchText = searchText, navController = navController)
        }
    }

    if (showAddUserView) {
        AddUserView(gymViewModel = gymViewModel, onDismiss = { showAddUserView = false })
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Esegui il logout
                        FirebaseAuth.getInstance().signOut()
                        resetSharedPref(context)
                        isLoggedOut = true
                        showLogoutDialog = false
                    }
                ) {
                    Text("Conferma")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Annulla")
                }
            },
            title = { Text("Logout") },
            text = { Text("Sei sicuro di voler effettuare il logout?") }
        )
    }

    if (isLoggedOut) {
        navController.navigate("login") {
            popUpTo(navController.graph.startDestinationId) {
                inclusive = true
            }
        }
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
fun UserList(users: List<Utente>, searchText: String, navController: NavHostController) {
    val filteredUsers = users.filter {
        it.nome.contains(searchText, ignoreCase = true) ||
                it.cognome.contains(searchText, ignoreCase = true) ||
                it.code.contains(searchText, ignoreCase = true)
    }

    LazyColumn {
        items(filteredUsers) { utente ->
            UserRow(utente) {
                navController.navigate(
                    route = "utenteNavHost?utenteCode=${utente.code}"
                )
            }
        }
    }
}

@Composable
fun UserRow(utente: Utente, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${utente.nome} ${utente.cognome}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                utente.code,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium
            )
            if (utente.scheda == null) {
                Text("Scheda mancante!", color = MaterialTheme.colorScheme.error)
            } else {
                // Assume this is implemented within Utente class
                if (!utente.scheda?.isSchedaValida()!!) {
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
                TextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome") })
                TextField(value = cognome, onValueChange = { cognome = it }, label = { Text("Cognome") })
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nome.isNotEmpty() && cognome.isNotEmpty()) {
                    cognome = cognome.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    nome = nome.trim().replaceFirstChar {  if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                    code = nome.substring(0,3) + cognome.substring(0,3) + (0..999).random()
                    gymViewModel.addUser(code, cognome, nome)
                    onDismiss()
                }
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
