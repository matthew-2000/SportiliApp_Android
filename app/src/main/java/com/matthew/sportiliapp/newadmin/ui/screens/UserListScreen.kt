// newadmin/ui/screens/UserListScreen.kt
package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("Utenti") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddUser) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add User")
            }
        }
    ) { padding ->
        when (uiState) {
            is UiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) { Text(text = "Caricamento...") }
            is UiState.Success -> {
                val users = (uiState as UiState.Success<List<Utente>>).data
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    items(users) { user ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                                .clickable { onUserSelected(user) },
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = "${user.nome} ${user.cognome}", style = MaterialTheme.typography.titleMedium)
                                Text(text = "Code: ${user.code}", style = MaterialTheme.typography.bodyMedium)
                                if (user.scheda == null) {
                                    Text("Scheda mancante!", color = MaterialTheme.colorScheme.error)
                                } else {
                                    // Assume this is implemented within Utente class
                                    if (!user.scheda?.isSchedaValida()!!) {
                                        Text("Scheda scaduta!", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is UiState.Error -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(text = "Error: ${(uiState as UiState.Error).exception.message}")
            }
        }
    }
}
