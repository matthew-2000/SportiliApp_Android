package com.matthew.sportiliapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.matthew.sportiliapp.newadmin.ui.navigation.AdminNavGraph
import com.matthew.sportiliapp.newadmin.utils.AdminAccessValidator
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

private sealed interface StartupUiState {
    object Loading : StartupUiState
    object Admin : StartupUiState
    data class App(val startDestination: String) : StartupUiState
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SportiliAppTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val startupState by produceState<StartupUiState>(
                    initialValue = StartupUiState.Loading,
                    key1 = context
                ) {
                    val auth = FirebaseAuth.getInstance()
                    val isLoggedIn = auth.currentUser != null
                    val sharedPreferences =
                        context.getSharedPreferences("shared", Context.MODE_PRIVATE)
                    val savedCode = sharedPreferences.getString("code", "") ?: ""
                    val hasAdminSession = sharedPreferences.getBoolean("isAdmin", false)

                    value = if (hasAdminSession &&
                        AdminAccessValidator.revalidateStoredAdminAccess(context)
                    ) {
                        StartupUiState.Admin
                    } else {
                        StartupUiState.App(
                            startDestination = if (isLoggedIn && savedCode.isNotEmpty()) {
                                "content"
                            } else {
                                "login"
                            }
                        )
                    }
                }

                when (val state = startupState) {
                    StartupUiState.Loading -> StartupLoadingScreen()
                    StartupUiState.Admin -> AdminNavGraph(navController = navController)
                    is StartupUiState.App -> {
                        AppNavHost(
                            navController = navController,
                            startDestination = state.startDestination
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StartupLoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
