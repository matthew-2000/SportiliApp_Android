package com.matthew.sportiliapp.model

import android.os.Parcelable
import android.os.Parcel

data class Giorno(
    var name: String,
    var gruppiMuscolari: Map<String, GruppoMuscolare> = mapOf()
) : Parcelable {

    fun toMap(): Map<String, Any> {
        val result: MutableMap<String, Any> = HashMap()
        result["name"] = name
        result["gruppiMuscolari"] = gruppiMuscolari.mapValues { entry -> entry.value.toMap() }
        return result
    }

    fun sortAll() {
        gruppiMuscolari = gruppiMuscolari.toSortedMap()
        gruppiMuscolari.forEach { gruppo ->
            gruppo.value.esercizi = gruppo.value.esercizi.toSortedMap()
        }
    }

    constructor() : this(name = "", gruppiMuscolari = mapOf())

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        mutableMapOf<String, GruppoMuscolare>().apply {
            parcel.readMap(this, GruppoMuscolare::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeMap(gruppiMuscolari)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "Giorno(name='$name', gruppiMuscolari=$gruppiMuscolari)"
    }

    companion object CREATOR : Parcelable.Creator<Giorno> {
        override fun createFromParcel(parcel: Parcel): Giorno {
            return Giorno(parcel)
        }

        override fun newArray(size: Int): Array<Giorno?> {
            return arrayOfNulls(size)
        }
    }
}
