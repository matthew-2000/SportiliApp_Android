package com.matthew.sportiliapp.model

data class Giorno(
    var name: String,
    var gruppiMuscolari: Map<String, GruppoMuscolare> = mapOf()
) {
    constructor() : this(name = "", gruppiMuscolari = mapOf())
}