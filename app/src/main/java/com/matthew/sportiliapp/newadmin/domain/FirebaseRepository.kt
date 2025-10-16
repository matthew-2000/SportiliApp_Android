package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.model.Utente
import kotlinx.coroutines.flow.Flow
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.Esercizio

interface FirebaseRepository {
    // Gestione utenti
    fun getUsers(): Flow<List<Utente>>
    suspend fun addUser(utente: Utente): Result<Unit>
    suspend fun updateUser(utente: Utente): Result<Unit>
    suspend fun removeUser(userCode: String): Result<Unit>

    // Gestione scheda (Workout Card)
    suspend fun updateWorkoutCard(userCode: String, scheda: Scheda): Result<Unit>
    suspend fun getWorkoutCard(userCode: String): Result<Scheda>

    // Gestione dei giorni (Day)
    suspend fun getDay(userCode: String, dayKey: String): Result<Giorno>
    suspend fun addDay(userCode: String, dayKey: String, giorno: Giorno): Result<Unit>
    suspend fun updateDay(userCode: String, dayKey: String, giorno: Giorno): Result<Unit>
    suspend fun removeDay(userCode: String, dayKey: String): Result<Unit>

    // Gestione dei gruppi muscolari (Muscle Group)
    suspend fun getMuscleGroup(userCode: String, dayKey: String, muscleGroupKey: String): Result<GruppoMuscolare>
    suspend fun addMuscleGroup(userCode: String, dayKey: String, muscleGroupKey: String, gruppo: GruppoMuscolare): Result<Unit>
    suspend fun updateMuscleGroup(userCode: String, dayKey: String, muscleGroupKey: String, gruppo: GruppoMuscolare): Result<Unit>
    suspend fun removeMuscleGroup(userCode: String, dayKey: String, muscleGroupKey: String): Result<Unit>

    // Gestione degli esercizi (Exercise)
    suspend fun addExercise(userCode: String, dayKey: String, muscleGroupKey: String, exerciseKey: String, esercizio: Esercizio): Result<Unit>
    suspend fun updateExercise(userCode: String, dayKey: String, muscleGroupKey: String, exerciseKey: String, esercizio: Esercizio): Result<Unit>
    suspend fun removeExercise(userCode: String, dayKey: String, muscleGroupKey: String, exerciseKey: String): Result<Unit>

    // Gestione avvisi
    fun getAlerts(): Flow<List<Avviso>>
    suspend fun addAlert(avviso: Avviso): Result<Unit>
    suspend fun updateAlert(avviso: Avviso): Result<Unit>
    suspend fun removeAlert(alertId: String): Result<Unit>
}
