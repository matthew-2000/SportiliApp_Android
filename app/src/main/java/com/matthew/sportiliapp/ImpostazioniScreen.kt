package com.matthew.sportiliapp
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth

private data class ExternalLinkItem(
    val label: String,
    val url: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpostazioniScreen(navController: NavHostController) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val links = remember {
        listOf(
            ExternalLinkItem("Instagram", "https://www.instagram.com/sportiliacentrofitness"),
            ExternalLinkItem("Facebook", "https://www.facebook.com/centrofitness.sportilia"),
            ExternalLinkItem("TikTok", "https://www.tiktok.com/@palestrasportilia"),
            ExternalLinkItem("Sito web", "https://www.palestrasportilia.it")
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(text = "Impostazioni") },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSectionCard(title = "Centro sportivo") {
                    Text(
                        text = "PALESTRA SPORTILIA",
                        style = MaterialTheme.typography.bodyLarge,
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
                SettingsSectionCard(title = "Seguici online") {
                    links.forEachIndexed { index, link ->
                        val isWebsite = link.label == "Sito web"
                        if (isWebsite) {
                            Button(
                                onClick = { openExternalLink(context, link.url) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Apri ${link.label}" }
                            ) {
                                Text("Visita ${link.label}")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { openExternalLink(context, link.url) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics { contentDescription = "Apri ${link.label}" }
                            ) {
                                Text("Seguici su ${link.label}")
                            }
                        }
                        if (index != links.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Account") {
                    Button(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Effettua logout" }
                    ) {
                        Text("Logout")
                    }
                }
            }

            item {
                SettingsSectionCard(title = "Credits") {
                    Text(
                        text = "Made with ❤️ by Matteo Ercolino",
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
                            FirebaseAuth.getInstance().signOut()
                            resetSharedPref(context)
                            showLogoutDialog = false
                            navController.navigate("login") {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
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
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

private fun openExternalLink(context: Context, url: String) {
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "Impossibile aprire il link", Toast.LENGTH_SHORT).show()
    }
}

fun resetSharedPref(context: Context) {
    val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
    sharedPreferences.edit().clear().apply()
}
