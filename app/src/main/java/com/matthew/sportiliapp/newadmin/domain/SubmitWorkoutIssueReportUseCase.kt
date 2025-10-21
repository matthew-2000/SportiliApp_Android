package com.matthew.sportiliapp.newadmin.domain

import com.matthew.sportiliapp.model.WorkoutIssueReport

class SubmitWorkoutIssueReportUseCase(private val repository: FirebaseRepository) {
    suspend operator fun invoke(report: WorkoutIssueReport): Result<Unit> =
        repository.addWorkoutIssueReport(report)
}
