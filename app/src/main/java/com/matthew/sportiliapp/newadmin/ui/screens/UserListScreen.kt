package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    onViewReports: () -> Unit,
    compactMode: Boolean = false
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

    Scaffold(
        topBar = { TopAppBar(title = { Text(if (compactMode) "Utenti" else "Amministrazione") }) },
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
            is UiState.Error -> AdminErrorState(
                modifier = Modifier.padding(paddingValues),
                onRetry = usersViewModel::loadUsers
            )
            is UiState.Success -> UserListContent(
                users = state.data,
                openReportCount = openReportCount,
                onUserSelected = onUserSelected,
                onManageAlerts = onManageAlerts,
                onViewReports = onViewReports,
                contentPadding = paddingValues,
                showDashboard = !compactMode
            )
        }
    }
}

@Composable
internal fun UserListContent(
    users: List<Utente>,
    openReportCount: Int?,
    onUserSelected: (Utente) -> Unit,
    onManageAlerts: () -> Unit,
    onViewReports: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    showDashboard: Boolean = true
) {
    var searchText by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(AdminUserFilter.ALL) }
    val useLargeTextLayout = LocalDensity.current.fontScale >= 1.3f
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
            bottom = contentPadding.calculateBottomPadding() + 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showDashboard) {
            item {
                Text("Da gestire", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Apri una priorità o restringi subito l’elenco utenti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        TriageMetric(
                            requestCount.toString(), "Richieste cambio", "Mostra gli utenti",
                            { Icon(Icons.Default.Info, contentDescription = null) }
                        ) { selectedFilter = AdminUserFilter.CHANGE_REQUESTED }
                    }
                    item {
                        TriageMetric(
                            expiredCount.toString(), "Schede da creare", "Scadute o mancanti",
                            { Icon(Icons.Default.Warning, contentDescription = null) }
                        ) { selectedFilter = AdminUserFilter.EXPIRED_OR_MISSING }
                    }
                    item {
                        TriageMetric(
                            openReportCount?.toString() ?: "—", "Segnalazioni aperte", "Apri segnalazioni",
                            { Icon(Icons.Default.Warning, contentDescription = null) }, onViewReports
                        )
                    }
                }
            }
            item {
                if (useLargeTextLayout) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminNavigationButton("Segnalazioni", Icons.Default.Warning, onViewReports)
                        AdminNavigationButton("Avvisi", Icons.Default.Notifications, onManageAlerts)
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AdminNavigationButton("Segnalazioni", Icons.Default.Warning, onViewReports, Modifier.weight(1f))
                        AdminNavigationButton("Avvisi", Icons.Default.Notifications, onManageAlerts, Modifier.weight(1f))
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text("Utenti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selectedFilter == AdminUserFilter.ALL,
                        { selectedFilter = AdminUserFilter.ALL },
                        { Text("Tutti ${users.size}") }
                    )
                }
                item {
                    FilterChip(
                        selectedFilter == AdminUserFilter.EXPIRED_OR_MISSING,
                        { selectedFilter = AdminUserFilter.EXPIRED_OR_MISSING },
                        { Text("Scadute o mancanti $expiredCount") },
                        leadingIcon = if (selectedFilter == AdminUserFilter.EXPIRED_OR_MISSING) {{
                            Icon(Icons.Default.Warning, null, modifier = Modifier.size(18.dp))
                        }} else null
                    )
                }
                item {
                    FilterChip(
                        selectedFilter == AdminUserFilter.CHANGE_REQUESTED,
                        { selectedFilter = AdminUserFilter.CHANGE_REQUESTED },
                        { Text("Richieste $requestCount") },
                        leadingIcon = if (selectedFilter == AdminUserFilter.CHANGE_REQUESTED) {{
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                        }} else null
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (filteredUsers.size == 1) "1 utente" else "${filteredUsers.size} utenti",
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
private fun AdminNavigationButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedButton(onClick = onClick, modifier = modifier) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun TriageMetric(
    count: String,
    label: String,
    supportingText: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(190.dp).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$label: $count. $supportingText" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
                Spacer(Modifier.width(8.dp))
                Text(count, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(supportingText, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    Scaffold(
        topBar = { TopAppBar(title = { Text("Amministrazione") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {},
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Nuovo utente") }
            )
        }
    ) { paddingValues ->
        UserListContent(previewAdminUsers(), 3, {}, {}, {}, paddingValues)
    }
}
