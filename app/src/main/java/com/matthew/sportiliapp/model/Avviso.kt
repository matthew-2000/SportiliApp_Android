package com.matthew.sportiliapp.model

import java.util.Locale

/**
 * Rappresenta un avviso mostrato agli utenti dell'applicazione.
 * [scadenza] è espresso in millisecondi dal 1 gennaio 1970 UTC.
 */
data class Avviso(
    var id: String = "",
    val titolo: String = "",
    val descrizione: String = "",
    val urgenza: String? = null,
    val scadenza: Long? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "titolo" to titolo,
        "descrizione" to descrizione,
        "urgenza" to urgenza,
        "scadenza" to scadenza
    )

    fun isExpired(referenceTime: Long = System.currentTimeMillis()): Boolean {
        val limit = scadenza ?: return false
        return limit < referenceTime
    }

    fun urgencyWeight(): Int = when (urgenza?.lowercase(Locale.getDefault())) {
        "alta" -> 3
        "media" -> 2
        "bassa" -> 1
        else -> 0
    }
}
