package com.matthew.sportiliapp.newadmin.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.matthew.sportiliapp.newadmin.di.ManualInjection
import com.matthew.sportiliapp.newadmin.ui.screens.AdminAlertsScreen
import com.matthew.sportiliapp.newadmin.ui.screens.AdminReportsScreen
import com.matthew.sportiliapp.newadmin.ui.screens.EditDayScreen
import com.matthew.sportiliapp.newadmin.ui.screens.EditMuscleGroupScreen
import com.matthew.sportiliapp.newadmin.ui.screens.EditUserScreen
import com.matthew.sportiliapp.newadmin.ui.screens.EditWorkoutCardScreen
import com.matthew.sportiliapp.newadmin.ui.screens.UserListScreen
import com.matthew.sportiliapp.newadmin.ui.viewmodel.AdminActionState
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
fun AdminNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.UserList.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Screen.UserList.route) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                UserListScreen(
                    onUserSelected = { user ->
                        navController.navigate(Screen.EditUser.createRoute(user.code))
                    },
                    onAddUser = {
                        navController.navigate(Screen.EditUser.createRoute("new"))
                    },
                    onManageAlerts = {
                        navController.navigate(Screen.Alerts.route)
                    },
                    onViewReports = {
                        navController.navigate(Screen.Reports.route)
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
            var userActionInProgress by remember(userCode) { mutableStateOf(false) }
            var userActionError by remember(userCode) { mutableStateOf<String?>(null) }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                if (userCode == "new") {
                    EditUserScreen(
                        initialUser = null,
                        isSaving = userActionInProgress,
                        errorMessage = userActionError,
                        onSave = { user ->
                            userActionInProgress = true
                            userActionError = null
                            gymAdminViewModel.addUser(user) { result ->
                                userActionInProgress = false
                                result.fold(
                                    onSuccess = {
                                        navController.navigate(
                                            Screen.EditWorkoutCard.createRoute(user.code)
                                        )
                                    },
                                    onFailure = { error ->
                                        userActionError = error.asUserMessage(
                                            "Errore durante la creazione dell'utente"
                                        )
                                    }
                                )
                            }
                        },
                        onCancel = { navController.popBackStack() },
                        onEditWorkoutCard = { }
                    )
                } else {
                    when (userState) {
                        UiState.Loading -> LoadingBox()
                        is UiState.Error -> ErrorBox(userState.exception.asUserMessage("Errore nel caricamento utente"))
                        is UiState.Success -> {
                            val initialUser = userState.data.find { it.code == userCode }
                            if (initialUser == null) {
                                ErrorBox("Utente non trovato")
                            } else {
                                EditUserScreen(
                                    initialUser = initialUser,
                                    isSaving = userActionInProgress,
                                    errorMessage = userActionError,
                                    onSave = { user ->
                                        userActionInProgress = true
                                        userActionError = null
                                        gymAdminViewModel.updateUser(user) { result ->
                                            userActionInProgress = false
                                            result.fold(
                                                onSuccess = {
                                                    navController.navigate(
                                                        Screen.EditWorkoutCard.createRoute(user.code)
                                                    )
                                                    navController.popBackStack()
                                                },
                                                onFailure = { error ->
                                                    userActionError = error.asUserMessage(
                                                        "Errore durante il salvataggio dell'utente"
                                                    )
                                                }
                                            )
                                        }
                                    },
                                    onRemove = {
                                        userActionInProgress = true
                                        userActionError = null
                                        gymAdminViewModel.removeUser(initialUser.code) { result ->
                                            userActionInProgress = false
                                            result.fold(
                                                onSuccess = {
                                                    navController.popBackStack()
                                                },
                                                onFailure = { error ->
                                                    userActionError = error.asUserMessage(
                                                        "Errore durante la rimozione dell'utente"
                                                    )
                                                }
                                            )
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
        }
        composable(Screen.EditWorkoutCard.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val workoutCardViewModel: WorkoutCardViewModel =
                viewModel(factory = WorkoutCardViewModelFactory(userCode))
            val state by workoutCardViewModel.state.collectAsState()
            val actionState by workoutCardViewModel.actionState.collectAsState()

            LaunchedEffect(userCode) {
                workoutCardViewModel.loadWorkoutCard(userCode)
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                when (state) {
                    WorkoutCardUiState.Idle,
                    WorkoutCardUiState.Loading -> LoadingBox()

                    is WorkoutCardUiState.Success -> {
                        val successState = state as WorkoutCardUiState.Success
                        EditWorkoutCardScreen(
                            scheda = successState.scheda,
                            isSaving = actionState is AdminActionState.InProgress,
                            errorMessage = actionState.errorMessage(),
                            onDaySelected = { dayKey, _, scheda ->
                                workoutCardViewModel.updateWorkoutCard(userCode, scheda) {
                                    navController.navigate(Screen.EditDay.createRoute(userCode, dayKey))
                                }
                            },
                            onSave = { updatedScheda ->
                                workoutCardViewModel.updateWorkoutCard(userCode, updatedScheda) {
                                    navController.popBackStack()
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }

                    is WorkoutCardUiState.Error -> {
                        ErrorBox(
                            (state as WorkoutCardUiState.Error).error.asUserMessage(
                                "Errore nel caricamento della scheda"
                            )
                        )
                    }
                }
            }
        }
        composable(Screen.EditDay.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val dayKey = backStackEntry.arguments?.getString("dayKey") ?: ""
            val dayViewModel: DayViewModel = viewModel(factory = DayViewModelFactory(userCode, dayKey))
            val state by dayViewModel.state.collectAsState()
            val actionState by dayViewModel.actionState.collectAsState()

            LaunchedEffect(userCode, dayKey) {
                dayViewModel.loadDay(userCode, dayKey)
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                when (state) {
                    DayUiState.Idle,
                    DayUiState.Loading -> LoadingBox()

                    is DayUiState.Success -> {
                        val successState = state as DayUiState.Success
                        EditDayScreen(
                            dayKey = dayKey,
                            day = successState.day,
                            isSaving = actionState is AdminActionState.InProgress,
                            errorMessage = actionState.errorMessage(),
                            onMuscleGroupSelected = { groupKey, _, updatedDay ->
                                dayViewModel.updateDay(userCode, dayKey, updatedDay) {
                                    navController.navigate(
                                        Screen.EditMuscleGroup.createRoute(userCode, dayKey, groupKey)
                                    )
                                }
                            },
                            onSave = { updatedDay ->
                                dayViewModel.updateDay(userCode, dayKey, updatedDay) {
                                    navController.popBackStack()
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }

                    is DayUiState.Error -> {
                        ErrorBox(
                            (state as DayUiState.Error).exception.asUserMessage(
                                "Errore nel caricamento del giorno"
                            )
                        )
                    }
                }
            }
        }
        composable(Screen.EditMuscleGroup.route) { backStackEntry ->
            val userCode = backStackEntry.arguments?.getString("userCode") ?: ""
            val dayKey = backStackEntry.arguments?.getString("dayKey") ?: ""
            val groupKey = backStackEntry.arguments?.getString("groupKey") ?: ""
            val muscleGroupViewModel: MuscleGroupViewModel =
                viewModel(factory = MuscleGroupViewModelFactory(userCode, dayKey, groupKey))
            val state by muscleGroupViewModel.state.collectAsState()
            val actionState by muscleGroupViewModel.actionState.collectAsState()

            LaunchedEffect(userCode, dayKey, groupKey) {
                muscleGroupViewModel.loadGroup(userCode, dayKey, groupKey)
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
                when (state) {
                    MuscleGroupUiState.Idle,
                    MuscleGroupUiState.Loading -> LoadingBox()

                    is MuscleGroupUiState.Success -> {
                        val successState = state as MuscleGroupUiState.Success
                        EditMuscleGroupScreen(
                            userCode = userCode,
                            dayKey = dayKey,
                            group = successState.group,
                            isSaving = actionState is AdminActionState.InProgress,
                            errorMessage = actionState.errorMessage(),
                            onSave = { updatedGroup ->
                                muscleGroupViewModel.updateMuscleGroup(
                                    userCode = userCode,
                                    dayKey = dayKey,
                                    groupKey = groupKey,
                                    group = updatedGroup
                                ) {
                                    navController.popBackStack()
                                }
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }

                    is MuscleGroupUiState.Error -> {
                        ErrorBox(
                            (state as MuscleGroupUiState.Error).exception.asUserMessage(
                                "Errore nel caricamento del gruppo muscolare"
                            )
                        )
                    }
                }
            }
        }
        composable(Screen.Alerts.route) {
            AdminAlertsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Reports.route) {
            AdminReportsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun LoadingBox() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message)
    }
}

private fun Throwable.asUserMessage(defaultMessage: String): String =
    localizedMessage?.takeIf { it.isNotBlank() } ?: defaultMessage

private fun AdminActionState.errorMessage(): String? =
    (this as? AdminActionState.Error)?.message
