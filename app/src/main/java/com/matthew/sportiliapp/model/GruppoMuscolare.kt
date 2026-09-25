package com.matthew.sportiliapp.model

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.database.Exclude

data class GruppoMuscolare(
    var nome: String,
    var esercizi: Map<String, Esercizio> = mapOf(),
    @get:Exclude var editBaseline: WorkoutEditBaseline? = null
) : Parcelable {

    fun toMap(): Map<String, Any> {
        val result: MutableMap<String, Any> = HashMap()
        result["nome"] = nome
        result["esercizi"] = esercizi.mapValues { entry -> entry.value.toMap() }
        return result
    }

    fun sortAll() {
        esercizi = esercizi.sortedExercises()
    }

    constructor() : this(nome = "", esercizi = mapOf())

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        mutableMapOf<String, Esercizio>().apply {
            parcel.readMap(this, Esercizio::class.java.classLoader)
        },
        null
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(nome)
        parcel.writeMap(esercizi)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "GruppoMuscolare(nome='$nome', esercizi=$esercizi)"
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
