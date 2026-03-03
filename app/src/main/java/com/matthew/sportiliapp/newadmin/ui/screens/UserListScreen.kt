package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.EsercizioPredefinito
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.ui.viewmodel.GymAdminViewModel
import com.matthew.sportiliapp.newadmin.ui.viewmodel.GymAdminViewModelFactory
import com.matthew.sportiliapp.newadmin.ui.viewmodel.UiState

private enum class UserWorkoutStatus { CHANGE_REQUESTED, MISSING, EXPIRED, ACTIVE }

private data class AdminUserListItem(
    val user: Utente,
    val fullNameLower: String,
    val codeLower: String,
    val status: UserWorkoutStatus
)

private fun resolveUserWorkoutStatus(user: Utente): UserWorkoutStatus {
    val scheda = user.scheda
    return when {
        scheda?.cambioRichiesto == true -> UserWorkoutStatus.CHANGE_REQUESTED
        scheda == null -> UserWorkoutStatus.MISSING
        scheda.isSchedaValida().not() -> UserWorkoutStatus.EXPIRED
        else -> UserWorkoutStatus.ACTIVE
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserListScreen(
    onUserSelected: (Utente) -> Unit,
    onAddUser: () -> Unit,
    onManageAlerts: () -> Unit,
    onViewReports: () -> Unit
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

    // STATO DEL FILTRO "SOLO RICHIESTE CAMBIO"
    var onlyRequests by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utenti") },
                actions = {
                    IconButton(onClick = onViewReports) {
                        Icon(Icons.Default.Warning, contentDescription = "Segnalazioni schede")
                    }
                    IconButton(onClick = onManageAlerts) {
                        Icon(Icons.Default.Notifications, contentDescription = "Gestisci avvisi")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddUser) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add User")
            }
        },
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
                val indexedUsers = remember(allUsers) {
                    allUsers.map { user ->
                        AdminUserListItem(
                            user = user,
                            fullNameLower = "${user.nome} ${user.cognome}".lowercase(),
                            codeLower = user.code.lowercase(),
                            status = resolveUserWorkoutStatus(user)
                        )
                    }
                }

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

                    // FILTRI
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = onlyExpired,
                            onClick = { onlyExpired = !onlyExpired },
                            label = { Text("Scadute") },
                            leadingIcon = if (onlyExpired) {
                                { Icon(Icons.Default.Warning, contentDescription = null) }
                            } else null
                        )

                        FilterChip(
                            selected = onlyRequests,
                            onClick = { onlyRequests = !onlyRequests },
                            label = { Text("Richieste cambio") },
                            leadingIcon = if (onlyRequests) {
                                { Icon(Icons.Default.Info, contentDescription = null) }
                            } else null
                        )
                    }

                    // LISTA UTENTI FILTRATA
                    val normalizedSearch = searchText.trim().lowercase()
                    val filteredUsers = indexedUsers.filter { item ->
                        val matchSearch = normalizedSearch.isBlank() ||
                            normalizedSearch in item.fullNameLower ||
                            normalizedSearch in item.codeLower

                        val matchExpirationFilter = if (onlyExpired) {
                            item.status == UserWorkoutStatus.MISSING ||
                                item.status == UserWorkoutStatus.EXPIRED
                        } else {
                            true
                        }
                        val matchRequestFilter = if (onlyRequests) {
                            item.status == UserWorkoutStatus.CHANGE_REQUESTED
                        } else {
                            true
                        }

                        matchSearch && matchExpirationFilter && matchRequestFilter
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp)
                    ) {
                        items(filteredUsers, key = { it.user.code }) { item ->
                            UserCard(
                                user = item.user,
                                status = item.status,
                                onUserClick = { onUserSelected(item.user) }
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
private fun UserCard(
    user: Utente,
    status: UserWorkoutStatus = resolveUserWorkoutStatus(user),
    onUserClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onUserClick() },
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp) // un po’ di spazio dal chip
                ) {
                    Text(
                        text = "${user.nome} ${user.cognome}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis // <-- tronca se troppo lungo
                    )
                    Text(
                        text = "Code: ${user.code}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stato scheda con colori personalizzati
                when (status) {
                    UserWorkoutStatus.CHANGE_REQUESTED -> {
                        StatusChip("Cambio richiesto", MaterialTheme.colorScheme.error)
                    }
                    UserWorkoutStatus.MISSING -> {
                        StatusChip("Mancante", Color.Gray)
                    }
                    UserWorkoutStatus.EXPIRED -> {
                        StatusChip("Scaduta", MaterialTheme.colorScheme.primary)
                    }
                    UserWorkoutStatus.ACTIVE -> {
                        StatusChip("Attiva", Color.Green)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
