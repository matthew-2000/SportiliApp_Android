package com.matthew.sportiliapp

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.matthew.sportiliapp.newadmin.utils.AdminAccessValidator
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val LOGIN_TAG = "LoginScreen"
private const val LOGIN_GENERIC_ERROR =
    "Non riusciamo a verificare il codice. Controlla la connessione e riprova."

@Composable
fun LoginScreen(navController: NavHostController) {
    var code by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }
    var showCodeInfo by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submit() {
        if (isSubmitting) return
        if (code.isBlank()) {
            codeError = "Inserisci il codice di accesso."
            return
        }

        codeError = null
        focusManager.clearFocus()
        keyboardController?.hide()
        coroutineScope.launch {
            isSubmitting = true
            try {
                register(
                    codice = code,
                    navController = navController,
                    context = context,
                    onError = { message -> codeError = message }
                )
            } catch (exception: Exception) {
                Log.e(LOGIN_TAG, "Unexpected login failure", exception)
                codeError = LOGIN_GENERIC_ERROR
            } finally {
                isSubmitting = false
            }
        }
    }

    LoginContent(
        code = code,
        codeError = codeError,
        isSubmitting = isSubmitting,
        onCodeChange = {
            code = it
            if (codeError != null) codeError = null
        },
        onSubmit = ::submit,
        onShowCodeInfo = { showCodeInfo = true }
    )

    if (showCodeInfo) {
        AlertDialog(
            onDismissRequest = { showCodeInfo = false },
            title = { Text("Codice di accesso") },
            text = {
                Text("Il codice viene fornito dal tuo trainer. Contattalo se non lo hai ancora ricevuto.")
            },
            confirmButton = {
                TextButton(onClick = { showCodeInfo = false }) {
                    Text("Ho capito")
                }
            }
        )
    }
}

@Composable
private fun LoginContent(
    code: String,
    codeError: String?,
    isSubmitting: Boolean,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onShowCodeInfo: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Image(
                    painter = painterResource(id = R.drawable.icon),
                    contentDescription = "Logo SportiliApp",
                    modifier = Modifier.size(112.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "SportiliApp",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Inserisci il codice ricevuto dal tuo trainer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    label = { Text("Codice di accesso") },
                    supportingText = {
                        Text(codeError ?: "Puoi usare lettere e numeri.")
                    },
                    isError = codeError != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { onSubmit() }),
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = if (isSubmitting) "Accesso in corso…" else "Accedi",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onShowCodeInfo,
                    enabled = !isSubmitting,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "Non hai il codice?",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private suspend fun register(
    codice: String,
    context: Context,
    navController: NavHostController,
    onError: (String) -> Unit
) {
    try {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }

        val db = FirebaseDatabase.getInstance().getReference("users")
        val normalizedCode = codice.trim()
        val userSnapshot = db.child(normalizedCode).get().await()

        val isAdmin = AdminAccessValidator.isAdminCode(normalizedCode)
        if (isAdmin) {
            salvaCodeInSharedPreferences(context, "")
            AdminAccessValidator.saveAdminSession(context, normalizedCode)
        } else {
            AdminAccessValidator.clearAdminSession(context)
        }

        if (isAdmin) {
            navController.navigate("admin") {
                popUpTo("login") { inclusive = true }
                launchSingleTop = true
            }
        } else if (userSnapshot.exists()) {
            val userName = userSnapshot.child("nome").getValue(String::class.java)
            val profileUpdates = userProfileChangeRequest {
                displayName = userName
            }
            auth.currentUser?.updateProfile(profileUpdates)?.await()
            salvaCodeInSharedPreferences(context, code = normalizedCode)
            navController.navigate("content") {
                popUpTo("login") { inclusive = true }
                launchSingleTop = true
            }
        } else {
            onError("Il codice non è valido. Controllalo e riprova.")
        }
    } catch (exception: Exception) {
        Log.e(LOGIN_TAG, "Login registration failed", exception)
        onError(LOGIN_GENERIC_ERROR)
    }
}

fun salvaCodeInSharedPreferences(context: Context, code: String) {
    val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
    val editor = sharedPreferences.edit()
    editor.putString("code", code)
    editor.apply()
}

@Preview(name = "Login light", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun LoginLightPreview() {
    SportiliAppTheme(isDarkTheme = false) {
        LoginContent(
            code = "",
            codeError = null,
            isSubmitting = false,
            onCodeChange = {},
            onSubmit = {},
            onShowCodeInfo = {}
        )
    }
}

@Preview(
    name = "Login dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 640,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun LoginDarkPreview() {
    SportiliAppTheme(isDarkTheme = true) {
        LoginContent(
            code = "SPORTILI24",
            codeError = null,
            isSubmitting = false,
            onCodeChange = {},
            onSubmit = {},
            onShowCodeInfo = {}
        )
    }
}

@Preview(
    name = "Login font grande ed errore",
    showBackground = true,
    widthDp = 320,
    heightDp = 640,
    fontScale = 1.6f
)
@Composable
private fun LoginLargeFontPreview() {
    SportiliAppTheme(isDarkTheme = false) {
        LoginContent(
            code = "",
            codeError = "Inserisci il codice di accesso.",
            isSubmitting = false,
            onCodeChange = {},
            onSubmit = {},
            onShowCodeInfo = {}
        )
    }
}
