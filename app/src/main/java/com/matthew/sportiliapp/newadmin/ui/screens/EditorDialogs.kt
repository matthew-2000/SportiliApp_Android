package com.matthew.sportiliapp.newadmin.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun UnsavedChangesDialog(
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modifiche non salvate") },
        text = {
            Text(
                text = "Vuoi salvare le modifiche prima di uscire?",
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Salva")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDiscard) {
                Text("Scarta")
            }
        }
    )
}

@Composable
fun AdminEditorSection(
    title: String,
    supportingText: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        supportingText?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminEditorErrorBanner(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onRetry) { Text("Riprova") }
        }
    }
}

@Composable
fun AdminEditorBottomBar(
    isDirty: Boolean,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    saveLabel: String = "Salva",
    canSave: Boolean = true,
    idleStatus: String = "Nessuna modifica",
    pendingStatus: String = "Salvataggio in corso…",
    idleIcon: ImageVector = Icons.Default.CheckCircle,
    onComplete: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val save: () -> Unit = {
        focusManager.clearFocus()
        keyboard?.hide()
        onSave()
    }
    Surface(shadowElevation = 8.dp, tonalElevation = 2.dp) {
        // Keep the form reachable even with the largest system text or the IME.
        // The actions retain their full text and can scroll within the bottom bar.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val stackedActions = LocalDensity.current.fontScale >= 1.8f && maxWidth < 480.dp
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight * 0.4f)
                    .testTag("admin-editor-actions").verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isDirty) Icons.Default.Edit else idleIcon,
                        contentDescription = null,
                        tint = if (isDirty) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            isSaving -> pendingStatus
                            isDirty -> "Modifiche non salvate"
                            else -> idleStatus
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val buttons: @Composable () -> Unit = {
                    OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) { Text("Annulla") }
                    Button(onClick = save, enabled = !isSaving && canSave, modifier = Modifier.fillMaxWidth()) { Text(saveLabel) }
                }
                if (stackedActions) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { buttons() }
                } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        enabled = !isSaving,
                        modifier = Modifier.weight(1f)
                    ) { Text("Annulla") }
                    Button(
                        onClick = save,
                        enabled = !isSaving && canSave,
                        modifier = Modifier.weight(1f)
                    ) { Text(saveLabel) }
                }
                onComplete?.let { complete ->
                    Text("Salva mantiene la richiesta di cambio aperta.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = complete, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                        Text("Salva e segna il cambio come completato")
                    }
                }
            }
        }
    }
}
