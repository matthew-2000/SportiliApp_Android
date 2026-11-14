package com.matthew.sportiliapp.model

// Shared wire format: users/{code}/weightSessions/{exerciseKey}/{sessionId}.
// Keep outside exerciseData: installed iOS releases can delete that subtree.
data class WorkoutWeightSet(val reps: Int, val weight: Double) {
    val isValid get() = reps in 1..1000 && weight.isFinite() && weight > 0
}

data class WorkoutWeightSession(val id: String, val timestamp: Long, val sets: List<WorkoutWeightSet>) {
    val isValid get() = timestamp in 1..253402300799999L && sets.size in 1..20 && sets.all { it.isValid }
    fun matches(reps: List<Int>) = sets.map { it.reps } == reps
    fun toMap(): Map<String, Any> = mapOf(
        "schemaVersion" to 1, "timestamp" to timestamp,
        "sets" to sets.mapIndexed { index, set ->
            "set_%02d".format(java.util.Locale.ROOT, index) to mapOf("reps" to set.reps, "weight" to set.weight)
        }.toMap()
    )
    companion object {
        fun parse(id: String, value: Any?): WorkoutWeightSession? {
            val data = value as? Map<*, *> ?: return null
            if ((data["schemaVersion"] as? Number)?.toDouble() != 1.0) return null
            val timestamp = (data["timestamp"] as? Number)?.toDouble() ?: return null
            if (!timestamp.isFinite() || timestamp <= 0 || timestamp > 253402300799999.0 || timestamp % 1.0 != 0.0) return null
            val raw = data["sets"] as? Map<*, *> ?: return null
            if (raw.size !in 1..20) return null
            val sets = (0 until raw.size).map { index ->
                val set = raw["set_%02d".format(java.util.Locale.ROOT, index)] as? Map<*, *> ?: return null
                val reps = (set["reps"] as? Number)?.toDouble() ?: return null
                val weight = (set["weight"] as? Number)?.toDouble() ?: return null
                if (reps !in 1.0..1000.0 || reps % 1.0 != 0.0) return null
                WorkoutWeightSet(reps.toInt(), weight)
            }
            return WorkoutWeightSession(id, timestamp.toLong(), sets).takeIf { it.isValid }
        }
    }
}

/** Only unambiguous prescriptions; free-form trainer instructions remain unchanged. */
fun workoutRepetitions(prescription: String): List<Int> {
    val text = prescription.lowercase(java.util.Locale.ROOT).replace(" ", "").replace('×', 'x').replace('–', '-')
    if (Regex("^[0-9]+(-[0-9]+)+$").matches(text)) {
        val reps = text.split('-').map { it.toIntOrNull() ?: return emptyList() }
        return reps.takeIf { it.size in 2..20 && it.all { rep -> rep in 1..1000 } }.orEmpty()
    }
    if (Regex("^[0-9]+x[0-9]+$").matches(text)) {
        val values = text.split('x').map { it.toIntOrNull() ?: return emptyList() }
        if (values[0] in 1..20 && values[1] in 1..1000) return List(values[0]) { values[1] }
    }
    return emptyList()
}
