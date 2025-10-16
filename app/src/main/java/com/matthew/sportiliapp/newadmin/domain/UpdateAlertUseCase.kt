package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.Avviso

class UpdateAlertUseCase(private val repository: FirebaseRepository) {
    suspend operator fun invoke(avviso: Avviso): Result<Unit> = repository.updateAlert(avviso)
}
