package com.matthew.sportiliapp.newadmin

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

class FakeFirebaseRepository : FirebaseRepository {
    val usersFlow = MutableStateFlow<List<Utente>>(emptyList())
    val alertsFlow = MutableStateFlow<List<Avviso>>(emptyList())
    val reportsFlow = MutableStateFlow<List<WorkoutIssueReport>>(emptyList())

    var addAlertResult: Result<Unit> = Result.success(Unit)
    var updateAlertResult: Result<Unit> = Result.success(Unit)
    var removeAlertResult: Result<Unit> = Result.success(Unit)
    var updateReportResult: Result<Unit> = Result.success(Unit)
    var removeReportResult: Result<Unit> = Result.success(Unit)

    override fun getUsers(): Flow<List<Utente>> = usersFlow
    override suspend fun addUser(utente: Utente): Result<Unit> = Result.success(Unit)
    override suspend fun updateUser(utente: Utente): Result<Unit> = Result.success(Unit)
    override suspend fun removeUser(userCode: String): Result<Unit> = Result.success(Unit)
    override suspend fun updateWorkoutCard(userCode: String, scheda: Scheda): Result<Unit> = Result.success(Unit)
    override suspend fun getWorkoutCard(userCode: String): Result<Scheda> = Result.success(Scheda())
    override suspend fun getDay(userCode: String, dayKey: String): Result<Giorno> = Result.success(Giorno())
    override suspend fun addDay(userCode: String, dayKey: String, giorno: Giorno): Result<Unit> = Result.success(Unit)
    override suspend fun updateDay(userCode: String, dayKey: String, giorno: Giorno): Result<Unit> = Result.success(Unit)
    override suspend fun removeDay(userCode: String, dayKey: String): Result<Unit> = Result.success(Unit)
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
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateMuscleGroup(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        gruppo: GruppoMuscolare
    ): Result<Unit> = Result.success(Unit)

    override suspend fun removeMuscleGroup(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String
    ): Result<Unit> = Result.success(Unit)

    override suspend fun addExercise(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String,
        esercizio: Esercizio
    ): Result<Unit> = Result.success(Unit)

    override suspend fun updateExercise(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String,
        esercizio: Esercizio
    ): Result<Unit> = Result.success(Unit)

    override suspend fun removeExercise(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String
    ): Result<Unit> = Result.success(Unit)

    override fun getAlerts(): Flow<List<Avviso>> = alertsFlow
    override suspend fun addAlert(avviso: Avviso): Result<Unit> = addAlertResult
    override suspend fun updateAlert(avviso: Avviso): Result<Unit> = updateAlertResult
    override suspend fun removeAlert(alertId: String): Result<Unit> = removeAlertResult
    override fun getWorkoutIssueReports(): Flow<List<WorkoutIssueReport>> = reportsFlow
    override suspend fun addWorkoutIssueReport(report: WorkoutIssueReport): Result<Unit> = Result.success(Unit)
    override suspend fun updateWorkoutIssueReport(report: WorkoutIssueReport): Result<Unit> = updateReportResult
    override suspend fun removeWorkoutIssueReport(reportId: String): Result<Unit> = removeReportResult
}
