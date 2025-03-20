package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Utente
import kotlinx.coroutines.flow.Flow

class GetMuscleGroupUseCase (
    private val repository: FirebaseRepository
) {
    suspend operator fun invoke(userCode: String, dayKey: String, groupKey: String): Result<GruppoMuscolare> {
        return repository.getMuscleGroup(userCode, dayKey, groupKey)
    }
}