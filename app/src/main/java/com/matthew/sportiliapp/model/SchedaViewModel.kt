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
            val database = FirebaseDatabase.getInstance()

            // Riferimento all'hash remoto
            val hashRef = database.reference.child("users").child(savedCode).child("scheda_hash")
            hashRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val remoteHash = snapshot.getValue(String::class.java)
                    val localHash = getLocalSchedaHash()

                    if (remoteHash != null && remoteHash == localHash) {
                        // Carica la scheda dalla memoria locale
                        val localScheda = loadSchedaFromLocal()
                        _scheda.postValue(localScheda)
                        isLoading.postValue(false)
                    } else {
                        // Scarica la scheda dal database remoto
                        val schedaRef = database.reference.child("users").child(savedCode).child("scheda")
                        schedaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                val data = snapshot.getValue(Scheda::class.java)
                                data?.sortAll()
                                _scheda.postValue(data)

                                // Salva la scheda e l'hash in locale
                                data?.let { saveSchedaToLocal(it) }
                                isLoading.postValue(false)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                isLoading.postValue(false)
                            }
                        })
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    isLoading.postValue(false)
                }
            })

            // Carica il nome utente
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

    // Salva la scheda in locale
    private fun saveSchedaToLocal(scheda: Scheda) {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val json = gson.toJson(scheda)
        val hash = hashString(json)

        editor.putString("scheda", json)
        editor.putString("scheda_hash", hash)
        editor.apply()
    }

    // Carica la scheda dalla memoria locale
    private fun loadSchedaFromLocal(): Scheda? {
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = sharedPreferences.getString("scheda", null)
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
