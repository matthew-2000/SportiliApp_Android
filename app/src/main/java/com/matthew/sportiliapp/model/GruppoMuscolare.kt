package com.matthew.sportiliapp.model

import android.os.Parcel
import android.os.Parcelable

data class GruppoMuscolare(
    var nome: String,
    var esercizi: Map<String, Esercizio> = mapOf()
) : Parcelable {

    constructor() : this(nome = "", esercizi = mapOf())

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        mutableMapOf<String, Esercizio>().apply {
            parcel.readMap(this, Esercizio::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(nome)
        parcel.writeMap(esercizi)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<GruppoMuscolare> {
        override fun createFromParcel(parcel: Parcel): GruppoMuscolare {
            return GruppoMuscolare(parcel)
        }

        override fun newArray(size: Int): Array<GruppoMuscolare?> {
            return arrayOfNulls(size)
        }
    }
}
