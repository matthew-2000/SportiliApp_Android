package com.matthew.sportiliapp.scheda

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun TimerSheet(riposo: String) {
    val initialDuration = remember(riposo) { parseRiposo(riposo) }
    var totalTime by remember(riposo) { mutableIntStateOf(initialDuration) }
    var timeRemaining by remember(riposo) { mutableIntStateOf(initialDuration) }
    var timerIsActive by remember { mutableStateOf(false) }
    var timerPaused by remember { mutableStateOf(false) }
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
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center  // Centra verticalmente il contenuto
    ) {
        Text("Tempo di Recupero", style = MaterialTheme.typography.headlineLarge)
        if (initialDuration > 0) {
            Text(
                text = "Recupero impostato: ${formatTime(initialDuration)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (totalTime > 0) {
            // Animazione circolare che mostra il progresso
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                CircularProgressIndicator(
                    progress = {
                        ((totalTime - timeRemaining).coerceAtLeast(0)).toFloat() / totalTime.toFloat()
                    },
                    strokeWidth = 10.dp,
                    modifier = Modifier.size(200.dp)
                )
                Text(formatTime(timeRemaining), style = MaterialTheme.typography.headlineLarge)
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                        timerIsActive = false
                        timerPaused = false
                    },
                    enabled = timerIsActive || timerPaused || timeRemaining != totalTime,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
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
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = { setDuration(60) }, modifier = Modifier.weight(1f)) {
                    Text("60 sec")
                }
                OutlinedButton(onClick = { setDuration(90) }, modifier = Modifier.weight(1f)) {
                    Text("90 sec")
                }
                OutlinedButton(onClick = { setDuration(120) }, modifier = Modifier.weight(1f)) {
                    Text("120 sec")
                }
            }
        }
    }
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
