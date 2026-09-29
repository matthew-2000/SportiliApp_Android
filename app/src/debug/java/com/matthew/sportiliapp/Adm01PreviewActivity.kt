package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.matthew.sportiliapp.newadmin.ui.screens.AdminUserListPreviewScreen
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

class Adm01PreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val useDarkTheme = intent.getBooleanExtra("dark", false)
        setContent {
            SportiliAppTheme(isDarkTheme = useDarkTheme) {
                AdminUserListPreviewScreen()
            }
        }
    }
}
