package com.matthew.sportiliapp.newadmin.domain
import com.matthew.sportiliapp.model.Utente
import kotlinx.coroutines.flow.Flow

class GetUsersUseCase(
    private val repository: FirebaseRepository
) {
    operator fun invoke(): Flow<List<Utente>> = repository.getUsers()
}