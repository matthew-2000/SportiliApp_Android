package com.matthew.sportiliapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.launch

private data class ExternalLinkItem(
    val label: String,
    val url: String
)

private val settingsLinks = listOf(
    ExternalLinkItem("Instagram", "https://www.instagram.com/sportiliacentrofitness"),
    ExternalLinkItem("Facebook", "https://www.facebook.com/centrofitness.sportilia"),
    ExternalLinkItem("TikTok", "https://www.tiktok.com/@palestrasportilia"),
    ExternalLinkItem("Sito web", "https://www.palestrasportilia.it")
)

@Composable
fun ImpostazioniScreen(navController: NavHostController) {
    val context = LocalContext.current
    val (versionName, buildNumber) = appVersionInfo(context)
    ImpostazioniContent(
        versionName = versionName,
        buildNumber = buildNumber,
        onOpenLink = { url -> openExternalLink(context, url) },
        onLogout = {
            FirebaseAuth.getInstance().signOut()
            resetSharedPref(context)
            navController.navigate("login") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImpostazioniContent(
    versionName: String,
    buildNumber: String,
    onOpenLink: (String) -> Boolean,
    onLogout: () -> Unit
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(text = "Impostazioni") },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "Informazioni e contatti") {
                    ListItem(
                        headlineContent = { Text("Palestra Sportilia") },
                        supportingContent = {
                            Text("Via Valle, 22\n83024 Monteforte Irpino (Avellino)\n338 7731977")
                        },
                        leadingContent = {
                            Icon(Icons.Default.Place, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Link esterni") {
                    settingsLinks.forEachIndexed { index, link ->
                        ListItem(
                            headlineContent = { Text(link.label) },
                            trailingContent = {
                                Text("↗", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            colors = ListItemDefaults.colors(
                                headlineColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .clickable {
                                    if (!onOpenLink(link.url)) {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Impossibile aprire ${link.label}. Riprova più tardi."
                                            )
                                        }
                                    }
                                }
                                .semantics {
                                    contentDescription = "${link.label}. Apre fuori dall’app"
                                }
                        )
                        if (index != settingsLinks.lastIndex) HorizontalDivider()
                    }
                }
            }

            item {
                SettingsSection(title = "Sessione") {
                    ListItem(
                        headlineContent = { Text("Esci") },
                        supportingContent = { Text("Esci dall’app su questo dispositivo.") },
                        leadingContent = {
                            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                        },
                        colors = ListItemDefaults.colors(
                            headlineColor = MaterialTheme.colorScheme.error,
                            leadingIconColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.clickable { showLogoutDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = "Versione e build") {
                    ListItem(
                        headlineContent = { Text("Versione") },
                        supportingContent = { Text(versionName) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Build") },
                        supportingContent = { Text(buildNumber) }
                    )
                }
            }

            item {
                Text(
                    "Realizzata con cura da Matteo Ercolino",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLogoutDialog = false
                            onLogout()
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Conferma uscita"
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Esci")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Annulla")
                    }
                },
                title = { Text("Uscire dall’account?") },
                text = { Text("Per accedere di nuovo dovrai inserire il tuo codice.") }
            )
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}

private fun openExternalLink(context: Context, url: String): Boolean = runCatching {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(context.packageManager) == null) {
        return@runCatching false
    }
    context.startActivity(intent)
    true
}.getOrDefault(false)

@Suppress("DEPRECATION")
private fun appVersionInfo(context: Context): Pair<String, String> {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }
    return packageInfo.versionName.orEmpty() to buildNumber.toString()
}

fun resetSharedPref(context: Context) {
    val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
    sharedPreferences.edit().clear().apply()
}

@Preview(name = "Impostazioni", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun ImpostazioniPreview() {
    SportiliAppTheme {
        ImpostazioniContent(
            versionName = "1.3.5",
            buildNumber = "31",
            onOpenLink = { true },
            onLogout = {}
        )
    }
}

@Preview(name = "Impostazioni dark", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun ImpostazioniDarkPreview() {
    SportiliAppTheme(isDarkTheme = true) {
        ImpostazioniContent(
            versionName = "1.3.5",
            buildNumber = "31",
            onOpenLink = { true },
            onLogout = {}
        )
    }
}

@Preview(
    name = "Impostazioni font grandi",
    showBackground = true,
    widthDp = 360,
    heightDp = 760,
    fontScale = 1.6f
)
@Composable
private fun ImpostazioniLargeFontPreview() {
    SportiliAppTheme {
        ImpostazioniContent(
            versionName = "1.3.5",
            buildNumber = "31",
            onOpenLink = { false },
            onLogout = {}
        )
    }
}
