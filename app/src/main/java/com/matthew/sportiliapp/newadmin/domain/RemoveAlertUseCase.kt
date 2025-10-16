package com.matthew.sportiliapp.newadmin.domain

class RemoveAlertUseCase(private val repository: FirebaseRepository) {
    suspend operator fun invoke(alertId: String): Result<Unit> = repository.removeAlert(alertId)
}
