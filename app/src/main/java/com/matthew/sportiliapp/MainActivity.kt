package com.matthew.sportiliapp

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SportiliAppTheme {
                val navController = rememberNavController()
                val context = LocalContext.current
                val startDestination = remember { mutableStateOf("login") }

                val auth = FirebaseAuth.getInstance()
                val isLoggedIn = auth.currentUser != null

                val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
                val savedCode = sharedPreferences.getString("code", "") ?: ""

                if (isLoggedIn && savedCode.isNotEmpty()) {
                    startDestination.value = "content"
                } else {
                    startDestination.value = "login"
                }
                AppNavHost(navController = navController, startDestination = startDestination.value)
            }
        }
    }
}