package com.matthew.sportiliapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BottomNavigationRouteTest {
    @Test fun workoutDetailsSelectSchedaForPatternsAndResolvedRoutes() {
        for (route in listOf("scheda", "giorno/{giornoId}", "giorno/a",
            "esercizio/{giornoId}/{gruppoMuscolareId}/{esercizioId}", "esercizio/a/petto/panca")) {
            assertEquals("scheda", bottomNavigationRoute(route))
        }
    }
    @Test fun otherSectionsAndUnknownRoutesStayIndependent() {
        assertEquals("avvisi", bottomNavigationRoute("avvisi"))
        assertEquals("impostazioni", bottomNavigationRoute("impostazioni"))
        for (route in listOf(null, "giornoExtra", "esercizioAltro", "login")) {
            assertNull(bottomNavigationRoute(route))
        }
    }
}
