package com.matthew.sportiliapp.newadmin.di

import com.google.firebase.database.FirebaseDatabase
import com.matthew.sportiliapp.newadmin.domain.*
import newadmin.data.FirebaseRepositoryImpl

object ManualInjection {
    private val firebaseDatabase: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    private val defaultRepository: FirebaseRepository by lazy { FirebaseRepositoryImpl(firebaseDatabase) }
    private var repositoryOverride: FirebaseRepository? = null

    val firebaseRepository: FirebaseRepository
        get() = repositoryOverride ?: defaultRepository
    val getUsersUseCase: GetUsersUseCase
        get() = GetUsersUseCase(firebaseRepository)
    val addUserUseCase: AddUserUseCase
        get() = AddUserUseCase(firebaseRepository)
    val updateUserUseCase: UpdateUserUseCase
        get() = UpdateUserUseCase(firebaseRepository)
    val removeUserUseCase: RemoveUserUseCase
        get() = RemoveUserUseCase(firebaseRepository)
    val updateWorkoutCardUseCase: UpdateWorkoutCardUseCase
        get() = UpdateWorkoutCardUseCase(firebaseRepository)
    val getWorkoutCardUseCase: GetWorkoutCardUseCase
        get() = GetWorkoutCardUseCase(firebaseRepository)
    val getDayUseCase: GetDayUseCase
        get() = GetDayUseCase(firebaseRepository)
    val updateDayUseCase: UpdateDayUseCase
        get() = UpdateDayUseCase(firebaseRepository)
    val getMuscleGroupUseCase: GetMuscleGroupUseCase
        get() = GetMuscleGroupUseCase(firebaseRepository)
    val addMuscleGroupUseCase: AddMuscleGroupUseCase
        get() = AddMuscleGroupUseCase(firebaseRepository)
    val updateMuscleGroupUseCase: UpdateMuscleGroupUseCase
        get() = UpdateMuscleGroupUseCase(firebaseRepository)
    val removeMuscleGroupUseCase: RemoveMuscleGroupUseCase
        get() = RemoveMuscleGroupUseCase(firebaseRepository)
    val addExerciseUseCase: AddExerciseUseCase
        get() = AddExerciseUseCase(firebaseRepository)
    val updateExerciseUseCase: UpdateExerciseUseCase
        get() = UpdateExerciseUseCase(firebaseRepository)
    val removeExerciseUseCase: RemoveExerciseUseCase
        get() = RemoveExerciseUseCase(firebaseRepository)
    val getAlertsUseCase: GetAlertsUseCase
        get() = GetAlertsUseCase(firebaseRepository)
    val addAlertUseCase: AddAlertUseCase
        get() = AddAlertUseCase(firebaseRepository)
    val updateAlertUseCase: UpdateAlertUseCase
        get() = UpdateAlertUseCase(firebaseRepository)
    val removeAlertUseCase: RemoveAlertUseCase
        get() = RemoveAlertUseCase(firebaseRepository)
    val getWorkoutIssueReportsUseCase: GetWorkoutIssueReportsUseCase
        get() = GetWorkoutIssueReportsUseCase(firebaseRepository)
    val submitWorkoutIssueReportUseCase: SubmitWorkoutIssueReportUseCase
        get() = SubmitWorkoutIssueReportUseCase(firebaseRepository)
    val updateWorkoutIssueReportUseCase: UpdateWorkoutIssueReportUseCase
        get() = UpdateWorkoutIssueReportUseCase(firebaseRepository)
    val removeWorkoutIssueReportUseCase: RemoveWorkoutIssueReportUseCase
        get() = RemoveWorkoutIssueReportUseCase(firebaseRepository)

    fun overrideRepositoryForTesting(repository: FirebaseRepository) {
        repositoryOverride = repository
    }

    fun resetOverrides() {
        repositoryOverride = null
    }
}
