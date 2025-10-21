package com.matthew.sportiliapp.model

import java.util.UUID

/**
 * Rappresenta una segnalazione inviata da un utente su un problema riscontrato nella scheda.
 */
data class WorkoutIssueReport(
    var id: String = "",
    val userCode: String = "",
    val userName: String = "",
    val message: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val resolved: Boolean = false,
    val resolutionNote: String? = null
) {
    fun ensureId(): WorkoutIssueReport {
        if (id.isBlank()) {
            id = UUID.randomUUID().toString()
        }
        return this
    }
}
