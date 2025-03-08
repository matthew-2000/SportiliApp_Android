package com.matthew.sportiliapp.newadmin.domain
import com.matthew.sportiliapp.model.Utente

class AddUserUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(utente: Utente): Result<Unit> = repository.addUser(utente)
}
