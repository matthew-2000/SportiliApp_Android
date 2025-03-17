package com.matthew.sportiliapp.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class EserciziPredefinitiViewModel : ViewModel() {

    private val _gruppiMuscolariPredefiniti = MutableLiveData<List<GruppoMuscolarePredefinito>>()
    val gruppiMuscolariPredefiniti: LiveData<List<GruppoMuscolarePredefinito>> = _gruppiMuscolariPredefiniti

    private val ref: DatabaseReference = FirebaseDatabase.getInstance().reference.child("esercizi")

    private var isDataLoaded = false // ✅ Flag per bloccare richiami multipli

    fun fetchWorkoutData() {
        if (isDataLoaded) return // ✅ Blocca chiamate ripetute
        isDataLoaded = true

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gruppi = mutableListOf<GruppoMuscolarePredefinito>()

                snapshot.children.forEach { groupSnapshot ->
                    val key = groupSnapshot.key ?: return@forEach
                    val dataDict = groupSnapshot.value as? Map<*, *> ?: return@forEach
                    val eserciziArray = dataDict["esercizi"] as? List<Map<*, *>> ?: return@forEach

                    val esercizi = eserciziArray.mapNotNull { dict ->
                        val nome = dict["nome"] as? String ?: return@mapNotNull null
                        val esercizioId = dict["id"] as? String ?: UUID.nameUUIDFromBytes(nome.toByteArray()).toString()
                        EsercizioPredefinito(id = esercizioId, nome = nome, imageurl = "")
                    }.sortedBy { it.nome }

                    val gruppo = GruppoMuscolarePredefinito(
                        id = key, // ✅ Manteniamo ID stabile
                        nome = key,
                        esercizi = esercizi
                    )
                    gruppi.add(gruppo)
                }

                // ✅ Aggiorna solo se i dati sono diversi per evitare cicli infiniti
                if (_gruppiMuscolariPredefiniti.value != gruppi) {
                    _gruppiMuscolariPredefiniti.value = gruppi
                }
            }

            override fun onCancelled(error: DatabaseError) {
                error.toException().printStackTrace()
            }
        })
    }
}

data class EsercizioPredefinito(
    val id: String,
    val nome: String,
    val imageurl: String
)

data class GruppoMuscolarePredefinito(
    val id: String,
    val nome: String,
    val esercizi: List<EsercizioPredefinito>
)


