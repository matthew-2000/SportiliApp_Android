package newadmin.data

import com.matthew.sportiliapp.model.WorkoutEditBaseline
import com.matthew.sportiliapp.model.WeightLogEntry

internal class WorkoutConflict : IllegalStateException(
    "La scheda è cambiata durante la modifica. Riaprila e applica nuovamente le modifiche. Nessun dato è stato sovrascritto."
)

// Match Firebase's number and empty-node representation; detach mutable model data.
internal fun firebaseTree(value: Any?): Any? = when (value) {
    is Map<*, *> -> value.entries.mapNotNull { (k, v) -> firebaseTree(v)?.let { k.toString() to it } }
        .toMap().takeIf { it.isNotEmpty() }
    is Byte, is Short, is Int, is Long -> (value as Number).toLong()
    is WeightLogEntry -> firebaseTree(mapOf("weight" to value.weight, "timestamp" to value.timestamp))
    else -> value
}

private fun fields(value: Any?): Map<String, Any?> =
    (value as? Map<*, *>)?.entries?.associate { it.key.toString() to it.value }.orEmpty()

/** Apply only local changes. Compare affected values with the original raw snapshot. */
private fun merge(original: Any?, edited: Any?, raw: Any?, current: Any?): Any? {
    if (original == edited) return current
    if (original is Map<*, *> && edited is Map<*, *>) {
        if (raw != null && current !is Map<*, *>) throw WorkoutConflict()
        val before = fields(original)
        val after = fields(edited)
        val result = fields(current).toMutableMap()
        for (key in before.keys + after.keys) {
            val value = merge(before[key], after[key], fields(raw)[key], result[key])
            if (value == null) result.remove(key) else result[key] = value
        }
        return result.takeIf { it.isNotEmpty() }
    }
    // Subtree deletion is intentional, but may not discard concurrent additions.
    if (raw != current) throw WorkoutConflict()
    return edited
}

internal fun mergeWorkout(baseline: WorkoutEditBaseline, edited: Any?, current: Any?, origins: Map<String, String>?): Any? {
    val before = fields(baseline.modeled)
    val after = fields(edited)
    val raw = fields(baseline.raw)
    val latest = fields(current)
    val moved = origins?.any { it.key != it.value } == true
    val replaced = origins != null && fields(after["giorni"]).keys.any {
        it in fields(before["giorni"]) && it !in origins
    }
    if (!moved && !replaced) return merge(before, after, raw, latest)
    // Renumbering/replacement changes day identity. Preserve unknown descendants with their day;
    // if any day changed remotely, ask for a fresh edit instead of guessing identity.
    if (raw["giorni"] != latest["giorni"]) throw WorkoutConflict()
    val days = fields(after["giorni"]).mapValues { (key, day) ->
        val source = origins?.get(key)
        if (source == null) day else merge(fields(before["giorni"])[source], day,
            fields(raw["giorni"])[source], fields(raw["giorni"])[source])
    }
    val result = fields(merge(before - "giorni", after - "giorni", raw - "giorni", latest - "giorni")).toMutableMap()
    if (days.isNotEmpty()) result["giorni"] = days
    return result
}

internal fun mergeEditedTree(baseline: WorkoutEditBaseline, edited: Any?, current: Any?): Any? =
    merge(baseline.modeled, firebaseTree(edited), baseline.raw, firebaseTree(current))
