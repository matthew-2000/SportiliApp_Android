package com.matthew.sportiliapp.scheda

import com.matthew.sportiliapp.model.WeightLogEntry
import org.junit.Assert.*
import org.junit.Test

class S05WeightInputTest {
    @Test fun decimalSeparatorsAndWhitespace() {
        for (input in listOf("47,5", "47.5", " 47,5 ")) assertEquals(47.5, parsedWeightInput(input)!!, 0.0)
        assertEquals(1.0, parsedWeightInput("1")!!, 0.0)
    }
    @Test fun invalidAndNonFiniteWeightsNeverReachWrites() {
        for (input in listOf("", "0", "-1", "abc", "NaN", "Infinity", "1e309", "1e2", "1,2.3", "1..2", "9".repeat(400))) {
            assertNull(input, parsedWeightInput(input))
        }
    }
    @Test fun timerDurationsAndMissingRestRemainUnchanged() {
        assertEquals(90, parseRiposo("1:30"))
        assertEquals(90, parseRiposo("1' 30\""))
        assertEquals(0, parseRiposo(""))
        assertEquals(0, parseRiposo("non configurato"))
    }
    @Test fun recentSamplesUseLatestTenAndIrregularDatesKeepOrder() {
        val entries = (0..11).map { WeightLogEntry(it.toDouble(), (it * it * 1000).toLong()) }.reversed()
        val recent = recentWeightSamples(entries)
        assertEquals(10, recent.size)
        assertEquals(2.0, recent.first().weight!!, 0.0)
        assertEquals(11.0, recent.last().weight!!, 0.0)
        assertEquals(1, recentWeightSamples(entries.take(1)).size)
        assertTrue(recentWeightSamples(emptyList()).isEmpty())
        assertEquals(10, recentWeightSamples(entries.take(10)).size)
    }
}
