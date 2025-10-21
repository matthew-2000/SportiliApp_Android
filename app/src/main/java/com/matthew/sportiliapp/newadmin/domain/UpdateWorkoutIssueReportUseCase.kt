package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.WorkoutIssueReport

class UpdateWorkoutIssueReportUseCase(private val repository: FirebaseRepository) {
    suspend operator fun invoke(report: WorkoutIssueReport): Result<Unit> =
        repository.updateWorkoutIssueReport(report)
}
