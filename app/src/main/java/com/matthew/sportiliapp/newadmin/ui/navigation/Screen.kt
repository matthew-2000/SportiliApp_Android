package com.matthew.sportiliapp.newadmin.ui.navigation

sealed class Screen(val route: String) {
    object UserList : Screen("user_list")
    object EditUser : Screen("edit_user/{userCode}") {
        fun createRoute(userCode: String) = "edit_user/$userCode"
    }
    object EditWorkoutCard : Screen("edit_workout_card/{userCode}") {
        fun createRoute(userCode: String) = "edit_workout_card/$userCode"
    }
    object EditDay : Screen("edit_day/{userCode}/{dayKey}") {
        fun createRoute(userCode: String, dayKey: String) = "edit_day/$userCode/$dayKey"
    }
    object EditMuscleGroup : Screen("edit_muscle_group/{userCode}/{dayKey}/{groupKey}") {
        fun createRoute(userCode: String, dayKey: String, groupKey: String) =
            "edit_muscle_group/$userCode/$dayKey/$groupKey"
    }
    object EditExercise : Screen("edit_exercise/{userCode}/{dayKey}/{groupKey}/{exerciseKey}") {
        fun createRoute(userCode: String, dayKey: String, groupKey: String, exerciseKey: String) =
            "edit_exercise/$userCode/$dayKey/$groupKey/$exerciseKey"
    }
    object Alerts : Screen("alerts")
}
