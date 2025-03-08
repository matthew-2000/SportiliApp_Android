package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.GruppoMuscolare

class UpdateMuscleGroupUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String,
        gruppo: GruppoMuscolare
    ): Result<Unit> = repository.updateMuscleGroup(userCode, dayKey, muscleGroupKey, gruppo)
}
