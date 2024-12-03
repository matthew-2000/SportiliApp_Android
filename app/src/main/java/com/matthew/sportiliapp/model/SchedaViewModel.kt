package com.matthew.sportiliapp.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import android.content.Context
import androidx.lifecycle.ViewModelProvider
import com.google.gson.Gson
import java.security.MessageDigest

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
            isLoading.postValue(true)
            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = sharedPreferences.getString("code", "") ?: ""
            // Se il codice dell'utente è vuoto, non possiamo procedere
            if (savedCode.isEmpty()) {
                isLoading.postValue(false)
                return@launch
            }
            val localScheda = loadSchedaFromLocal()
            if (localScheda != null) {
                _scheda.postValue(localScheda)  // Carica la scheda locale
                isLoading.postValue(false)
                return@launch
            }
            // Carica i dati dal database solo se la scheda non è disponibile localmente
            val database = FirebaseDatabase.getInstance()
            val schedaRef = database.reference.child("users").child(savedCode).child("scheda")
            schedaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.getValue(Scheda::class.java)
                    if (data != null) {
                        val remoteHash = hashString(data.toString())
                        saveSchedaToLocal(data, remoteHash)  // Salva la scheda localmente
                        _scheda.postValue(data)
                    }
                    isLoading.postValue(false)
                }

                override fun onCancelled(error: DatabaseError) {
                    isLoading.postValue(false)
                }
            })
        }
    }


    // Salva la scheda in locale
    fun saveSchedaToLocal(scheda: Scheda, hash: String) {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("scheda_hash", hash)
        // Salva i dati della scheda come JSON
        editor.putString("scheda_data", Gson().toJson(scheda))
        editor.apply()
    }

    // Carica la scheda dalla memoria locale
    private fun loadSchedaFromLocal(): Scheda? {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("scheda_data", null)
        return json?.let { gson.fromJson(it, Scheda::class.java) }
    }


    // Recupera l'hash locale della scheda
    private fun getLocalSchedaHash(): String? {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        return sharedPreferences.getString("scheda_hash", null)
    }

    // Calcola l'hash di una stringa
    private fun hashString(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .fold("") { str, it -> str + "%02x".format(it) }
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