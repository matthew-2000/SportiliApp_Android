package com.matthew.sportiliapp.newadmin.domain
import com.matthew.sportiliapp.model.Scheda

class UpdateWorkoutCardUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(userCode: String, scheda: Scheda): Result<Unit> =
        repository.updateWorkoutCard(userCode, scheda)
}
