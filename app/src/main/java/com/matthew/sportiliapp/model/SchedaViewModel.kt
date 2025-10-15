package com.matthew.sportiliapp.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
import com.matthew.sportiliapp.model.WeightLogEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SchedaViewModel(private val context: Context) : ViewModel() {
    private val _scheda = MutableLiveData<Scheda?>()
    private val _name = MutableLiveData<String?>()
    val scheda: LiveData<Scheda?> = _scheda
    val name: LiveData<String?> = _name
    val isLoading = MutableLiveData(true) // Stato di caricamento

    init {
        loadScheda()
    }

    private fun loadScheda() {
        viewModelScope.launch {
            // Imposta lo stato di caricamento a true
            isLoading.postValue(true)

            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = sharedPreferences.getString("code", "") ?: ""
            val database = FirebaseDatabase.getInstance()

            val schedaRef = database.reference.child("users").child(savedCode).child("scheda")
            schedaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.getValue(Scheda::class.java)
                    data?.sortAll()
                    _scheda.postValue(data)
                    // Caricamento completato, imposta isLoading a false
                    isLoading.postValue(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    // In caso di errore, imposta isLoading a false
                    isLoading.postValue(false)
                }
            })

            val nameRef = database.reference.child("users").child(savedCode).child("nome")
            nameRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.getValue(String::class.java)
                    _name.postValue(data)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Gestione errori
                }
            })
        }
    }

    fun addWeightEntry(
        giornoId: String,
        gruppoMuscolareId: String,
        esercizioId: String,
        weight: Double,
        onSuccess: (WeightLogEntry) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (weight <= 0) {
            onFailure("Il peso deve essere maggiore di zero")
            return
        }

        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = sharedPreferences.getString("code", "") ?: ""
            if (savedCode.isEmpty()) {
                onFailure("Codice utente non trovato")
                return@launch
            }

            val database = FirebaseDatabase.getInstance()

            val esercizioRef = database.reference
                .child("users")
                .child(savedCode)
                .child("scheda")
                .child("giorni")
                .child(giornoId)
                .child("gruppiMuscolari")
                .child(gruppoMuscolareId)
                .child("esercizi")
                .child(esercizioId)

            val timestamp = System.currentTimeMillis()
            val entry = WeightLogEntry(weight = weight, timestamp = timestamp)
            val entryData = hashMapOf<String, Any?>(
                "weight" to weight,
                "timestamp" to timestamp
            )

            val noteFormatter = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            val formattedNote = "Ultimo peso: ${String.format(Locale.getDefault(), "%.1f", weight)} kg - ${noteFormatter.format(Date(timestamp))}"

            val weightLogRef = esercizioRef.child("weightLogs").push()
            weightLogRef.setValue(entryData)
                .addOnSuccessListener {
                    esercizioRef.child("noteUtente").setValue(formattedNote)
                    onSuccess(entry)
                }
                .addOnFailureListener { error ->
                    onFailure(error.message ?: "Errore sconosciuto")
                }
        }
    }

    fun inviaRichiestaCambioScheda(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val savedCode = sharedPreferences.getString("code", "") ?: ""
        if (savedCode.isEmpty()) {
            onError(Exception("Codice utente non trovato"))
            return
        }

        val db = FirebaseDatabase.getInstance().reference
        db.child("users").child(savedCode).child("scheda").child("cambioRichiesto")
            .setValue(true)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(e) }
    }

}


class SchedaViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SchedaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SchedaViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
