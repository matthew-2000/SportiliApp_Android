package com.matthew.sportiliapp.scheda

import android.content.res.Configuration
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
fun TimerSheet(riposo: String) {
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

    // Aggiorna il timer in tempo reale quando è attivo
    LaunchedEffect(timerIsActive, timeRemaining, totalTime) {
        if (timerIsActive && timeRemaining > 0) {
            delay(1000L)
            timeRemaining -= 1
            timerAnnouncement = timerAnnouncementFor(timeRemaining).orEmpty()
            if (timeRemaining <= 0) {
                timeRemaining = 0
                timerIsActive = false
                timerPaused = false
                playSound(context)
                triggerVibration(context)
            }
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
        Text("Timer recupero", style = MaterialTheme.typography.headlineSmall)
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
                        onClick = { timerIsActive = false; timerPaused = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pausa")
                    }
                } else {
                    val startLabel = if (timerPaused) "Riprendi" else "Inizia"
                    Button(
                        onClick = {
                            if (timeRemaining <= 0) timeRemaining = totalTime
                            timerAnnouncement = ""
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
                        timeRemaining = totalTime
                        timerAnnouncement = ""
                        timerIsActive = false
                        timerPaused = false
                    },
                    enabled = timerIsActive || timerPaused || timeRemaining != totalTime,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }

            Text(
                text = "Suggerimento: usa il timer ad ogni fine serie per mantenere costante il recupero.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = "Recupero non configurato per questo esercizio.",
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

// Funzione per attivare la vibrazione
fun triggerVibration(context: Context) {
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(VibratorManager::class.java)
        vibratorManager?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    }
    if (vibrator == null) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(500)
    }
}

// Funzione per riprodurre il suono
fun playSound(context: Context) {
    val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    val ringtone = RingtoneManager.getRingtone(context, sound)
    ringtone.play()
}

@Preview(name = "Timer recupero", showBackground = true, widthDp = 360, heightDp = 620)
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
