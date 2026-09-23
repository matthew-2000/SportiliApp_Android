package com.matthew.sportiliapp.model

import java.time.OffsetDateTime
import java.util.*
import org.junit.Assert.*
import org.junit.Test

class WorkoutExpiryTest {
    private val zone = TimeZone.getTimeZone("Europe/Rome")
    private fun date(s: String) = Date.from(OffsetDateTime.parse(s).toInstant())
    @Test fun labelsAndExactBoundaries() {
        val card = Scheda("2026-09-01T12:00:00+0200", 4)
        for ((now, expected) in listOf(
            "2026-09-22T12:00:00+02:00" to "1 settimana rimanente",
            "2026-09-23T12:00:00+02:00" to "6 giorni rimanenti",
            "2026-09-28T12:00:00+02:00" to "1 giorno rimanente",
            "2026-09-29T11:59:59+02:00" to "Meno di un giorno rimanente",
            "2026-09-29T12:00:00+02:00" to "Scheda scaduta"
        )) assertEquals(expected, card.tempoRimanente(date(now), zone))
    }
    @Test fun calendarDaysAcrossBothClockChanges() {
        assertEquals("1 giorno rimanente", Scheda("2026-03-22T12:00:00+0100", 1).tempoRimanente(date("2026-03-28T12:00:00+01:00"), zone))
        assertEquals("1 giorno rimanente", Scheda("2026-10-18T12:00:00+0200", 1).tempoRimanente(date("2026-10-24T12:00:00+02:00"), zone))
    }
}
