package com.matthew.sportiliapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.google.firebase.database.FirebaseDatabase
import com.matthew.sportiliapp.model.*
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.domain.FirebaseRepository
import com.matthew.sportiliapp.newadmin.ui.navigation.AdminNavGraph
import com.matthew.sportiliapp.ui.theme.SportiliAppTheme

/** Real adaptive admin graph. Repository writes are in memory; default SDK stays offline. */
class S11AdminPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseDatabase.getInstance().goOffline()
        ManualInjection.overrideRepositoryForTesting(S11AdminRepository())
        setContent { SportiliAppTheme(isDarkTheme = intent.getBooleanExtra("dark", false)) { AdminNavGraph() } }
    }
    override fun onDestroy() { super.onDestroy(); ManualInjection.resetOverrides() }
}

private class S11AdminRepository(private val base: S02FixtureRepository = S02FixtureRepository()) : FirebaseRepository by base {
    private var card = s08Card()
    init {
        base.usersFlow.value = s07Users()
        base.alertsFlow.value = s06Alerts()
        base.reportsFlow.value = listOf(s02Report)
    }
    override suspend fun addUser(utente: Utente): Result<Unit> { base.usersFlow.value += utente; return Result.success(Unit) }
    override suspend fun updateUser(utente: Utente): Result<Unit> {
        base.usersFlow.value = base.usersFlow.value.map { if (it.code == utente.code) utente else it }; return Result.success(Unit)
    }
    override suspend fun removeUser(userCode: String): Result<Unit> {
        base.usersFlow.value = base.usersFlow.value.filterNot { it.code == userCode }; return Result.success(Unit)
    }
    override suspend fun getWorkoutCard(userCode: String) = Result.success(card)
    override suspend fun updateWorkoutCard(userCode: String, scheda: Scheda): Result<Unit> { card = scheda; return Result.success(Unit) }
    override suspend fun getDay(userCode: String, dayKey: String) = Result.success(card.giorni.getValue(dayKey))
    override suspend fun updateDay(userCode: String, dayKey: String, giorno: Giorno): Result<Unit> {
        card = card.copy(giorni = LinkedHashMap(card.giorni).apply { put(dayKey, giorno) }); return Result.success(Unit)
    }
    override suspend fun getMuscleGroup(userCode: String, dayKey: String, muscleGroupKey: String) =
        Result.success(card.giorni.getValue(dayKey).gruppiMuscolari.getValue(muscleGroupKey))
    override suspend fun updateMuscleGroup(userCode: String, dayKey: String, muscleGroupKey: String, gruppo: GruppoMuscolare): Result<Unit> {
        val day = card.giorni.getValue(dayKey)
        return updateDay(userCode, dayKey, day.copy(gruppiMuscolari = LinkedHashMap(day.gruppiMuscolari).apply { put(muscleGroupKey, gruppo) }))
    }
}
