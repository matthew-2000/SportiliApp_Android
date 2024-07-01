package com.matthew.sportiliapp.model

data class Esercizio(
    var name: String,
    var serie: String,
    var priorita: Int? = null,
    var riposo: String? = null,
    var notePT: String? = null,
    var noteUtente: String? = null,
    var ordine: Int? = null
) {
    constructor() : this(name = "", serie = "")
}
