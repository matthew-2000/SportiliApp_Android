package com.matthew.sportiliapp.newadmin.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.ui.screens.EditDayScreen
import com.matthew.sportiliapp.newadmin.ui.screens.EditMuscleGroupScreen
import com.matthew.sportiliapp.newadmin.ui.screens.EditUserScreen
import com.matthew.sportiliapp.newadmin.ui.screens.EditWorkoutCardScreen
import com.matthew.sportiliapp.newadmin.ui.screens.UserListScreen
import com.matthew.sportiliapp.newadmin.ui.viewmodel.DayUiState
import com.matthew.sportiliapp.newadmin.ui.viewmodel.DayViewModel
import com.matthew.sportiliapp.newadmin.ui.viewmodel.DayViewModelFactory
import com.matthew.sportiliapp.newadmin.ui.viewmodel.GymAdminViewModel
import com.matthew.sportiliapp.newadmin.ui.viewmodel.GymAdminViewModelFactory
import com.matthew.sportiliapp.newadmin.ui.viewmodel.MuscleGroupUiState
import com.matthew.sportiliapp.newadmin.ui.viewmodel.MuscleGroupViewModel
import com.matthew.sportiliapp.newadmin.ui.viewmodel.MuscleGroupViewModelFactory
import com.matthew.sportiliapp.newadmin.ui.viewmodel.UiState
import com.matthew.sportiliapp.newadmin.ui.viewmodel.WorkoutCardUiState
import com.matthew.sportiliapp.newadmin.ui.viewmodel.WorkoutCardViewModel
import com.matthew.sportiliapp.newadmin.ui.viewmodel.WorkoutCardViewModelFactory

@Composable
fun AdminNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.UserList.route,
        modifier = Modifier.fillMaxSize() // Forza il NavHost a occupare tutto lo schermo
    ) {
        composable(Screen.UserList.route) {
            // Forziamo l'allineamento topStart per evitare spostamenti
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                UserListScreen(
                    onUserSelected = { user ->
                        navController.navigate(Screen.EditUser.createRoute(user.code))
                    },
                    onAddUser = {
                        navController.navigate(Screen.EditUser.createRoute("new"))
                    }
                )
            }
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
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
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
                        is UiState.Success -> {
                            val initialUser = userState.data.find { it.code == userCode }
                            if (initialUser == null) {
                                Text(text = "User not found", modifier = Modifier.align(Alignment.TopStart))
                            } else {
                                EditUserScreen(
                                    initialUser = initialUser,
                                    onSave = { user ->
                                        gymAdminViewModel.updateUser(user) { result ->
                                            if (result.isSuccess) {
                                                navController.navigate(Screen.EditWorkoutCard.createRoute(user.code))
                                                navController.popBackStack()
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
                        else -> {
                            // Mostra un contenitore vuoto per mantenere la posizione
                        }
                    }
                }
            }
        }
        composable(Screen.EditWorkoutCard.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val workoutCardViewModel: WorkoutCardViewModel = viewModel(factory = WorkoutCardViewModelFactory(userCode))
            LaunchedEffect(userCode) {
                workoutCardViewModel.loadWorkoutCard(userCode)
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                when (val state = workoutCardViewModel.state.collectAsState().value) {
                    is WorkoutCardUiState.Success -> {
                        EditWorkoutCardScreen(
                            scheda = state.scheda,
                            onDaySelected = { dayKey, day, scheda ->
                                workoutCardViewModel.updateWorkoutCard(userCode, scheda)
                                navController.navigate(Screen.EditDay.createRoute(userCode, dayKey))
                            },
                            onSave = { updatedScheda ->
                                workoutCardViewModel.updateWorkoutCard(userCode, updatedScheda)
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                    is WorkoutCardUiState.Error -> {
                        Text(
                            text = "Error: ${state.error.localizedMessage}",
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                    else -> {
                        // Contenitore vuoto
                    }
                }
            }
        }
        composable(Screen.EditDay.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val dayKey = backStackEntry.arguments?.getString("dayKey") ?: ""
            val dayViewModel: DayViewModel = viewModel(factory = DayViewModelFactory(userCode, dayKey))
            LaunchedEffect(userCode, dayKey) {
                dayViewModel.loadDay(userCode, dayKey)
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                when (val state = dayViewModel.state.collectAsState().value) {
                    is DayUiState.Success -> {
                        EditDayScreen(
                            dayKey = dayKey,
                            day = state.day,
                            onMuscleGroupSelected = { groupKey, muscleGroup, updatedDay ->
                                dayViewModel.updateDay(userCode, dayKey, updatedDay)
                                navController.navigate(Screen.EditMuscleGroup.createRoute(userCode, dayKey, groupKey))
                            },
                            onSave = { updatedDay ->
                                dayViewModel.updateDay(userCode, dayKey, updatedDay)
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                    is DayUiState.Error -> {
                        Text(
                            text = "Error: ${state.exception.localizedMessage}",
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                    else -> {
                        // Box vuota
                    }
                }
            }
        }
        composable(Screen.EditMuscleGroup.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val dayKey = backStackEntry.arguments?.getString("dayKey") ?: ""
            val groupKey = backStackEntry.arguments?.getString("groupKey") ?: ""
            val muscleGroupViewModel: MuscleGroupViewModel = viewModel(factory = MuscleGroupViewModelFactory(userCode, dayKey, groupKey))
            LaunchedEffect(userCode, dayKey, groupKey) {
                muscleGroupViewModel.loadGroup(userCode, dayKey, groupKey)
            }
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                when (val state = muscleGroupViewModel.state.collectAsState().value) {
                    is MuscleGroupUiState.Success -> {
                        EditMuscleGroupScreen(
                            userCode = userCode,
                            dayKey = dayKey,
                            group = state.group,
                            onSave = { updatedGroup ->
                                muscleGroupViewModel.updateMuscleGroup(userCode, dayKey, groupKey, updatedGroup)
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                    is MuscleGroupUiState.Error -> {
                        Text(
                            text = "Error: ${state.exception.localizedMessage}",
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                    else -> {
                        // Box vuota
                    }
                }
            }
        }
    }
}