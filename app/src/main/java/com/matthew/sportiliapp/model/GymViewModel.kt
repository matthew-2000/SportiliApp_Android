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
        dbRef.child(userCode).child("scheda").setValue(schedaDict)
    }
}
