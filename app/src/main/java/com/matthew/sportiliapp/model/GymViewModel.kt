package com.matthew.sportiliapp.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.LiveData
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class GymViewModel : ViewModel() {
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")
    private val _users = MutableLiveData<List<Utente>>()
    val users: LiveData<List<Utente>> = _users

    init {
        fetchUsers()
    }

    private fun fetchUsers() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newUsers = snapshot.children.mapNotNull { dataSnapshot ->
                    dataSnapshot.getValue(Utente::class.java)?.also { user ->
                        user.code = dataSnapshot.key ?: ""
                    }
                }
                newUsers.forEach { utente ->
                    utente.scheda?.sortAll()
                }
                _users.value = newUsers.sortedWith(compareBy { it.nome })
            }

            override fun onCancelled(error: DatabaseError) {
                // Log or handle the database error
            }
        })
    }

    fun addUser(code: String, cognome: String, nome: String) {
        val userRef = dbRef.child(code)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    val userDict = mapOf("cognome" to cognome, "nome" to nome)
                    userRef.setValue(userDict)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Log or handle the database error
            }
        })
    }

    fun removeUser(code: String) {
        dbRef.child(code).removeValue()
    }

    fun updateUser(utente: Utente) {
        val userDict = mapOf(
            "cognome" to utente.cognome,
            "nome" to utente.nome,
            "scheda" to utente.scheda?.toMap()
        )
        dbRef.child(utente.code).setValue(userDict)
    }

    fun saveScheda(scheda: Scheda, userCode: String) {
        val schedaDict = scheda.toMap()
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userCode).child("scheda")
        dbRef.setValue(schedaDict)
            .addOnSuccessListener {
                // Operazione completata con successo
            }
            .addOnFailureListener { exception ->
                // Gestisci gli errori qui
            }
    }

    fun addGruppoMuscolare(
        userCode: String,
        giornoName: String,
        gruppoMuscolare: GruppoMuscolare,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val gruppoMuscolareDict = gruppoMuscolare.toMap()
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
            .child(userCode).child("scheda").child("giorni").child(giornoName)
            .child("gruppiMuscolari").child(gruppoMuscolare.nome)

        dbRef.setValue(gruppoMuscolareDict)
            .addOnSuccessListener {
                // Successo
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Gestione dell'errore
                onFailure(exception)
            }
    }

    fun removeGruppoMuscolare(
        userCode: String,
        giornoName: String,
        gruppoName: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
            .child(userCode).child("scheda").child("giorni").child(giornoName)
            .child("gruppiMuscolari").child(gruppoName)

        dbRef.removeValue()
            .addOnSuccessListener {
                // Successo
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Gestione dell'errore
                onFailure(exception)
            }
    }

    fun updateGruppoMuscolare(
        userCode: String,
        giornoName: String,
        gruppoName: String,
        nuovoGruppoMuscolare: GruppoMuscolare,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val nuovoGruppoMuscolareDict = nuovoGruppoMuscolare.toMap()
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
            .child(userCode).child("scheda").child("giorni").child(giornoName)
            .child("gruppiMuscolari").child(gruppoName)

        dbRef.setValue(nuovoGruppoMuscolareDict)
            .addOnSuccessListener {
                // Successo
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Gestione dell'errore
                onFailure(exception)
            }
    }

    fun addEsercizio(
        userCode: String,
        giornoName: String,
        gruppoName: String,
        esercizio: Esercizio,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val esercizioDict = esercizio.toMap()
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
            .child(userCode).child("scheda").child("giorni").child(giornoName)
            .child("gruppiMuscolari").child(gruppoName).child("esercizi").child(esercizio.name)

        dbRef.setValue(esercizioDict)
            .addOnSuccessListener {
                // Successo
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Gestione dell'errore
                onFailure(exception)
            }
    }

    fun removeEsercizio(
        userCode: String,
        giornoName: String,
        gruppoName: String,
        esercizioName: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
            .child(userCode).child("scheda").child("giorni").child(giornoName)
            .child("gruppiMuscolari").child(gruppoName).child("esercizi").child(esercizioName)

        dbRef.removeValue()
            .addOnSuccessListener {
                // Successo
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Gestione dell'errore
                onFailure(exception)
            }
    }

    fun updateEsercizio(
        userCode: String,
        giornoName: String,
        gruppoName: String,
        esercizioName: String,
        nuovoEsercizio: Esercizio,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val nuovoEsercizioDict = nuovoEsercizio.toMap()
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
            .child(userCode).child("scheda").child("giorni").child(giornoName)
            .child("gruppiMuscolari").child(gruppoName).child("esercizi").child(esercizioName)

        dbRef.setValue(nuovoEsercizioDict)
            .addOnSuccessListener {
                // Successo
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Gestione dell'errore
                onFailure(exception)
            }
    }

}
