package com.matthew.sportiliapp.newadmin.domain

class RemoveDayUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(userCode: String, dayKey: String): Result<Unit> =
        repository.removeDay(userCode, dayKey)
}