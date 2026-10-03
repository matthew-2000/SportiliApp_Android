package com.matthew.sportiliapp.newadmin.ui.screens

import com.matthew.sportiliapp.model.EsercizioPredefinito
import com.matthew.sportiliapp.model.GruppoMuscolarePredefinito
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CircuitExerciseFilterTest {
    private fun group(name: String, vararg exercises: String) = GruppoMuscolarePredefinito(
        name, name, exercises.map { EsercizioPredefinito(it, it, "") }
    )
    private val catalog = listOf(
        group("Extra B", "Panca extra"), group("Pettorali", "Panca piana", "Croci", "Panca inclinata"),
        group("Addominali", "Crunch"), group("Extra A", "Panca alternativa"), group("Cardio")
    )

    @Test fun emptyAndWhitespaceQueriesKeepGroupAndExerciseOrderAndHideEmptyGroups() {
        for (query in listOf("", "   ")) {
            val result = filterCircuitExerciseGroups(catalog, query)
            assertEquals(listOf("Addominali", "Pettorali", "Extra B", "Extra A"), result.map { it.nome })
            assertEquals(catalog[1].esercizi, result[1].esercizi)
        }
    }

    @Test fun partialCaseInsensitiveQueryMatchesNamesAndKeepsOriginalEntriesAndOrder() {
        val result = filterCircuitExerciseGroups(catalog, "  pAnC  ")
        assertEquals(listOf("Pettorali", "Extra B", "Extra A"), result.map { it.nome })
        assertEquals(listOf(catalog[1].esercizi[0], catalog[1].esercizi[2]), result[0].esercizi)
        assertEquals("Panca piana", catalog[1].esercizi[0].nome)
    }

    @Test fun noMatchOrGroupNameAloneHidesAllGroups() {
        assertTrue(filterCircuitExerciseGroups(catalog, "inesistente").isEmpty())
        assertTrue(filterCircuitExerciseGroups(catalog, "Pettorali").isEmpty())
    }
}
