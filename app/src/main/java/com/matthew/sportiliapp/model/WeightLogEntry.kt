package com.matthew.sportiliapp.model

import android.os.Parcel
import android.os.Parcelable
import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class WeightLogEntry(
    var weight: Double? = null,
    var timestamp: Long? = null
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readValue(Double::class.java.classLoader) as? Double,
        parcel.readValue(Long::class.java.classLoader) as? Long
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(weight)
        parcel.writeValue(timestamp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<WeightLogEntry> {
        override fun createFromParcel(parcel: Parcel): WeightLogEntry {
            return WeightLogEntry(parcel)
        }

        override fun newArray(size: Int): Array<WeightLogEntry?> {
            return arrayOfNulls(size)
        }
    }
}
