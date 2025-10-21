package com.matthew.sportiliapp.newadmin.domain

class RemoveWorkoutIssueReportUseCase(private val repository: FirebaseRepository) {
    suspend operator fun invoke(reportId: String): Result<Unit> =
        repository.removeWorkoutIssueReport(reportId)
}
