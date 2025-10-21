package com.matthew.sportiliapp.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class UserExerciseData(
    var noteUtente: String? = null,
    var weightLogs: Map<String, WeightLogEntry>? = null
)
