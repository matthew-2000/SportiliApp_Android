package com.matthew.sportiliapp.newadmin

import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.newadmin.ui.screens.buildUpdatedScheda
import com.matthew.sportiliapp.newadmin.ui.screens.formatToDisplayDate
import org.junit.Assert.*
import org.junit.Test

class EditWorkoutCardTest {
    @Test
    fun `opening a day or saving ordinary changes preserves a pending request`() {
        val original = Scheda("2026-09-01T00:00:00+0000", 4,
            mapOf("giorno3" to Giorno("C")), cambioRichiesto = true)

        val unchanged = buildUpdatedScheda(original, formatToDisplayDate(original.dataInizio), "4", original.giorni.toList())
        val edited = buildUpdatedScheda(original, "22/09/2026", "6", original.giorni.toList())

        assertEquals(original.dataInizio, unchanged.dataInizio)
        assertTrue(unchanged.cambioRichiesto)
        assertTrue(edited.cambioRichiesto)
        assertEquals(6, edited.durata)
        assertEquals(original.giorni, edited.giorni)
        assertTrue(original.cambioRichiesto)
    }

    @Test
    fun `ordinary save does not create a change request`() {
        val original = Scheda("2026-09-01T00:00:00+0000", 4)
        val updated = buildUpdatedScheda(original, "22/09/2026", "6", emptyList())
        assertFalse(updated.cambioRichiesto)
    }
}
