package com.matthew.sportiliapp.newadmin.domain

class RemoveExerciseUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        exerciseKey: String
    ): Result<Unit> = repository.removeExercise(userCode, dayKey, muscleGroupKey, exerciseKey)
}
