package com.matthew.sportiliapp.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow


class UserViewModel : ViewModel() {
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")

    val users = liveData {
        emitSource(dbRef.addValueEventListenerAsFlow().asLiveData())
    }
}

// Estensione per convertire ValueEventListener in un flusso di dati.
fun DatabaseReference.addValueEventListenerAsFlow() = callbackFlow {
    val listener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            try {
                val userList = snapshot.children.mapNotNull {
                    it.getValue(Utente::class.java)
                }
                trySend(userList).isSuccess
            } catch (e: Exception) {
                close(e)
            }
        }

        override fun onCancelled(error: DatabaseError) {
            close(error.toException())
        }
    }
    addValueEventListener(listener)
    awaitClose { removeEventListener(listener) }
}
