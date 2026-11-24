package com.matthew.sportiliapp.scheda

import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.delay

@Composable
fun TimerSheet(riposo: String, exerciseName: String? = null) {
    val initialDuration = remember(riposo) { parseRiposo(riposo) }
    var totalTime by remember(riposo) { mutableIntStateOf(initialDuration) }
    var timeRemaining by remember(riposo) { mutableIntStateOf(initialDuration) }
    var timerIsActive by remember { mutableStateOf(false) }
    var timerPaused by remember { mutableStateOf(false) }
    var timerAnnouncement by remember { mutableStateOf("") }
    val context = LocalContext.current
    val setDuration: (Int) -> Unit = { seconds ->
        totalTime = seconds
        timeRemaining = seconds
        timerIsActive = false
        timerPaused = false
    }

    val permission = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { com.matthew.sportiliapp.nativefeatures.RestTimer.refresh(context) }
    LaunchedEffect(Unit) {
        while (true) {
            val end = com.matthew.sportiliapp.nativefeatures.RestTimer.deadline(context)
            if (end > 0) {
                totalTime = com.matthew.sportiliapp.nativefeatures.RestTimer.total(context)
                val next = com.matthew.sportiliapp.nativefeatures.RestTimer.remaining(context)
                if (next != timeRemaining) timerAnnouncement = timerAnnouncementFor(next).orEmpty()
                timeRemaining = next
                timerIsActive = timeRemaining > 0
                if (!timerIsActive) com.matthew.sportiliapp.nativefeatures.RestTimer.finish(context, end)
            } else {
                if (timerIsActive) timeRemaining = totalTime
                timerIsActive = false
            }
            delay(250)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!com.matthew.sportiliapp.nativefeatures.RestTimer.allowed(context)) Text("Notifiche disattivate. Abilitale nelle impostazioni per ricevere l’avviso.")
        if (Build.VERSION.SDK_INT >= 31 && !context.getSystemService(android.app.AlarmManager::class.java).canScheduleExactAlarms()) {
            Text("L’avviso può arrivare in ritardo. Per maggiore precisione abilita Sveglie e promemoria.")
            TextButton(onClick = { context.startActivity(android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, android.net.Uri.parse("package:${context.packageName}"))) }) { Text("Imposta avviso preciso") }
        }
        Text("Timer di recupero", style = MaterialTheme.typography.headlineSmall)
        if (initialDuration > 0) {
            Text(
                text = "Recupero impostato: ${formatTime(initialDuration)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (totalTime > 0) {
            val progress = ((totalTime - timeRemaining).coerceAtLeast(0)).toFloat() / totalTime.toFloat()
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val gaugeSize = minOf(maxWidth, 220.dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(gaugeSize)
                        .align(Alignment.Center)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Tempo rimanente ${formatTime(timeRemaining)}"
                            progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                        }
                ) {
                    CircularProgressIndicator(
                        progress = { progress },
                        strokeWidth = 10.dp,
                        modifier = Modifier.size(gaugeSize)
                    )
                    Text(formatTime(timeRemaining), style = MaterialTheme.typography.headlineLarge)
                }
            }

            Text(
                text = timerAnnouncement,
                modifier = Modifier.semantics {
                    if (timerAnnouncement.isNotEmpty()) liveRegion = LiveRegionMode.Assertive
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (timerIsActive) {
                    Button(
                        onClick = { com.matthew.sportiliapp.nativefeatures.RestTimer.clear(context); timerIsActive = false; timerPaused = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pausa")
                    }
                } else {
                    val startLabel = if (timerPaused) "Riprendi" else "Avvia"
                    Button(
                        onClick = {
                            if (timeRemaining <= 0) timeRemaining = totalTime
                            timerAnnouncement = ""
                            if (Build.VERSION.SDK_INT >= 33) permission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            com.matthew.sportiliapp.nativefeatures.RestTimer.start(context, timeRemaining, totalTime, exerciseName)
                            timerIsActive = true
                            timerPaused = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(startLabel)
                    }
                }

                OutlinedButton(
                    onClick = {
                        com.matthew.sportiliapp.nativefeatures.RestTimer.clear(context)
                        timeRemaining = totalTime
                        timerAnnouncement = ""
                        timerIsActive = false
                        timerPaused = false
                    },
                    enabled = timerIsActive || timerPaused || timeRemaining != totalTime,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reimposta")
                }
            }

            Text(
                text = "Il recupero continua anche chiudendo questa schermata.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Scegli la durata del recupero.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { setDuration(60) }, modifier = Modifier.fillMaxWidth()) {
                    Text("60 sec")
                }
                OutlinedButton(onClick = { setDuration(90) }, modifier = Modifier.fillMaxWidth()) {
                    Text("90 sec")
                }
                OutlinedButton(onClick = { setDuration(120) }, modifier = Modifier.fillMaxWidth()) {
                    Text("120 sec")
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

internal fun timerAnnouncementFor(secondsRemaining: Int): String? = when (secondsRemaining) {
    10 -> "10 secondi rimanenti"
    5 -> "5 secondi rimanenti"
    0 -> "Recupero terminato"
    else -> null
}

// Funzione per parsare il tempo di riposo
fun parseRiposo(riposo: String): Int {
    val normalized = riposo.trim()
    if (normalized.isEmpty()) return 0

    val mmSsMatch = Regex("""^(\d{1,2})\s*:\s*(\d{1,2})$""").find(normalized)
    if (mmSsMatch != null) {
        val minutes = mmSsMatch.groupValues[1].toIntOrNull() ?: return 0
        val seconds = mmSsMatch.groupValues[2].toIntOrNull() ?: return 0
        return (minutes * 60) + seconds
    }

    val quoteMatch = Regex("""^(\d+)\s*'\s*(\d{1,2})?\s*"?$""").find(normalized)
    if (quoteMatch != null) {
        val minutes = quoteMatch.groupValues[1].toIntOrNull() ?: 0
        val seconds = quoteMatch.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
        return (minutes * 60) + seconds
    }

    val plainSeconds = normalized.toIntOrNull()
    if (plainSeconds != null) {
        return plainSeconds
    }

    return 0
}

// Funzione per formattare il tempo in mm:ss
fun formatTime(time: Int): String {
    val minutes = time / 60
    val seconds = time % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Preview(name = "Timer di recupero", showBackground = true, widthDp = 360, heightDp = 620)
@Preview(
    name = "Timer dark font grande",
    showBackground = true,
    widthDp = 360,
    heightDp = 700,
    fontScale = 1.6f,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun TimerSheetPreview() {
    SportiliAppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            TimerSheet(riposo = "1' 30\"")
        }
    }
}
