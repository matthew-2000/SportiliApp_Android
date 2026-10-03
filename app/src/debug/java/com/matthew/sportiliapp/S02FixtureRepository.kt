package com.matthew.sportiliapp

import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.model.WorkoutIssueReport
import com.matthew.sportiliapp.newadmin.domain.FirebaseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class S02FixtureRepository : FirebaseRepository {
    val usersFlow = MutableStateFlow<List<Utente>>(emptyList())
    val alertsFlow = MutableStateFlow<List<Avviso>>(emptyList())
    val reportsFlow = MutableStateFlow<List<WorkoutIssueReport>>(emptyList())

    var alertWrites = 0
    var saveAlert: suspend (Avviso) -> Result<Unit> = { Result.success(Unit) }

    override fun getUsers(): Flow<List<Utente>> = usersFlow
    override suspend fun addUser(utente: Utente): Result<Unit> = error("Unused fixture operation")
    override suspend fun updateUser(utente: Utente): Result<Unit> = error("Unused fixture operation")
    override suspend fun removeUser(userCode: String): Result<Unit> = error("Unused fixture operation")
    override suspend fun updateWorkoutCard(userCode: String, scheda: Scheda): Result<Unit> = error("Unused fixture operation")
    override suspend fun getWorkoutCard(userCode: String): Result<Scheda> = Result.success(Scheda())
    override suspend fun getDay(userCode: String, dayKey: String): Result<Giorno> = Result.success(Giorno())
    override suspend fun addDay(userCode: String, dayKey: String, giorno: Giorno): Result<Unit> = error("Unused fixture operation")
    override suspend fun updateDay(userCode: String, dayKey: String, giorno: Giorno): Result<Unit> = error("Unused fixture operation")
    override suspend fun removeDay(userCode: String, dayKey: String): Result<Unit> = error("Unused fixture operation")
    override suspend fun getMuscleGroup(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String
    ): Result<GruppoMuscolare> = Result.success(GruppoMuscolare())

    override suspend fun addMuscleGroup(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        gruppo: GruppoMuscolare
    ): Result<Unit> = error("Unused fixture operation")

    override suspend fun updateMuscleGroup(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        gruppo: GruppoMuscolare
    ): Result<Unit> = error("Unused fixture operation")

    override suspend fun removeMuscleGroup(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String
    ): Result<Unit> = error("Unused fixture operation")

    override suspend fun addExercise(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String,
        esercizio: Esercizio
    ): Result<Unit> = error("Unused fixture operation")

    override suspend fun updateExercise(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String,
        esercizio: Esercizio
    ): Result<Unit> = error("Unused fixture operation")

    override suspend fun removeExercise(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String
    ): Result<Unit> = error("Unused fixture operation")

    override fun getAlerts(): Flow<List<Avviso>> = alertsFlow
    override suspend fun addAlert(avviso: Avviso): Result<Unit> = writeAlert(avviso)
    override suspend fun updateAlert(avviso: Avviso): Result<Unit> = writeAlert(avviso)
    private suspend fun writeAlert(alert: Avviso): Result<Unit> {
        alertWrites++
        val result = saveAlert(alert)
        if (result.isSuccess) {
            val saved = alert.copy(id = alert.id.ifBlank { "fixture-alert" })
            alertsFlow.value = alertsFlow.value.filterNot { it.id == saved.id } + saved
        }
        return result
    }
    override suspend fun removeAlert(alertId: String): Result<Unit> = error("Unused fixture operation")
    override fun getWorkoutIssueReports(): Flow<List<WorkoutIssueReport>> = reportsFlow
    override suspend fun addWorkoutIssueReport(report: WorkoutIssueReport): Result<Unit> = error("Unused fixture operation")
    override suspend fun updateWorkoutIssueReport(report: WorkoutIssueReport): Result<Unit> = run {
        reportsFlow.value = reportsFlow.value.map { if (it.id == report.id) report else it }
        Result.success(Unit)
    }
    override suspend fun removeWorkoutIssueReport(reportId: String): Result<Unit> = run {
        reportsFlow.value = reportsFlow.value.filterNot { it.id == reportId }
        Result.success(Unit)
    }
}
