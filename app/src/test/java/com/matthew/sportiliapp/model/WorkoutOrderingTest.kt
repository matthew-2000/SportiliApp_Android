package com.matthew.sportiliapp.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutOrderingTest {

    @Test
    fun `group sort keeps numeric exercise order instead of lexicographic order`() {
        val group = GruppoMuscolare(
            nome = "Circuito",
            esercizi = linkedMapOf(
                "esercizio10" to Esercizio(name = "Exercise 10", serie = "3x10"),
                "esercizio2" to Esercizio(name = "Exercise 2", serie = "3x10"),
                "esercizio1" to Esercizio(name = "Exercise 1", serie = "3x10")
            )
        )

        group.sortAll()

        assertEquals(
            listOf("esercizio1", "esercizio2", "esercizio10"),
            group.esercizi.keys.toList()
        )
    }

    @Test
    fun `group sort prefers explicit exercise order when present`() {
        val group = GruppoMuscolare(
            nome = "Circuito",
            esercizi = linkedMapOf(
                "esercizio1" to Esercizio(name = "Exercise 1", serie = "3x10", ordine = 2),
                "esercizio2" to Esercizio(name = "Exercise 2", serie = "3x10", ordine = 0),
                "esercizio3" to Esercizio(name = "Exercise 3", serie = "3x10", ordine = 1)
            )
        )

        group.sortAll()

        assertEquals(
            listOf("Exercise 2", "Exercise 3", "Exercise 1"),
            group.esercizi.values.map { it.name }
        )
    }

    @Test
    fun `scheda sort keeps numeric order for days and groups`() {
        val scheda = Scheda(
            dataInizio = "2026-01-01T00:00:00+0000",
            durata = 4,
            giorni = linkedMapOf(
                "giorno10" to Giorno(
                    name = "Giorno 10",
                    gruppiMuscolari = linkedMapOf(
                        "gruppo10" to GruppoMuscolare(nome = "Gruppo 10"),
                        "gruppo2" to GruppoMuscolare(nome = "Gruppo 2")
                    )
                ),
                "giorno2" to Giorno(name = "Giorno 2"),
                "giorno1" to Giorno(name = "Giorno 1")
            )
        )

        scheda.sortAll()

        assertEquals(listOf("giorno1", "giorno2", "giorno10"), scheda.giorni.keys.toList())
        assertEquals(
            listOf("gruppo2", "gruppo10"),
            scheda.giorni.getValue("giorno10").gruppiMuscolari.keys.toList()
        )
    }

    @Test
    fun `sorted snapshot preserves legacy keys and does not mutate the source`() {
        val scheda = Scheda(
            dataInizio = "2026-01-01T00:00:00+0000",
            durata = 4,
            giorni = linkedMapOf(
                "giorno10" to Giorno(
                    name = "Legacy Day",
                    gruppiMuscolari = linkedMapOf(
                        "gruppo7" to GruppoMuscolare(
                            nome = "Circuito",
                            esercizi = linkedMapOf(
                                "esercizio10" to Esercizio(name = "Exercise 10", serie = "3x10"),
                                "esercizio2" to Esercizio(name = "Exercise 2", serie = "3x10"),
                                "esercizio1" to Esercizio(name = "Exercise 1", serie = "3x10")
                            )
                        )
                    )
                )
            )
        )

        val normalized = scheda.sortedSnapshot()
        val normalizedExercises = normalized.giorni
            .getValue("giorno10")
            .gruppiMuscolari
            .getValue("gruppo7")
            .esercizi

        assertEquals(listOf("giorno10"), normalized.giorni.keys.toList())
        assertEquals(listOf("gruppo7"), normalized.giorni.getValue("giorno10").gruppiMuscolari.keys.toList())
        assertEquals(listOf("esercizio1", "esercizio2", "esercizio10"), normalizedExercises.keys.toList())
        assertEquals(
            listOf("Exercise 1", "Exercise 2", "Exercise 10"),
            normalizedExercises.values.map { it.name }
        )
        normalizedExercises.values.forEach { assertNull(it.ordine) }
        assertEquals(
            listOf("esercizio10", "esercizio2", "esercizio1"),
            scheda.giorni.getValue("giorno10").gruppiMuscolari.getValue("gruppo7").esercizi.keys.toList()
        )
    }

    @Test
    fun `sorted snapshot honors explicit order while preserving data and Firebase paths`() {
        val logs = mapOf("log1" to WeightLogEntry(weight = 42.5, timestamp = 1700000000000))
        val first = Esercizio("First", "3x10", ordine = 0, noteUtente = "Keep", weightLogs = logs)
        val second = Esercizio("Second", "3x8", ordine = 1)
        val scheda = Scheda("2026-01-01T00:00:00+0000", 4, linkedMapOf(
            "giorno3" to Giorno("C", mapOf("gruppo8" to GruppoMuscolare("Gambe", linkedMapOf(
                "esercizio2" to second, "esercizio10" to first
            ))))
        ), cambioRichiesto = true)

        val sorted = scheda.sortedSnapshot()
        val exercises = sorted.giorni.getValue("giorno3").gruppiMuscolari.getValue("gruppo8").esercizi

        assertEquals(listOf("esercizio10", "esercizio2"), exercises.keys.toList())
        assertEquals(first, exercises.getValue("esercizio10"))
        assertEquals(second, exercises.getValue("esercizio2"))
        assertTrue(sorted.cambioRichiesto)
        assertEquals(scheda.toMap(), sorted.toMap())
    }
}
