package com.matthew.sportiliapp.newadmin

import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.newadmin.ui.screens.AdminUserFilter
import com.matthew.sportiliapp.newadmin.ui.screens.UserWorkoutStatus
import com.matthew.sportiliapp.newadmin.ui.screens.filterAdminUsers
import com.matthew.sportiliapp.newadmin.ui.screens.indexAdminUsers
import org.junit.Assert.assertEquals
import org.junit.Test

class UserListScreenTest {

    private val users = listOf(
        Utente("REQ-1", "Rossi", "Giulia", futureWorkout(requested = true)),
        Utente("OLD-2", "Bianchi", "Marco", expiredWorkout()),
        Utente("NONE-3", "Verdi", "Sara"),
        Utente("OK-4", "Romano", "Luca", futureWorkout())
    )

    @Test
    fun `status prioritizes change requests and distinguishes missing workouts`() {
        val indexed = indexAdminUsers(users)

        assertEquals(UserWorkoutStatus.CHANGE_REQUESTED, indexed[0].status)
        assertEquals(UserWorkoutStatus.EXPIRED, indexed[1].status)
        assertEquals(UserWorkoutStatus.MISSING, indexed[2].status)
        assertEquals(UserWorkoutStatus.ACTIVE, indexed[3].status)
    }

    @Test
    fun `expired filter includes expired and missing but not requests`() {
        val result = filterAdminUsers(
            indexAdminUsers(users),
            searchText = "",
            filter = AdminUserFilter.EXPIRED_OR_MISSING
        )

        assertEquals(listOf("OLD-2", "NONE-3"), result.map { it.user.code })
    }

    @Test
    fun `search matches name and code within selected filter`() {
        val byName = filterAdminUsers(indexAdminUsers(users), "giulia", AdminUserFilter.CHANGE_REQUESTED)
        val byCode = filterAdminUsers(indexAdminUsers(users), "ok-4", AdminUserFilter.ALL)

        assertEquals(listOf("REQ-1"), byName.map { it.user.code })
        assertEquals(listOf("OK-4"), byCode.map { it.user.code })
    }

    private fun futureWorkout(requested: Boolean = false) = Scheda(
        dataInizio = "2099-01-01T08:00:00+0100",
        durata = 8,
        cambioRichiesto = requested
    )

    private fun expiredWorkout() = Scheda(
        dataInizio = "2020-01-01T08:00:00+0100",
        durata = 4
    )
}
