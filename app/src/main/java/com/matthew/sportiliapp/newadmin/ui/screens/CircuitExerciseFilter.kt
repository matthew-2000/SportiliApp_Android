package com.matthew.sportiliapp.newadmin.ui.screens

import com.matthew.sportiliapp.model.GruppoMuscolarePredefinito

private val circuitGroupOrder = listOf(
    "Addominali", "Gambe e Glutei", "Pettorali", "Spalle", "Dorsali",
    "Bicipiti", "Tricipiti", "Polpacci", "Cardio"
)

/** Search exercise names only, retaining catalog order within the existing group order. */
internal fun filterCircuitExerciseGroups(
    groups: List<GruppoMuscolarePredefinito>,
    query: String
): List<GruppoMuscolarePredefinito> = groups
    .sortedBy { group ->
        circuitGroupOrder.indexOfFirst { it.equals(group.nome, ignoreCase = true) }
            .takeIf { it >= 0 } ?: Int.MAX_VALUE
    }
    .mapNotNull { group ->
        val exercises = group.esercizi.filter { it.nome.contains(query.trim(), ignoreCase = true) }
        group.copy(esercizi = exercises).takeIf { exercises.isNotEmpty() }
    }
