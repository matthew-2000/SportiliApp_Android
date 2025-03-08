package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.GruppoMuscolare

class AddMuscleGroupUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        gruppo: GruppoMuscolare
    ): Result<Unit> = repository.addMuscleGroup(userCode, dayKey, muscleGroupKey, gruppo)
}