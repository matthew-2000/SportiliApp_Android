package com.matthew.sportiliapp.newadmin

import com.matthew.sportiliapp.model.WorkoutEditBaseline
import newadmin.data.*
import org.junit.Assert.*
import org.junit.Test

class WorkoutMergeTest {
    private val day = mapOf("name" to "A", "noteUtente" to "nota", "weightLogs" to mapOf("a" to 40))
    private val known = mapOf("durata" to 4, "cambioRichiesto" to false, "giorni" to mapOf("giorno1" to day))
    private val raw = known + ("future" to "keep")
    private val baseline = WorkoutEditBaseline("test", firebaseTree(raw), firebaseTree(known))
    private fun save(edit: Any?, current: Any?, origins: Map<String, String>? = null) =
        mergeWorkout(baseline, firebaseTree(edit), firebaseTree(current), origins)

    @Test fun independentChangesPreserveRequestNotesHistoryAndUnknownFields() {
        val server = raw + mapOf("cambioRichiesto" to true, "giorni" to mapOf("giorno1" to (day + mapOf("noteUtente" to "nuova", "future" to 7, "weightLogs" to mapOf("a" to 40, "b" to 50)))))
        assertEquals(firebaseTree(server + ("durata" to 6)), save(known + ("durata" to 6), server))
    }
    @Test(expected = WorkoutConflict::class) fun conflictingDurationFails() {
        save(known + ("durata" to 6), raw + ("durata" to 8))
    }
    @Test(expected = WorkoutConflict::class) fun deletionCannotLoseNewNotes() {
        save(known - "giorni", raw + ("giorni" to mapOf("giorno1" to (day + ("noteUtente" to "nuova")))))
    }
    @Test fun intentionalDeletionSucceeds() {
        assertEquals(firebaseTree(raw - "giorni"), save(known - "giorni", raw))
    }
    @Test fun reorderMovesUnknownFieldsWithTheDay() {
        val server = raw + ("giorni" to mapOf("giorno1" to (day + ("future" to "A"))))
        val base = baseline.copy(raw = firebaseTree(server))
        val edited = known + ("giorni" to mapOf("giorno2" to day))
        val expected = server + mapOf("giorni" to mapOf("giorno2" to (day + mapOf("future" to "A"))))
        assertEquals(firebaseTree(expected),
            mergeWorkout(base, firebaseTree(edited), firebaseTree(server), mapOf("giorno2" to "giorno1")))
    }
    @Test(expected = WorkoutConflict::class) fun reorderConflictsWithConcurrentChanges() {
        save(known + ("giorni" to mapOf("giorno2" to day)), raw + ("giorni" to mapOf("giorno1" to (day + ("future" to 1)))), mapOf("giorno2" to "giorno1"))
    }
    @Test fun intentionallyReplacedDayDoesNotInheritDeletedUnknownFields() {
        val server = raw + ("giorni" to mapOf("giorno1" to (day + ("future" to "remove"))))
        val base = baseline.copy(raw = firebaseTree(server))
        val edited = known + ("giorni" to mapOf("giorno1" to mapOf("name" to "New day")))
        assertEquals(firebaseTree(edited + ("future" to "keep")),
            mergeWorkout(base, firebaseTree(edited), firebaseTree(server), emptyMap()))
    }

    @Test fun explicitRequestCompletionPreservesUnknownFields() {
        val original = known + ("cambioRichiesto" to true)
        val base = WorkoutEditBaseline("test", firebaseTree(raw + ("cambioRichiesto" to true)), firebaseTree(original))
        assertEquals(firebaseTree(raw), mergeWorkout(base, firebaseTree(known), base.raw, null))
    }
}
