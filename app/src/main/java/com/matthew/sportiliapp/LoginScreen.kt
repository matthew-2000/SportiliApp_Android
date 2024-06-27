package com.matthew.sportiliapp

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.matthew.sportiliapp.R

@Composable
fun LoginScreen(navController: NavHostController) {
    var code by remember { mutableStateOf("") }
    var showAlert by remember { mutableStateOf(false) }
    var alertMessage by remember { mutableStateOf("") }

    val context = LocalContext.current  // Ottieni il contesto dal LocalContext
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.frame_1),
                contentDescription = null,
                modifier = Modifier.size(200.dp)
            )
            Text(
                text = "SportiliApp",
                style = MaterialTheme.typography.headlineLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Codice") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    if (code.isEmpty()) {
                        alertMessage = "Inserisci il codice!"
                        showAlert = true
                    } else {
                        coroutineScope.launch {
                            try {
                                register(
                                    codice = code,
                                    navController = navController,
                                    context = context,
                                    onError = { message ->
                                        alertMessage = message
                                        showAlert = true
                                    }
                                )
                            } catch (e: Exception) {
                                Log.e("AIUTO", "OOOOO")
                                alertMessage = "Errore inaspettato: ${e.message}"
                                showAlert = true
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Entra",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = {
                // Azione per gestire il caso in cui l'utente non ha il codice
            }) {
                Text(
                    text = "Hai bisogno di aiuto?",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = { Text("Attenzione") },
            text = { Text(alertMessage) },
            confirmButton = {
                Button(onClick = { showAlert = false }) {
                    Text("OK")
                }
            }
        )
    }
}

private suspend fun register(
    codice: String,
    context: Context, // Aggiunto il parametro del contesto
    navController: NavHostController,
    onError: (String) -> Unit
) {
    try {
        Log.e("AIUTO", "OOOOO")
        val db = FirebaseDatabase.getInstance().getReference("users")
        val snapshot = db.get().await()
        val authUsers = snapshot.value as? Map<String, Map<String, Any>>

        if (authUsers != null && authUsers[codice] != null) {
            FirebaseAuth.getInstance().signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    val profileUpdates = userProfileChangeRequest {
                        displayName = authUsers[codice]?.get("nome") as? String
                    }
                    user?.updateProfile(profileUpdates)
                    salvaCodeInSharedPreferences(context, code = codice)
                    navController.navigate("content")
                } else {
                    // Errore durante l'accesso
                    onError("Errore durante l'accesso. Riprova più tardi.")
                }
            }.await() // Aggiunto await per attendere il completamento dell'operazione
        } else {
            // Codice non autorizzato
            onError("Codice non autorizzato.")
        }
    } catch (e: Exception) {
        e.message?.let { Log.e("AIUTO", it) }
        onError("Errore durante il processo di registrazione: ${e.message}")
    }
}

fun salvaCodeInSharedPreferences(context: Context, code: String) {
    val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("code", code)
    editor.apply()
}