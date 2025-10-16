package com.matthew.sportiliapp.newadmin.di

import com.google.firebase.database.FirebaseDatabase
import com.matthew.sportiliapp.newadmin.domain.*
import newadmin.data.FirebaseRepositoryImpl

object ManualInjection {
    private val firebaseDatabase: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
    val firebaseRepository: FirebaseRepository by lazy { FirebaseRepositoryImpl(firebaseDatabase) }
    val getUsersUseCase: GetUsersUseCase by lazy { GetUsersUseCase(firebaseRepository) }
    val addUserUseCase: AddUserUseCase by lazy { AddUserUseCase(firebaseRepository) }
    val updateUserUseCase: UpdateUserUseCase by lazy { UpdateUserUseCase(firebaseRepository) }
    val removeUserUseCase: RemoveUserUseCase by lazy { RemoveUserUseCase(firebaseRepository) }
    val updateWorkoutCardUseCase: UpdateWorkoutCardUseCase by lazy { UpdateWorkoutCardUseCase(firebaseRepository) }
    val getWorkoutCardUseCase: GetWorkoutCardUseCase by lazy { GetWorkoutCardUseCase(firebaseRepository) }
    val getDayUseCase: GetDayUseCase by lazy { GetDayUseCase(firebaseRepository) }
    val updateDayUseCase: UpdateDayUseCase by lazy { UpdateDayUseCase(firebaseRepository) }
    val getMuscleGroupUseCase: GetMuscleGroupUseCase by lazy { GetMuscleGroupUseCase(firebaseRepository) }
    val addMuscleGroupUseCase: AddMuscleGroupUseCase by lazy { AddMuscleGroupUseCase(firebaseRepository) }
    val updateMuscleGroupUseCase: UpdateMuscleGroupUseCase by lazy { UpdateMuscleGroupUseCase(firebaseRepository) }
    val removeMuscleGroupUseCase: RemoveMuscleGroupUseCase by lazy { RemoveMuscleGroupUseCase(firebaseRepository) }
    val addExerciseUseCase: AddExerciseUseCase by lazy { AddExerciseUseCase(firebaseRepository) }
    val updateExerciseUseCase: UpdateExerciseUseCase by lazy { UpdateExerciseUseCase(firebaseRepository) }
    val removeExerciseUseCase: RemoveExerciseUseCase by lazy { RemoveExerciseUseCase(firebaseRepository) }
    val getAlertsUseCase: GetAlertsUseCase by lazy { GetAlertsUseCase(firebaseRepository) }
    val addAlertUseCase: AddAlertUseCase by lazy { AddAlertUseCase(firebaseRepository) }
    val updateAlertUseCase: UpdateAlertUseCase by lazy { UpdateAlertUseCase(firebaseRepository) }
    val removeAlertUseCase: RemoveAlertUseCase by lazy { RemoveAlertUseCase(firebaseRepository) }
}
