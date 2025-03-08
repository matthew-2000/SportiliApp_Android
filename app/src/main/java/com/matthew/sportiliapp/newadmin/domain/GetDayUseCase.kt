package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.Giorno
import kotlinx.coroutines.flow.Flow

class GetDayUseCase(
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(userCode: String, dayKey: String): Flow<Giorno> {
        return repository.getDay(userCode, dayKey)
    }
}
