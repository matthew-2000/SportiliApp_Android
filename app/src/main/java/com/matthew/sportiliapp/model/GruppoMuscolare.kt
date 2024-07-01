package com.matthew.sportiliapp.model

data class GruppoMuscolare(
    var nome: String,
    var esercizi: Map<String, Esercizio> = mapOf()
) {
    constructor() : this(nome = "", esercizi = mapOf())
}