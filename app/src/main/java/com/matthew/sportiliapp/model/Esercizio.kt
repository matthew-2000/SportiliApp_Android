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
        parcel.readValue(Int::class.java.classLoader) as? Int
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(serie)
        parcel.writeValue(priorita)
        parcel.writeString(riposo)
        parcel.writeString(notePT)
        parcel.writeString(noteUtente)
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
        result["ordine"] = ordine

        return result
    }

    override fun toString(): String {
        return "Esercizio(name='$name', serie='$serie', priorita=$priorita, riposo=$riposo, notePT=$notePT, noteUtente=$noteUtente, ordine=$ordine)"
    }

    companion object CREATOR : Parcelable.Creator<Esercizio> {
        override fun createFromParcel(parcel: Parcel): Esercizio {
            return Esercizio(parcel)
        }

        override fun newArray(size: Int): Array<Esercizio?> {
            return arrayOfNulls(size)
        }
    }
}
