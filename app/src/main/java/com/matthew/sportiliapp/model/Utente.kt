package com.matthew.sportiliapp.model

import java.util.UUID

data class Utente(
    var code: String,
    val cognome: String,
    val nome: String,
    var scheda: Scheda? = null
) {
    override fun toString(): String {
        return """
        Utente {
            code: $code
            cognome: $cognome
            nome: $nome
            scheda: ${scheda?.toString() ?: "null"}
        }
        """.trimIndent()
    }
    constructor() : this("", "", "")
}


