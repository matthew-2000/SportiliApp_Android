package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.Scheda
import kotlinx.coroutines.flow.Flow

class GetWorkoutCardUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(userCode: String): Result<Scheda> {
        return repository.getWorkoutCard(userCode)
    }
}
