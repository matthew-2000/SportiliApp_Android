package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.ui.viewmodel.GymAdminViewModel
import com.matthew.sportiliapp.newadmin.ui.viewmodel.GymAdminViewModelFactory
import com.matthew.sportiliapp.newadmin.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    onUserSelected: (Utente) -> Unit,
    onAddUser: () -> Unit
) {
    val gymAdminViewModel: GymAdminViewModel = viewModel(
        factory = GymAdminViewModelFactory(
            ManualInjection.getUsersUseCase,
            ManualInjection.addUserUseCase,
            ManualInjection.updateUserUseCase,
            ManualInjection.removeUserUseCase
        )
    )
    val uiState by gymAdminViewModel.usersState.collectAsState()

    // TESTO DI RICERCA
    var searchText by remember { mutableStateOf("") }

    // STATO DEL FILTRO "SOLO SCADUTE"
    var onlyExpired by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utenti") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddUser) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add User")
            }
        }
    ) { paddingValues ->
        // Gestione degli stati
        when (uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(text = "Caricamento...")
                }
            }
            is UiState.Success -> {
                val allUsers = (uiState as UiState.Success<List<Utente>>).data

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // CAMPO DI TESTO PER LA RICERCA
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Cerca utente...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )

                    // CHECKBOX FILTRO "SOLO SCADUTE"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Checkbox(
                            checked = onlyExpired,
                            onCheckedChange = { onlyExpired = it }
                        )
                        Text(text = "Mostra solo schede scadute")
                    }

                    // LISTA UTENTI FILTRATA
                    val filteredUsers = allUsers.filter { user ->
                        val fullName = "${user.nome} ${user.cognome}".lowercase()
                        val code = user.code?.lowercase().orEmpty()
                        val matchSearch = searchText.lowercase() in fullName ||
                                searchText.lowercase() in code ||
                                searchText.isBlank()

                        // Logica per considerare la scheda "scaduta"
                        val isExpired = user.scheda == null || user.scheda?.isSchedaValida() == false

                        // Se onlyExpired == true, mostro solo gli scaduti
                        // Altrimenti mostro tutti
                        val matchExpirationFilter = if (onlyExpired) isExpired else true

                        matchSearch && matchExpirationFilter
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(filteredUsers) { user ->
                            UserCard(
                                user = user,
                                onUserClick = { onUserSelected(user) }
                            )
                        }
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text(text = "Error: ${(uiState as UiState.Error).exception.message}")
                }
            }
        }
    }
}

@Composable
fun UserCard(
    user: Utente,
    onUserClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onUserClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${user.nome} ${user.cognome}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Code: ${user.code}",
                style = MaterialTheme.typography.bodyMedium
            )
            when {
                user.scheda == null -> {
                    Text(
                        text = "Scheda mancante!",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                user.scheda?.isSchedaValida() == false -> {
                    Text(
                        text = "Scheda scaduta!",
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {
                    // Scheda presente e valida
                }
            }
        }
    }
}
