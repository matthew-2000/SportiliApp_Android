package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.Utente

class UpdateUserUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(utente: Utente): Result<Unit> = repository.updateUser(utente)
}