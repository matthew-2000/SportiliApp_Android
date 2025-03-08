package com.matthew.sportiliapp.newadmin.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.ui.screens.*
import com.matthew.sportiliapp.newadmin.ui.screens.UserListScreen
import com.matthew.sportiliapp.newadmin.ui.viewmodel.*
import com.matthew.sportiliapp.newadmin.ui.viewmodel.DayUiState

@Composable
fun AdminNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.UserList.route) {
        composable(Screen.UserList.route) {
            UserListScreen(
                onUserSelected = { user -> navController.navigate(Screen.EditUser.createRoute(user.code)) },
                onAddUser = { navController.navigate(Screen.EditUser.createRoute("new")) }
            )
        }
        composable(Screen.EditUser.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val gymAdminViewModel: GymAdminViewModel = viewModel(
                factory = GymAdminViewModelFactory(
                    ManualInjection.getUsersUseCase,
                    ManualInjection.addUserUseCase,
                    ManualInjection.updateUserUseCase,
                    ManualInjection.removeUserUseCase
                )
            )
            val userState = gymAdminViewModel.usersState.collectAsState().value
            if (userCode == "new") {
                EditUserScreen(
                    initialUser = null,
                    onSave = { user ->
                        gymAdminViewModel.addUser(user) { result ->
                            if (result.isSuccess) {
                                navController.navigate(Screen.EditWorkoutCard.createRoute(user.code))
                            }
                        }
                    },
                    onCancel = { navController.popBackStack() },
                    onEditWorkoutCard = { }
                )
            } else {
                when (userState) {
                    is UiState.Loading -> { Text(text = "Loading User...") }
                    is UiState.Error -> { Text(text = "Error: ${userState.exception.localizedMessage}") }
                    is UiState.Success -> {
                        val initialUser = userState.data.find { it.code == userCode }
                        if (initialUser == null) {
                            Text(text = "User not found")
                        } else {
                            EditUserScreen(
                                initialUser = initialUser,
                                onSave = { user ->
                                    gymAdminViewModel.updateUser(user) { result ->
                                        if (result.isSuccess) {
                                            navController.navigate(Screen.EditWorkoutCard.createRoute(user.code))
                                        }
                                    }
                                },
                                onRemove = {
                                    gymAdminViewModel.removeUser(initialUser.code) { result ->
                                        if (result.isSuccess) {
                                            navController.popBackStack()
                                        }
                                    }
                                },
                                onCancel = { navController.popBackStack() },
                                onEditWorkoutCard = { code ->
                                    navController.navigate(Screen.EditWorkoutCard.createRoute(code))
                                }
                            )
                        }
                    }
                }
            }
        }
        composable(Screen.EditWorkoutCard.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val workoutCardViewModel: WorkoutCardViewModel = viewModel(factory = WorkoutCardViewModelFactory(userCode))
            workoutCardViewModel.loadWorkoutCard(userCode)
            when (val state = workoutCardViewModel.state.collectAsState().value) {
                is WorkoutCardUiState.Loading -> { Text(text = "Loading Workout Card...") }
                is WorkoutCardUiState.Error -> { Text(text = "Error: ${state.error.localizedMessage}") }
                is WorkoutCardUiState.Success -> {
                    EditWorkoutCardScreen(
                        scheda = state.scheda,
                        onDaySelected = { dayKey, day -> navController.navigate(Screen.EditDay.createRoute(userCode, dayKey)) },
                        onSave = { updatedScheda ->
                            workoutCardViewModel.updateWorkoutCard(userCode, updatedScheda)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                WorkoutCardUiState.Idle -> {}
            }
        }
        composable(Screen.EditDay.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val dayKey = backStackEntry.arguments?.getString("dayKey") ?: ""
            val dayViewModel: DayViewModel = viewModel(factory = DayViewModelFactory(userCode, dayKey))
            dayViewModel.loadDay(userCode, dayKey)
            when (val state = dayViewModel.state.collectAsState().value) {
                is DayUiState.Loading -> { Text(text = "Loading Day...") }
                is DayUiState.Error -> { Text(text = "Error: ${state.exception.localizedMessage}") }
                is DayUiState.Success -> {
                    EditDayScreen(
                        dayKey = dayKey,
                        day = state.day,
                        onMuscleGroupSelected = { groupKey, muscleGroup ->
                            navController.navigate(Screen.EditMuscleGroup.createRoute(userCode, dayKey, groupKey))
                        },
                        onSave = { updatedDay ->
                            dayViewModel.updateDay(userCode, dayKey, updatedDay)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                DayUiState.Idle -> {}
            }
        }
        composable(Screen.EditMuscleGroup.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val dayKey = backStackEntry.arguments?.getString("dayKey") ?: ""
            val groupKey = backStackEntry.arguments?.getString("groupKey") ?: ""
            val muscleGroupViewModel: MuscleGroupViewModel = viewModel(factory = MuscleGroupViewModelFactory(userCode, dayKey, groupKey))
            when (val state = muscleGroupViewModel.state.collectAsState().value) {
                is MuscleGroupUiState.Loading -> { Text(text = "Loading Muscle Group...") }
                is MuscleGroupUiState.Error -> { Text(text = "Error: ${state.exception.localizedMessage}") }
                is MuscleGroupUiState.Success -> {
                    EditMuscleGroupScreen(
                        userCode = userCode,
                        dayKey = dayKey,
                        groupKey = groupKey,
                        onSave = { updatedGroup ->
                            muscleGroupViewModel.updateMuscleGroup(userCode, dayKey, groupKey, updatedGroup)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                MuscleGroupUiState.Idle -> {
                    EditMuscleGroupScreen(
                        userCode = userCode,
                        dayKey = dayKey,
                        groupKey = groupKey,
                        onSave = { updatedGroup ->
                            muscleGroupViewModel.addMuscleGroup(userCode, dayKey, groupKey, updatedGroup)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
            }
        }
        composable(Screen.EditExercise.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val dayKey = backStackEntry.arguments?.getString("dayKey") ?: ""
            val groupKey = backStackEntry.arguments?.getString("groupKey") ?: ""
            val exerciseKey = backStackEntry.arguments?.getString("exerciseKey") ?: ""
            val exerciseViewModel: ExerciseViewModel = viewModel(factory = ExerciseViewModelFactory(userCode, dayKey, groupKey, exerciseKey))
            when (val state = exerciseViewModel.state.collectAsState().value) {
                is ExerciseUiState.Loading -> { Text(text = "Loading Exercise...") }
                is ExerciseUiState.Error -> { Text(text = "Error: ${state.exception.localizedMessage}") }
                is ExerciseUiState.Success -> {
                    EditExerciseScreen(
                        userCode = userCode,
                        dayKey = dayKey,
                        groupKey = groupKey,
                        exerciseKey = exerciseKey,
                        onSave = { updatedExercise ->
                            exerciseViewModel.updateExercise(userCode, dayKey, groupKey, exerciseKey, updatedExercise)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
                ExerciseUiState.Idle -> {
                    EditExerciseScreen(
                        userCode = userCode,
                        dayKey = dayKey,
                        groupKey = groupKey,
                        exerciseKey = exerciseKey,
                        onSave = { updatedExercise ->
                            exerciseViewModel.addExercise(userCode, dayKey, groupKey, exerciseKey, updatedExercise)
                            navController.popBackStack()
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}