package com.matthew.sportiliapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.launch

private data class ExternalLinkItem(
    val label: String,
    val action: String,
    val url: String,
    val emphasized: Boolean = false
)

private val settingsLinks = listOf(
    ExternalLinkItem("Instagram", "Seguici su Instagram", "https://www.instagram.com/sportiliacentrofitness"),
    ExternalLinkItem("Facebook", "Seguici su Facebook", "https://www.facebook.com/centrofitness.sportilia"),
    ExternalLinkItem("TikTok", "Seguici su TikTok", "https://www.tiktok.com/@palestrasportilia"),
    ExternalLinkItem(
        label = "Sito web",
        action = "Visita il sito web",
        url = "https://www.palestrasportilia.it",
        emphasized = true
    )
)

@Composable
fun ImpostazioniScreen(navController: NavHostController) {
    val context = LocalContext.current
    ImpostazioniContent(
        versionLabel = appVersionLabel(context),
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
    versionLabel: String,
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
                SettingsSectionCard(title = "Centro sportivo") {
                    Text(
                        text = "Palestra Sportilia",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Via Valle, 22\n83024 Monteforte Irpino (Avellino)\nCell. 338 7731977",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                SettingsSectionCard(title = "Social") {
                    settingsLinks.forEachIndexed { index, link ->
                        SettingsLinkButton(
                            link = link,
                            onClick = {
                                if (!onOpenLink(link.url)) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "Impossibile aprire ${link.label}. Riprova più tardi."
                                        )
                                    }
                                }
                            }
                        )
                        if (index != settingsLinks.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Account") {
                    Text(
                        text = "Esci dall’app su questo dispositivo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Esci")
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Informazioni") {
                    Text(
                        text = versionLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    Text(
                        text = "Realizzata con cura da Matteo Ercolino",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
private fun SettingsLinkButton(link: ExternalLinkItem, onClick: () -> Unit) {
    if (link.emphasized) {
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(link.action)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(link.action)
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
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
private fun appVersionLabel(context: Context): String {
    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
    val buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        packageInfo.versionCode.toLong()
    }
    return "Versione ${packageInfo.versionName.orEmpty()} ($buildNumber)"
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
            versionLabel = "Versione 1.3.5 (31)",
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
            versionLabel = "Versione 1.3.5 (31)",
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
            versionLabel = "Versione 1.3.5 (31)",
            onOpenLink = { false },
            onLogout = {}
        )
    }
}
