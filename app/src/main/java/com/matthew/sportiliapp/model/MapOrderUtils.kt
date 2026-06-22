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

internal fun Scheda.normalizedOrderSnapshot(): Scheda {
    val normalizedDays = giorni
        .sortedByIndexedKey()
        .values
        .mapIndexed { dayIndex, day ->
            val normalizedGroups = day.gruppiMuscolari
                .sortedByIndexedKey()
                .values
                .mapIndexed { groupIndex, group ->
                    val normalizedExercises = group.esercizi
                        .sortedExercises()
                        .values
                        .mapIndexed { exerciseIndex, exercise ->
                            "esercizio${exerciseIndex + 1}" to exercise.copy(ordine = exerciseIndex)
                        }
                        .associateTo(LinkedHashMap()) { it }

                    "gruppo${groupIndex + 1}" to group.copy(esercizi = normalizedExercises)
                }
                .associateTo(LinkedHashMap()) { it }

            "giorno${dayIndex + 1}" to day.copy(gruppiMuscolari = normalizedGroups)
        }
        .associateTo(LinkedHashMap()) { it }

    return copy(giorni = normalizedDays)
}
