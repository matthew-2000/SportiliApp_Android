package com.matthew.sportiliapp.nativefeatures

import android.app.Application
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

class SurfaceApplication : Application() {
    private lateinit var listener: SharedPreferences.OnSharedPreferenceChangeListener
    override fun onCreate() {
        super.onCreate()
        val shared = getSharedPreferences("shared", MODE_PRIVATE)
        val local = getSharedPreferences("surface_session", MODE_PRIVATE)
        fun reconcile() {
            val identity = (FirebaseAuth.getInstance().currentUser?.uid ?: "") + ":" + shared.getString("code", "")
            if (local.getString("identity", null) != identity) {
                local.edit().putString("identity", identity).commit()
                WorkoutSurface.clear(this)
            }
        }
        listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if (key == null || key == "code") reconcile() }
        shared.registerOnSharedPreferenceChangeListener(listener)
        FirebaseAuth.getInstance().addAuthStateListener { reconcile() }
        reconcile()
        RestTimer.restore(this)
    }
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        WorkoutSurface.refresh(this)
    }

}
