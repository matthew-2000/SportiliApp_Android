package com.matthew.sportiliapp.model
import com.google.firebase.database.IgnoreExtraProperties
import java.text.SimpleDateFormat
import java.util.*

@IgnoreExtraProperties
data class Scheda(
    var dataInizio: String,
    var durata: Int,
    var giorni: Map<String, Giorno> = mapOf() // Usa una mappa di GiornoFirebase
) {
    fun toMap(): Map<String, Any> {
        val result: MutableMap<String, Any> = HashMap()
        result["dataInizio"] = dataInizio
        result["durata"] = durata

        // Convertendo la mappa `giorni` a una mappa di chiavi-valori per ogni `Giorno`.
        result["giorni"] = giorni.mapValues { entry -> entry.value.toMap() }

        return result
    }

    companion object {
        val dateFormatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
    }

    constructor() : this("", 0, mapOf())

//    fun description(): String {
//        val giorniDesc = giorni.joinToString(separator = "\n    ") { it.description() }
//
//        return """
//        Scheda {
//            dataInizio: $dataInizio
//            durata: $durata
//            giorni: $giorniDesc
//        }
//        """.trimIndent()
//    }

    fun sortAll() {
        giorni = giorni.toSortedMap()
        giorni.forEach { giorno ->
            giorno.value.gruppiMuscolari = giorno.value.gruppiMuscolari.toSortedMap()
            giorno.value.gruppiMuscolari.forEach { gruppo ->
                gruppo.value.esercizi = gruppo.value.esercizi.toSortedMap()
            }
        }
    }

    fun getDurataScheda(): Int? {
//        val calendar = Calendar.getInstance()
//        calendar.time = dataInizio
//        calendar.add(Calendar.WEEK_OF_YEAR, durata)
//
//        val endDate = calendar.time
//        val currentDate = Date()
//
//        if (currentDate >= endDate) {
//            return null
//        }
//
//        val weeksDifference = (endDate.time - currentDate.time) / (1000 * 60 * 60 * 24 * 7)
//        return weeksDifference.toInt()
        return 1
    }
}
