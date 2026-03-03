package com.matthew.sportiliapp.newadmin.utils

import android.content.Context
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

object AdminAccessValidator {
    private const val SHARED_PREFS = "shared"
    private const val ADMIN_FLAG_KEY = "isAdmin"
    private const val ADMIN_CODE_KEY = "adminCode"

    suspend fun isAdminCode(code: String): Boolean {
        val normalizedCode = code.trim()
        if (normalizedCode.isEmpty()) return false

        val remoteValue = FirebaseDatabase.getInstance()
            .getReference("fausto")
            .get()
            .await()
            .value
            ?.toString()
            ?.trim()

        return normalizedCode == remoteValue
    }

    suspend fun revalidateStoredAdminAccess(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        val hasAdminFlag = sharedPreferences.getBoolean(ADMIN_FLAG_KEY, false)
        if (!hasAdminFlag) return false

        val savedAdminCode = sharedPreferences.getString(ADMIN_CODE_KEY, "")?.trim().orEmpty()
        if (savedAdminCode.isEmpty()) {
            clearAdminSession(context)
            return false
        }

        return try {
            val isValid = isAdminCode(savedAdminCode)
            if (!isValid) {
                clearAdminSession(context)
            }
            isValid
        } catch (_: Exception) {
            clearAdminSession(context)
            false
        }
    }

    fun saveAdminSession(context: Context, adminCode: String) {
        context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ADMIN_FLAG_KEY, true)
            .putString(ADMIN_CODE_KEY, adminCode.trim())
            .apply()
    }

    fun clearAdminSession(context: Context) {
        context.getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(ADMIN_FLAG_KEY, false)
            .remove(ADMIN_CODE_KEY)
            .apply()
    }
}
