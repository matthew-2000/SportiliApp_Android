package com.matthew.sportiliapp.newadmin.domain

class RemoveMuscleGroupUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String
    ): Result<Unit> = repository.removeMuscleGroup(userCode, dayKey, muscleGroupKey)
}
