package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.WorkoutIssueReport
import kotlinx.coroutines.flow.Flow

class GetWorkoutIssueReportsUseCase(private val repository: FirebaseRepository) {
    operator fun invoke(): Flow<List<WorkoutIssueReport>> = repository.getWorkoutIssueReports()
}
