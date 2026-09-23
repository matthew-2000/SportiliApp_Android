package com.matthew.sportiliapp.model

import java.util.LinkedHashMap

internal fun <T> Map<String, T>.sortedByIndexedKey(): LinkedHashMap<String, T> =
    entries
        .sortedWith(
            compareBy<Map.Entry<String, T>>(
                { it.key.numericSuffix() ?: Int.MAX_VALUE },
                { it.key }
            )
        )
        .associateTo(LinkedHashMap()) { it.toPair() }

internal fun Map<String, Esercizio>.sortedExercises(): LinkedHashMap<String, Esercizio> =
    entries
        .sortedWith(
            compareBy<Map.Entry<String, Esercizio>>(
                { it.value.ordine ?: it.key.numericSuffix() ?: Int.MAX_VALUE },
                { it.key.numericSuffix() ?: Int.MAX_VALUE },
                { it.key }
            )
        )
        .associateTo(LinkedHashMap()) { it.toPair() }

private fun String.numericSuffix(): Int? =
    takeLastWhile(Char::isDigit)
        .takeIf { it.isNotEmpty() }
        ?.toIntOrNull()

// Reading a workout must never change the keys used by navigation and Firebase writes.
internal fun Scheda.sortedSnapshot(): Scheda {
    val sortedDays = giorni
        .sortedByIndexedKey()
        .mapValues { (_, day) ->
            val sortedGroups = day.gruppiMuscolari
                .sortedByIndexedKey()
                .mapValues { (_, group) ->
                    group.copy(esercizi = group.esercizi.sortedExercises())
                }
            day.copy(gruppiMuscolari = sortedGroups)
        }
    return copy(giorni = sortedDays)
}
