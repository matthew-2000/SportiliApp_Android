package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.ui.viewmodel.*
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import com.matthew.sportiliapp.ui.theme.sportiliStatusColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

internal enum class UserWorkoutStatus { CHANGE_REQUESTED, MISSING, EXPIRED, ACTIVE }
internal enum class AdminUserFilter { ALL, EXPIRED_OR_MISSING, CHANGE_REQUESTED }

internal data class AdminUserListItem(
    val user: Utente,
    val fullNameLower: String,
    val codeLower: String,
    val status: UserWorkoutStatus
)

internal fun resolveUserWorkoutStatus(user: Utente): UserWorkoutStatus {
    val workout = user.scheda
    return when {
        workout?.cambioRichiesto == true -> UserWorkoutStatus.CHANGE_REQUESTED
        workout == null -> UserWorkoutStatus.MISSING
        !workout.isSchedaValida() -> UserWorkoutStatus.EXPIRED
        else -> UserWorkoutStatus.ACTIVE
    }
}

internal fun indexAdminUsers(users: List<Utente>) = users.map { user ->
    AdminUserListItem(
        user = user,
        fullNameLower = "${user.nome} ${user.cognome}".lowercase(),
        codeLower = user.code.lowercase(),
        status = resolveUserWorkoutStatus(user)
    )
}

internal fun filterAdminUsers(
    users: List<AdminUserListItem>,
    searchText: String,
    filter: AdminUserFilter
): List<AdminUserListItem> {
    val search = searchText.trim().lowercase()
    return users.filter { item ->
        val matchesSearch = search.isBlank() || search in item.fullNameLower || search in item.codeLower
        val matchesFilter = when (filter) {
            AdminUserFilter.ALL -> true
            AdminUserFilter.EXPIRED_OR_MISSING ->
                item.status == UserWorkoutStatus.MISSING || item.status == UserWorkoutStatus.EXPIRED
            AdminUserFilter.CHANGE_REQUESTED -> item.status == UserWorkoutStatus.CHANGE_REQUESTED
        }
        matchesSearch && matchesFilter
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
    val usersViewModel: GymAdminViewModel = viewModel(
        factory = GymAdminViewModelFactory(
            ManualInjection.getUsersUseCase,
            ManualInjection.addUserUseCase,
            ManualInjection.updateUserUseCase,
            ManualInjection.removeUserUseCase
        )
    )
    val reportsViewModel: WorkoutReportsViewModel = viewModel(
        factory = WorkoutReportsViewModelFactory(
            ManualInjection.getWorkoutIssueReportsUseCase,
            ManualInjection.updateWorkoutIssueReportUseCase,
            ManualInjection.removeWorkoutIssueReportUseCase
        )
    )
    val uiState by usersViewModel.usersState.collectAsState()
    val reportsState by reportsViewModel.uiState.collectAsState()
    val openReportCount = (reportsState as? WorkoutReportsUiState.Success)
        ?.reports?.count { !it.resolved }

    AdminUserListScreen(
        uiState = uiState,
        openReportCount = openReportCount,
        onUserSelected = onUserSelected,
        onAddUser = onAddUser,
        onManageAlerts = onManageAlerts,
        onViewReports = onViewReports,
        onRetry = usersViewModel::loadUsers
    )
}

/** Presentation shared by the live admin and local fixtures; callbacks keep existing navigation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminUserListScreen(
    uiState: UiState<List<Utente>>,
    openReportCount: Int?,
    onUserSelected: (Utente) -> Unit,
    onAddUser: () -> Unit,
    onManageAlerts: () -> Unit,
    onViewReports: () -> Unit,
    onRetry: () -> Unit
) {
    var showAreas by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Utenti") },
                actions = {
                    Box {
                        IconButton(onClick = { showAreas = true }) {
                            Icon(Icons.Default.MoreVert, "Altre aree admin")
                        }
                        DropdownMenu(expanded = showAreas, onDismissRequest = { showAreas = false }) {
                            DropdownMenuItem(
                                text = { Text(openReportCount?.let { "Segnalazioni · $it aperte" } ?: "Segnalazioni") },
                                leadingIcon = { Icon(Icons.Default.Warning, null) },
                                onClick = { showAreas = false; onViewReports() }
                            )
                            DropdownMenuItem(
                                text = { Text("Avvisi") },
                                leadingIcon = { Icon(Icons.Default.Notifications, null) },
                                onClick = { showAreas = false; onManageAlerts() }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddUser,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuovo utente") }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            UiState.Loading -> AdminLoadingState(Modifier.padding(paddingValues))
            is UiState.Error -> AdminErrorState(Modifier.padding(paddingValues), onRetry)
            is UiState.Success -> UserListContent(state.data, onUserSelected, paddingValues)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UserListContent(
    users: List<Utente>,
    onUserSelected: (Utente) -> Unit,
    contentPadding: PaddingValues = PaddingValues()
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(AdminUserFilter.ALL) }
    val indexedUsers = remember(users) { indexAdminUsers(users) }
    val expiredCount = indexedUsers.count {
        it.status == UserWorkoutStatus.MISSING || it.status == UserWorkoutStatus.EXPIRED
    }
    val requestCount = indexedUsers.count { it.status == UserWorkoutStatus.CHANGE_REQUESTED }
    val filteredUsers = remember(indexedUsers, searchText, selectedFilter) {
        filterAdminUsers(indexedUsers, searchText, selectedFilter)
    }
    val hasActiveFilters = searchText.isNotBlank() || selectedFilter != AdminUserFilter.ALL

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            end = 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 128.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "${users.size} utenti · ${requestCount + expiredCount} schede da gestire",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Cerca per nome o codice") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchText.isNotEmpty()) {{
                    IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Clear, "Cancella ricerca")
                    }
                }} else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (filter in AdminUserFilter.entries) {
                    val label = when (filter) {
                        AdminUserFilter.ALL -> "Tutti ${users.size}"
                        AdminUserFilter.EXPIRED_OR_MISSING -> "Scadute o mancanti $expiredCount"
                        AdminUserFilter.CHANGE_REQUESTED -> "Richieste $requestCount"
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(label) },
                        leadingIcon = if (selectedFilter == filter) {{
                            Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                        }} else null
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (filteredUsers.size == 1) "1 risultato" else "${filteredUsers.size} risultati",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (hasActiveFilters) {
                    TextButton(onClick = {
                        searchText = ""
                        selectedFilter = AdminUserFilter.ALL
                    }) {
                        Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Azzera filtri")
                    }
                }
            }
        }
        if (filteredUsers.isEmpty()) {
            item {
                AdminUsersEmptyState(hasActiveFilters) {
                    searchText = ""
                    selectedFilter = AdminUserFilter.ALL
                }
            }
        } else {
            items(filteredUsers, key = { it.user.code }) { item ->
                UserRow(item.user, item.status) { onUserSelected(item.user) }
            }
        }
    }
}

@Composable
private fun UserRow(user: Utente, status: UserWorkoutStatus, onUserClick: () -> Unit) {
    val useLargeTextLayout = LocalDensity.current.fontScale >= 1.3f
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onUserClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(Modifier.padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (useLargeTextLayout) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserIdentity(user)
                    StatusChip(status)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserIdentity(user, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    StatusChip(status)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(workoutTimingLabel(user, status), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                Text("Apri", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun UserIdentity(user: Utente, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text("${user.nome} ${user.cognome}", style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold)
        Text("Codice ${user.code}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusChip(status: UserWorkoutStatus) {
    val colors = MaterialTheme.sportiliStatusColors
    val label: String
    val icon = when (status) {
        UserWorkoutStatus.CHANGE_REQUESTED -> Icons.Default.Info
        UserWorkoutStatus.MISSING, UserWorkoutStatus.EXPIRED -> Icons.Default.Warning
        UserWorkoutStatus.ACTIVE -> Icons.Default.CheckCircle
    }
    val container: Color
    val content: Color
    when (status) {
        UserWorkoutStatus.CHANGE_REQUESTED -> { label = "Cambio richiesto"; container = colors.infoContainer; content = colors.onInfoContainer }
        UserWorkoutStatus.MISSING -> { label = "Mancante"; container = MaterialTheme.colorScheme.errorContainer; content = MaterialTheme.colorScheme.onErrorContainer }
        UserWorkoutStatus.EXPIRED -> { label = "Scaduta"; container = colors.warningContainer; content = colors.onWarningContainer }
        UserWorkoutStatus.ACTIVE -> { label = "Attiva"; container = colors.successContainer; content = colors.onSuccessContainer }
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun workoutTimingLabel(user: Utente, status: UserWorkoutStatus): String {
    val workout = user.scheda ?: return "Nessuna scheda assegnata"
    val start = runCatching { Scheda.dateFormatter.parse(workout.dataInizio) }.getOrNull()
        ?: return "Data della scheda non disponibile"
    val end = Calendar.getInstance().apply { time = start; add(Calendar.WEEK_OF_YEAR, workout.durata) }.time
    val date = SimpleDateFormat("d MMM yyyy", Locale.ITALIAN).format(end)
    return if (status == UserWorkoutStatus.EXPIRED) "Terminata il $date" else "Termina il $date"
}

@Composable
private fun AdminUsersEmptyState(hasActiveFilters: Boolean, onReset: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(if (hasActiveFilters) Icons.Default.Search else Icons.Default.Info, null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
        Text(if (hasActiveFilters) "Nessun utente corrisponde ai filtri" else "Nessun utente presente",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            if (hasActiveFilters) "Modifica la ricerca oppure azzera i filtri per vedere l’elenco completo."
            else "Aggiungi il primo utente con il pulsante in basso.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (hasActiveFilters) Button(onClick = onReset) { Text("Azzera filtri") }
    }
}

@Composable
private fun AdminLoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(); Spacer(Modifier.height(12.dp)); Text("Caricamento utenti…")
        }
    }
}

@Composable
private fun AdminErrorState(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
            Text("Impossibile caricare gli utenti", style = MaterialTheme.typography.titleMedium)
            Text("Controlla la connessione e riprova.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRetry) {
                Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Riprova")
            }
        }
    }
}

internal fun previewAdminUsers() = listOf(
    Utente("SPT-042", "Rossi", "Giulia", Scheda("2099-01-01T08:00:00+0100", 8, cambioRichiesto = true)),
    Utente("SPT-108", "Bianchi con un cognome molto lungo", "Alessandro", Scheda("2020-01-01T08:00:00+0100", 6)),
    Utente("SPT-211", "Verdi", "Sara"),
    Utente("SPT-315", "Romano", "Luca", Scheda("2099-03-01T08:00:00+0100", 12))
)

@Preview(name = "Admin - light", showBackground = true, widthDp = 393, heightDp = 852)
@Composable private fun AdminUsersLightPreview() = SportiliAppTheme { AdminUserListPreviewScreen() }

@Preview(name = "Admin - dark", showBackground = true, widthDp = 393, heightDp = 852)
@Composable private fun AdminUsersDarkPreview() = SportiliAppTheme(isDarkTheme = true) { AdminUserListPreviewScreen() }

@Preview(name = "Admin - font grande", showBackground = true, widthDp = 393, heightDp = 852, fontScale = 1.6f)
@Composable private fun AdminUsersLargeFontPreview() = SportiliAppTheme { AdminUserListPreviewScreen() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AdminUserListPreviewScreen() {
    AdminUserListScreen(UiState.Success(previewAdminUsers()), 3, {}, {}, {}, {}, {})
}
