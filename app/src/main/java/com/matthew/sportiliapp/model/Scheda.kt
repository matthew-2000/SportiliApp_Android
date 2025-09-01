package com.matthew.sportiliapp.model
import com.google.firebase.database.IgnoreExtraProperties
import java.text.SimpleDateFormat
import java.util.*

@IgnoreExtraProperties
data class Scheda(
    var dataInizio: String,
    var durata: Int,
    var giorni: Map<String, Giorno> = mapOf(),
    var cambioRichiesto: Boolean = false // 👈 nuovo campo
) {
    fun toMap(): Map<String, Any> {
        val result: MutableMap<String, Any> = HashMap()
        result["dataInizio"] = dataInizio
        result["durata"] = durata

        // Convertendo la mappa `giorni` a una mappa di chiavi-valori per ogni `Giorno`.
        result["giorni"] = giorni.mapValues { entry -> entry.value.toMap() }
        result["cambioRichiesto"] = cambioRichiesto // 👈 includilo anche qui

        return result
    }

    companion object {
        val dateFormatter: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
    }

    constructor() : this("", 0, mapOf(), false)


    fun sortAll() {
        giorni = giorni.toSortedMap()
        giorni.forEach { giorno ->
            giorno.value.gruppiMuscolari = giorno.value.gruppiMuscolari.toSortedMap()
            giorno.value.gruppiMuscolari.forEach { gruppo ->
                gruppo.value.esercizi = gruppo.value.esercizi.toSortedMap()
            }
        }
    }

    fun isSchedaValida(): Boolean {
        // Definisci il formato della data che ricevi
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())

        // Prova a parsare la stringa `dataInizio` in un oggetto Date
        val startDate: Date = try {
            dateFormat.parse(dataInizio) ?: return false
        } catch (e: Exception) {
            return false  // Se la parsing fallisce, restituisce false
        }

        // Ottieni l'istanza del calendario e imposta la data di inizio
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        // Aggiungi la durata (in settimane) alla data di inizio per ottenere la data di fine
        calendar.add(Calendar.WEEK_OF_YEAR, durata)

        // Ottieni la data di fine della scheda
        val endDate = calendar.time
        // Ottieni la data corrente
        val currentDate = Date()

        // Restituisci `true` se la scheda è ancora valida, `false` altrimenti
        return currentDate < endDate
    }

    fun getSettimaneMancanti(): Int {
        // Definisci il formato della data che ricevi
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())

        // Prova a parsare la stringa `dataInizio` in un oggetto Date
        val startDate: Date = try {
            dateFormat.parse(dataInizio) ?: return 0
        } catch (e: Exception) {
            return 0  // Se la parsing fallisce, restituisce 0
        }

        // Ottieni l'istanza del calendario e imposta la data di inizio
        val calendar = Calendar.getInstance()
        calendar.time = startDate

        // Aggiungi la durata (in settimane) alla data di inizio per ottenere la data di fine
        calendar.add(Calendar.WEEK_OF_YEAR, durata)

        // Ottieni la data di fine della scheda
        val endDate = calendar.time
        // Ottieni la data corrente
        val currentDate = Date()

        // Calcola il numero di settimane rimanenti
        val diffInMillis = endDate.time - currentDate.time
        return (diffInMillis / (1000 * 60 * 60 * 24 * 7)).toInt()
    }

    override fun toString(): String {
        return "Scheda(dataInizio='$dataInizio', durata=$durata, giorni=$giorni)"
    }
}
