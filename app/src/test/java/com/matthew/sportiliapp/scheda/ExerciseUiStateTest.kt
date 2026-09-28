package com.matthew.sportiliapp.scheda

import com.matthew.sportiliapp.model.WeightLogEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseUiStateTest {
    @Test
    fun `weight summary orders entries and calculates range and variation`() {
        val summary = weightProgressSummary(
            listOf(
                WeightLogEntry(weight = 27.5, timestamp = 30),
                WeightLogEntry(weight = 24.0, timestamp = 10),
                WeightLogEntry(weight = null, timestamp = 40),
                WeightLogEntry(weight = 26.0, timestamp = 20)
            )
        )

        assertEquals(27.5, summary?.latest ?: 0.0, 0.0)
        assertEquals(1.5, summary?.change ?: 0.0, 0.0)
        assertEquals(24.0, summary?.minimum ?: 0.0, 0.0)
        assertEquals(27.5, summary?.maximum ?: 0.0, 0.0)
    }

    @Test
    fun `weight summary handles empty and single history`() {
        assertNull(weightProgressSummary(emptyList()))

        val summary = weightProgressSummary(
            listOf(WeightLogEntry(weight = 18.0, timestamp = 10))
        )
        assertEquals(18.0, summary?.latest ?: 0.0, 0.0)
        assertNull(summary?.change)
    }

    @Test
    fun `timer announces only important thresholds`() {
        assertEquals("10 secondi rimanenti", timerAnnouncementFor(10))
        assertEquals("5 secondi rimanenti", timerAnnouncementFor(5))
        assertEquals("Recupero terminato", timerAnnouncementFor(0))
        assertNull(timerAnnouncementFor(9))
    }
}
