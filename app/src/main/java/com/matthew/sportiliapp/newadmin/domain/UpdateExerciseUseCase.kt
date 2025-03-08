package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.Esercizio

class UpdateExerciseUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String,
        esercizio: Esercizio
    ): Result<Unit> = repository.updateExercise(userCode, dayKey, muscleGroupKey, exerciseKey, esercizio)
}
