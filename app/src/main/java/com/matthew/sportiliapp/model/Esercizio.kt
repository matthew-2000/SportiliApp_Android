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

    companion object CREATOR : Parcelable.Creator<Esercizio> {
        override fun createFromParcel(parcel: Parcel): Esercizio {
            return Esercizio(parcel)
        }

        override fun newArray(size: Int): Array<Esercizio?> {
            return arrayOfNulls(size)
        }
    }
}
