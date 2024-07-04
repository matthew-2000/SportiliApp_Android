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
    val scheda: LiveData<Scheda?> = _scheda

    init {
        loadScheda()
    }

    private fun loadScheda() {
        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = sharedPreferences.getString("code", "") ?: ""
            val database = FirebaseDatabase.getInstance()
            val schedaRef = database.reference.child("users").child(savedCode).child("scheda")

            schedaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val data = snapshot.getValue(Scheda::class.java)
                    _scheda.postValue(data)
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
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
