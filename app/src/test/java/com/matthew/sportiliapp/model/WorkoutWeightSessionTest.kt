package com.matthew.sportiliapp.model

import org.junit.Assert.*
import org.junit.Test

class WorkoutWeightSessionTest {
    private val session = WorkoutWeightSession("session-a", 1790000000000L,
        listOf(WorkoutWeightSet(12, 30.0), WorkoutWeightSet(10, 35.5), WorkoutWeightSet(8, 40.0), WorkoutWeightSet(6, 45.0)))

    @Test fun sharedWireFormatRoundTripsAndKeepsOrder() {
        val payload = session.toMap()
        assertEquals(setOf("schemaVersion", "timestamp", "sets"), payload.keys)
        assertEquals(setOf("set_00", "set_01", "set_02", "set_03"), (payload["sets"] as Map<*, *>).keys)
        assertEquals(session, WorkoutWeightSession.parse(session.id, payload))
        assertTrue(session.matches(listOf(12, 10, 8, 6)))
        assertFalse(session.matches(listOf(12, 10, 8)))
        assertFalse(session.matches(listOf(12, 10, 6, 8)))
    }

    @Test fun malformedDataAndFutureVersionsAreRejectedWithoutPartialSeries() {
        for (payload in listOf(
            session.toMap() + ("schemaVersion" to 2),
            session.toMap() + ("schemaVersion" to true),
            session.toMap() + ("timestamp" to 0),
            session.toMap() + ("timestamp" to 1790000000000.5),
            session.toMap() + ("timestamp" to Double.NaN),
            session.toMap() + ("sets" to emptyMap<String, Any>()),
            session.toMap() + ("sets" to mapOf("set_01" to mapOf("reps" to 12, "weight" to 30))),
            session.toMap() + ("sets" to mapOf("set_00" to mapOf("reps" to 12.5, "weight" to 30))),
            session.toMap() + ("sets" to mapOf("set_00" to mapOf("reps" to 12, "weight" to Double.POSITIVE_INFINITY))),
            session.toMap() + ("sets" to mapOf("set_00" to mapOf("reps" to 12, "weight" to 0)))
        )) assertNull(payload.toString(), WorkoutWeightSession.parse("bad", payload))
        assertFalse(session.copy(sets = List(21) { WorkoutWeightSet(10, 20.0) }).isValid)
    }

    @Test fun prescriptionsAreRecognizedConservatively() {
        assertEquals(listOf(12, 10, 8, 6), workoutRepetitions("12 - 10 - 8 - 6"))
        assertEquals(listOf(10, 10, 10, 10), workoutRepetitions("4 × 10"))
        for (text in listOf("", "3x10 + 3x12", "12-10-8 a cedimento", "3x0", "21x10", "12-999999999999999999999999-8", "4x999999999999999999999999", "12-0-8")) {
            assertTrue(text, workoutRepetitions(text).isEmpty())
        }
    }
}
