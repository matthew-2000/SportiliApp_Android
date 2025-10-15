package com.matthew.sportiliapp.model

import android.os.Parcel
import android.os.Parcelable

data class Esercizio(
    var name: String,
    var serie: String,
    var priorita: Int? = null,
    var riposo: String? = null,
    var notePT: String? = null,
    var noteUtente: String? = null,
    var weightLogs: Map<String, WeightLogEntry>? = null,
    var ordine: Int? = null
) : Parcelable {

    constructor() : this(name = "", serie = "")

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        readWeightLogs(parcel),
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(serie)
        parcel.writeValue(priorita)
        parcel.writeString(riposo)
        parcel.writeString(notePT)
        parcel.writeString(noteUtente)
        writeWeightLogs(parcel, weightLogs, flags)
        parcel.writeValue(ordine)
    }

    override fun describeContents(): Int {
        return 0
    }

    fun toMap(): Map<String, Any?> {
        val result: MutableMap<String, Any?> = HashMap()
        result["name"] = name
        result["serie"] = serie
        result["priorita"] = priorita
        result["riposo"] = riposo
        result["notePT"] = notePT
        result["noteUtente"] = noteUtente
        result["weightLogs"] = weightLogs
        result["ordine"] = ordine

        return result
    }

    override fun toString(): String {
        return "Esercizio(name='$name', serie='$serie', priorita=$priorita, riposo=$riposo, notePT=$notePT, noteUtente=$noteUtente, weightLogs=$weightLogs, ordine=$ordine)"
    }

    companion object CREATOR : Parcelable.Creator<Esercizio> {
        override fun createFromParcel(parcel: Parcel): Esercizio {
            return Esercizio(parcel)
        }

        override fun newArray(size: Int): Array<Esercizio?> {
            return arrayOfNulls(size)
        }

        private fun readWeightLogs(parcel: Parcel): Map<String, WeightLogEntry>? {
            val size = parcel.readInt()
            if (size < 0) return null
            val map = mutableMapOf<String, WeightLogEntry>()
            repeat(size) {
                val key = parcel.readString()
                val value = parcel.readParcelable<WeightLogEntry>(WeightLogEntry::class.java.classLoader)
                if (key != null && value != null) {
                    map[key] = value
                }
            }
            return map
        }

        private fun writeWeightLogs(parcel: Parcel, map: Map<String, WeightLogEntry>?, flags: Int) {
            if (map == null) {
                parcel.writeInt(-1)
                return
            }
            parcel.writeInt(map.size)
            map.forEach { (key, value) ->
                parcel.writeString(key)
                parcel.writeParcelable(value, flags)
            }
        }
    }
}
