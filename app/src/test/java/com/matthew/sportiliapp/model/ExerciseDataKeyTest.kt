package com.matthew.sportiliapp.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class ExerciseDataKeyTest {
    @Test fun `canonical keys are stable and Firebase safe`() {
        assertEquals("panca_inclinata", ExerciseDataKey.canonical(" Panca inclinata "))
        assertEquals("elite", ExerciseDataKey.canonical("Élite"))
        assertEquals("exercise_u_0a932e41848fbb46", ExerciseDataKey.canonical("🏋️"))
    }

    @Test fun `canonical key does not depend on device locale`() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertEquals("incline", ExerciseDataKey.canonical("INCLINE"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun `existing legacy key remains authoritative`() {
        assertEquals("lite", ExerciseDataKey.resolve("Élite", setOf("lite")))
        assertEquals("elite", ExerciseDataKey.resolve("Élite", emptySet()))
        assertEquals("elite", ExerciseDataKey.resolve("Élite", setOf("lite", "elite")))
    }
}
