package com.matthew.sportiliapp.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import android.content.Context
import androidx.lifecycle.ViewModelProvider

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

    // Funzione per aggiornare le note utente di un esercizio
    fun updateEsercizioNotes(
        giornoId: String,
        gruppoMuscolareId: String,
        esercizioId: String,
        noteUtente: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = sharedPreferences.getString("code", "") ?: ""
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

            val updates = hashMapOf<String, Any?>(
                "noteUtente" to noteUtente
            )

            esercizioRef.updateChildren(updates)
                .addOnSuccessListener {
                    onSuccess()  // Callback di successo
                }
                .addOnFailureListener { error ->
                    onFailure(error.message ?: "Errore sconosciuto")  // Callback di fallimento
                }
        }
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
