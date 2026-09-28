package com.matthew.sportiliapp.scheda

import com.matthew.sportiliapp.model.Scheda
import java.time.OffsetDateTime
import java.util.Date
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class SchedaOverviewStateTest {
    private fun date(value: String): Date = Date.from(OffsetDateTime.parse(value).toInstant())

    @Test
    fun `status follows active expiring expired and requested priority`() {
        val workout = Scheda(
            dataInizio = "2026-09-01T12:00:00+0200",
            durata = 4
        )

        assertEquals(
            WorkoutStatus.Active,
            workoutStatus(workout, date("2026-09-20T12:00:00+02:00"))
        )
        assertEquals(
            WorkoutStatus.Expiring,
            workoutStatus(workout, date("2026-09-23T12:00:00+02:00"))
        )
        assertEquals(
            WorkoutStatus.Expired,
            workoutStatus(workout, date("2026-09-29T12:00:00+02:00"))
        )
        assertEquals(
            WorkoutStatus.Requested,
            workoutStatus(
                workout.copy(cambioRichiesto = true),
                date("2026-09-20T12:00:00+02:00")
            )
        )
    }

    @Test
    fun `invalid start date is displayed safely`() {
        assertEquals("Data non disponibile", formatWorkoutDate("not-a-date", Locale.ITALIAN))
    }
}
