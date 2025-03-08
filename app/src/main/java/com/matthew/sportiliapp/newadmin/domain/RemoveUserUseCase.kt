package com.matthew.sportiliapp.newadmin.domain

class RemoveUserUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(userCode: String): Result<Unit> = repository.removeUser(userCode)
}