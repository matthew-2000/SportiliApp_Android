package com.matthew.sportiliapp.model

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

/** Informational only: requests keep using the existing Firebase flag. */
object SchedaChangeRequestSchedule {
    const val outsideWindowMessage = "Dopo la scadenza della scheda, invia la richiesta tra venerdì alle 20:00 e sabato alle 10:00 (ora italiana). Il personal trainer si occupa dei cambi la domenica. Puoi inviare comunque la richiesta: il cambio non è immediato e l'invio fuori orario non lo anticipa."
    const val sentMessage = "Il trainer ha ricevuto la richiesta. I cambi scheda vengono gestiti la domenica."

    fun isWithinWindow(now: Date = Date()): Boolean {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Rome")).apply { time = now }
        val day = calendar.get(Calendar.DAY_OF_WEEK)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        // Friday 20:00 inclusive, Saturday 10:00 exclusive.
        return (day == Calendar.FRIDAY && hour >= 20) ||
            (day == Calendar.SATURDAY && hour < 10)
    }
}
