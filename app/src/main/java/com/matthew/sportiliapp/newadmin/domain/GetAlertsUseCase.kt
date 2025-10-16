package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.Avviso
import kotlinx.coroutines.flow.Flow

class GetAlertsUseCase(private val repository: FirebaseRepository) {
    operator fun invoke(): Flow<List<Avviso>> = repository.getAlerts()
}
