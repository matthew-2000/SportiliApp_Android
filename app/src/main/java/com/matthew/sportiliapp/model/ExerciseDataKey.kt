package com.matthew.sportiliapp.model

import java.text.Normalizer
import java.util.Locale

internal object ExerciseDataKey {
    fun canonical(name: String): String {
        val normalized = normalizedName(name)
        val ascii = Normalizer.normalize(normalized, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
        return ascii.ifEmpty { "exercise_u_${fnv1a64(normalized)}" }
    }

    fun resolve(name: String, existingKeys: Set<String>): String {
        val canonical = canonical(name)
        if (canonical in existingKeys) return canonical
        val legacy = legacy(name)
        return if (legacy in existingKeys) legacy else canonical
    }

    internal fun legacy(name: String): String {
        val trimmed = name.trim()
        val sanitized = trimmed.lowercase(Locale.getDefault())
            .replace("[^a-z0-9]+".toRegex(), "_")
            .trim('_')
        return sanitized.ifEmpty { "exercise_${trimmed.hashCode()}" }
    }

    private fun normalizedName(name: String): String =
        Normalizer.normalize(name.trim().lowercase(Locale.ROOT), Normalizer.Form.NFC)

    private fun fnv1a64(value: String): String {
        var hash = 0xcbf29ce484222325uL
        value.toByteArray(Charsets.UTF_8).forEach { byte ->
            hash = (hash xor byte.toUByte().toULong()) * 0x100000001b3uL
        }
        return hash.toString(16).padStart(16, '0')
    }
}
