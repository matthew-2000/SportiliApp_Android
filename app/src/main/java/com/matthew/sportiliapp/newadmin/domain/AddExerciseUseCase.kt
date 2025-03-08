package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.Esercizio

class AddExerciseUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String,
        esercizio: Esercizio
    ): Result<Unit> = repository.addExercise(userCode, dayKey, muscleGroupKey, exerciseKey, esercizio)
}
