package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.Giorno

class AddDayUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(userCode: String, dayKey: String, giorno: Giorno): Result<Unit> =
        repository.addDay(userCode, dayKey, giorno)
}
