package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun AdminContextAppBar(title: String, context: String, enabled: Boolean, onBack: () -> Unit) {
    Surface(tonalElevation = 2.dp) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val contextHeight = maxHeight * 0.25f
            val titleFirst = LocalDensity.current.fontScale >= 1.8f
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(end = 16.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, enabled = enabled) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                }
                Column(Modifier.weight(1f).heightIn(max = contextHeight)
                    .testTag("admin-editor-context").verticalScroll(rememberScrollState())) {
                    if (titleFirst) Text(title, style = MaterialTheme.typography.titleLarge)
                    Text(context, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!titleFirst) Text(title, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}

/** Removal edits the local draft; persistence still belongs to the editor's existing save callback. */
@Composable
internal fun AdminOrderedItem(
    name: String, summary: String, kind: String, position: Int, total: Int, enabled: Boolean,
    onMoveUp: () -> Unit, onMoveDown: () -> Unit, onRemove: () -> Unit, onEdit: () -> Unit,
    editLabel: String? = null,
    removalSupportingText: String = "La rimozione sarà applicata quando salvi o apri un altro elemento. Prima puoi annullarla uscendo e scartando la bozza."
) {
    var confirmingRemoval by remember(name, position) { mutableStateOf(false) }
    if (confirmingRemoval) {
        AlertDialog(
            onDismissRequest = { confirmingRemoval = false },
            title = { Text("Rimuovi $kind") },
            text = { Text("Rimuovere «$name» dalla bozza? $removalSupportingText") },
            confirmButton = {
                TextButton(enabled = enabled, onClick = { confirmingRemoval = false; onRemove() }) {
                    Text("Rimuovi dalla bozza", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmingRemoval = false }) { Text("Annulla") } }
        )
    }
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Column(Modifier.fillMaxWidth().clickable(enabled = enabled, onClickLabel = "Apri $name", onClick = onEdit)
                .padding(vertical = 8.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodySmall)
                Text("$position di $total" + when {
                    total == 1 -> " · Unico"
                    position == 1 -> " · Primo"
                    position == total -> " · Ultimo"
                    else -> ""
                }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(enabled = enabled && position > 1, onClick = onMoveUp) {
                    Icon(Icons.Default.KeyboardArrowUp, "Sposta $name prima")
                }
                IconButton(enabled = enabled && position < total, onClick = onMoveDown) {
                    Icon(Icons.Default.KeyboardArrowDown, "Sposta $name dopo")
                }
                if (editLabel != null) TextButton(enabled = enabled, onClick = onEdit, modifier = Modifier.weight(1f)) { Text(editLabel) }
                else Spacer(Modifier.weight(1f))
                IconButton(enabled = enabled, onClick = { confirmingRemoval = true },
                    colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, "Rimuovi $name dalla bozza")
                }
            }
        }
    }
}

@Composable
internal fun AdminEditorLoadState(
    title: String, context: String, loading: Boolean, onBack: () -> Unit, onRetry: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        AdminContextAppBar(title, context, true, onBack)
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) CircularProgressIndicator()
            else {
                Text("Impossibile caricare ${when (title) { "Scheda" -> "la scheda"; "Giorno" -> "il giorno"; else -> "il gruppo muscolare" }}",
                    style = MaterialTheme.typography.titleMedium)
                Button(onClick = onRetry) { Text("Riprova") }
            }
        }
    }
}
