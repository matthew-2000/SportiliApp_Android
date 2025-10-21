package com.matthew.sportiliapp.model

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.*
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.util.Locale

class SchedaViewModel(private val context: Context) : ViewModel() {
    private val gson = Gson()
    private val _scheda = MutableLiveData<Scheda?>()
    private val _name = MutableLiveData<String?>()
    private val _isOfflineMode = MutableLiveData(false)
    private val _userExerciseData = MutableLiveData<Map<String, UserExerciseData>>(emptyMap())
    val scheda: LiveData<Scheda?> = _scheda
    val name: LiveData<String?> = _name
    val isOfflineMode: LiveData<Boolean> = _isOfflineMode
    val userExerciseData: LiveData<Map<String, UserExerciseData>> = _userExerciseData
    val isLoading = MutableLiveData(true) // Stato di caricamento

    private var userCode: String? = null
    private var exerciseDataRef: DatabaseReference? = null
    private var exerciseDataListener: ValueEventListener? = null

    init {
        loadScheda()
    }

    private fun loadScheda() {
        viewModelScope.launch {
            // Imposta lo stato di caricamento a true di default
            isLoading.postValue(true)

            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val cachedScheda = getCachedScheda(sharedPreferences)?.apply { sortAll() }
            val cachedName = getCachedName(sharedPreferences)

            if (cachedScheda != null) {
                _scheda.postValue(cachedScheda)
            }
            if (cachedName != null) {
                _name.postValue(cachedName)
            }

            if (cachedScheda != null || cachedName != null) {
                // Mostra immediatamente i dati in cache mentre attendiamo la rete
                isLoading.postValue(false)
            }

            val savedCode = sharedPreferences.getString("code", "") ?: ""
            if (savedCode.isEmpty()) {
                if (cachedScheda == null && cachedName == null) {
                    isLoading.postValue(false)
                }
                _isOfflineMode.postValue(true)
                return@launch
            }

            userCode = savedCode
            observeUserExerciseData(savedCode)

            if (!isNetworkAvailable()) {
                if (cachedScheda == null && cachedName == null) {
                    isLoading.postValue(false)
                }
                _isOfflineMode.postValue(true)
                return@launch
            }

            _isOfflineMode.postValue(false)

            val database = FirebaseDatabase.getInstance()

            val schedaRef = database.reference.child("users").child(savedCode).child("scheda")
            schedaRef.get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val snapshot = task.result
                        val data = snapshot.getValue(Scheda::class.java)
                        data?.sortAll()
                        _scheda.postValue(data)
                        saveSchedaToCache(sharedPreferences, data)
                        _isOfflineMode.postValue(false)
                    } else {
                        val fallbackScheda = getCachedScheda(sharedPreferences)?.apply { sortAll() }
                        if (fallbackScheda != null) {
                            _scheda.postValue(fallbackScheda)
                        }
                        _isOfflineMode.postValue(true)
                    }
                    // Caricamento completato, imposta isLoading a false
                    isLoading.postValue(false)
                }

            val nameRef = database.reference.child("users").child(savedCode).child("nome")
            nameRef.get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val data = task.result.getValue(String::class.java)
                        _name.postValue(data)
                        saveNameToCache(sharedPreferences, data)
                    } else {
                        val fallbackName = getCachedName(sharedPreferences)
                        if (fallbackName != null) {
                            _name.postValue(fallbackName)
                        }
                    }
                }
        }
    }

    private fun saveSchedaToCache(sharedPreferences: SharedPreferences, scheda: Scheda?) {
        val editor = sharedPreferences.edit()
        if (scheda != null) {
            editor.putString(CACHED_SCHEDA_KEY, gson.toJson(scheda))
        } else {
            editor.remove(CACHED_SCHEDA_KEY)
        }
        editor.apply()
    }

    private fun getCachedScheda(sharedPreferences: SharedPreferences): Scheda? {
        val json = sharedPreferences.getString(CACHED_SCHEDA_KEY, null) ?: return null
        return try {
            gson.fromJson(json, Scheda::class.java)
        } catch (e: Exception) {
            null
        }
    }

    private fun saveNameToCache(sharedPreferences: SharedPreferences, name: String?) {
        val editor = sharedPreferences.edit()
        if (!name.isNullOrEmpty()) {
            editor.putString(CACHED_NAME_KEY, name)
        } else {
            editor.remove(CACHED_NAME_KEY)
        }
        editor.apply()
    }

    private fun getCachedName(sharedPreferences: SharedPreferences): String? {
        return sharedPreferences.getString(CACHED_NAME_KEY, null)
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } else {
            @Suppress("DEPRECATION")
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            @Suppress("DEPRECATION")
            activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }

    fun addWeightEntry(
        exerciseKey: String,
        weight: Double,
        onSuccess: (String, WeightLogEntry) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (weight <= 0) {
            onFailure("Il peso deve essere maggiore di zero")
            return
        }

        if (!isNetworkAvailable()) {
            onFailure("Connessione internet assente. Impossibile salvare il peso.")
            return
        }

        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = userCode ?: sharedPreferences.getString("code", "") ?: ""
            if (savedCode.isEmpty()) {
                onFailure("Codice utente non trovato")
                return@launch
            }

            val database = FirebaseDatabase.getInstance()

            val esercizioRef = database.reference
                .child("users")
                .child(savedCode)
                .child("exerciseData")
                .child(exerciseKey)

            val timestamp = System.currentTimeMillis()
            val entry = WeightLogEntry(weight = weight, timestamp = timestamp)
            val entryData = hashMapOf<String, Any?>(
                "weight" to weight,
                "timestamp" to timestamp
            )

            val weightLogRef = esercizioRef.child("weightLogs").push()
            weightLogRef.setValue(entryData)
                .addOnSuccessListener {
                    val entryId = weightLogRef.key
                    if (entryId != null) {
                        updateLocalExerciseData(exerciseKey) { current ->
                            val updatedLogs = (current.weightLogs ?: emptyMap()).toMutableMap()
                            updatedLogs[entryId] = entry
                            current.copy(weightLogs = updatedLogs)
                        }
                        onSuccess(entryId, entry)
                    } else {
                        onFailure("Impossibile ottenere l'identificativo del peso salvato")
                    }
                }
                .addOnFailureListener { error ->
                    onFailure(error.message ?: "Errore sconosciuto")
                }
        }
    }

    fun updateWeightEntry(
        exerciseKey: String,
        entryId: String,
        newWeight: Double,
        onSuccess: (WeightLogEntry) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (newWeight <= 0) {
            onFailure("Il peso deve essere maggiore di zero")
            return
        }

        if (!isNetworkAvailable()) {
            onFailure("Connessione internet assente. Impossibile modificare il peso.")
            return
        }

        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = userCode ?: sharedPreferences.getString("code", "") ?: ""
            if (savedCode.isEmpty()) {
                onFailure("Codice utente non trovato")
                return@launch
            }

            val database = FirebaseDatabase.getInstance()

            val esercizioRef = database.reference
                .child("users")
                .child(savedCode)
                .child("exerciseData")
                .child(exerciseKey)

            val timestamp = System.currentTimeMillis()
            val entry = WeightLogEntry(weight = newWeight, timestamp = timestamp)
            val entryData = hashMapOf<String, Any?>(
                "weight" to newWeight,
                "timestamp" to timestamp
            )

            esercizioRef.child("weightLogs").child(entryId).setValue(entryData)
                .addOnSuccessListener {
                    updateLocalExerciseData(exerciseKey) { current ->
                        val updatedLogs = (current.weightLogs ?: emptyMap()).toMutableMap()
                        updatedLogs[entryId] = entry
                        current.copy(weightLogs = updatedLogs)
                    }
                    onSuccess(entry)
                }
                .addOnFailureListener { error ->
                    onFailure(error.message ?: "Errore sconosciuto")
                }
        }
    }

    fun deleteWeightEntry(
        exerciseKey: String,
        entryId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isNetworkAvailable()) {
            onFailure("Connessione internet assente. Impossibile eliminare il peso.")
            return
        }

        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = userCode ?: sharedPreferences.getString("code", "") ?: ""
            if (savedCode.isEmpty()) {
                onFailure("Codice utente non trovato")
                return@launch
            }

            val database = FirebaseDatabase.getInstance()

            val esercizioRef = database.reference
                .child("users")
                .child(savedCode)
                .child("exerciseData")
                .child(exerciseKey)

            esercizioRef.child("weightLogs").child(entryId).removeValue()
                .addOnSuccessListener {
                    updateLocalExerciseData(exerciseKey) { current ->
                        val updatedLogs = (current.weightLogs ?: emptyMap()).toMutableMap()
                        updatedLogs.remove(entryId)
                        current.copy(weightLogs = if (updatedLogs.isEmpty()) null else updatedLogs)
                    }
                    onSuccess()
                }
                .addOnFailureListener { error ->
                    onFailure(error.message ?: "Errore sconosciuto")
                }
        }
    }

    fun updateUserNote(
        exerciseKey: String,
        newNote: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!isNetworkAvailable()) {
            onFailure("Connessione internet assente. Impossibile salvare la nota.")
            return
        }

        viewModelScope.launch {
            val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
            val savedCode = userCode ?: sharedPreferences.getString("code", "") ?: ""
            if (savedCode.isEmpty()) {
                onFailure("Codice utente non trovato")
                return@launch
            }

            val database = FirebaseDatabase.getInstance()
            val esercizioRef = database.reference
                .child("users")
                .child(savedCode)
                .child("exerciseData")
                .child(exerciseKey)

            val task = if (newNote.isNullOrBlank()) {
                esercizioRef.child("noteUtente").removeValue()
            } else {
                esercizioRef.child("noteUtente").setValue(newNote)
            }

            task.addOnSuccessListener {
                updateLocalExerciseData(exerciseKey) { current ->
                    current.copy(noteUtente = newNote?.takeIf { it.isNotBlank() })
                }
                onSuccess()
            }.addOnFailureListener { error ->
                onFailure(error.message ?: "Errore sconosciuto")
            }
        }
    }

    fun exerciseKeyFromName(name: String): String {
        val normalized = name.trim().lowercase(Locale.getDefault())
        val sanitized = normalized.replace("[^a-z0-9]+".toRegex(), "_").trim('_')
        return if (sanitized.isNotEmpty()) {
            sanitized
        } else {
            "exercise_${name.trim().hashCode()}"
        }
    }

    private fun observeUserExerciseData(code: String) {
        val database = FirebaseDatabase.getInstance()
        val reference = database.reference
            .child("users")
            .child(code)
            .child("exerciseData")

        exerciseDataListener?.let { listener ->
            exerciseDataRef?.removeEventListener(listener)
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.children.mapNotNull { child ->
                    val key = child.key ?: return@mapNotNull null
                    val value = child.getValue(UserExerciseData::class.java) ?: UserExerciseData()
                    val hasContent = !value.noteUtente.isNullOrEmpty() || (value.weightLogs != null && value.weightLogs!!.isNotEmpty())
                    if (hasContent) key to value else null
                }.toMap()
                _userExerciseData.postValue(data)
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignora l'errore, mantieni i dati in cache
            }
        }

        reference.addValueEventListener(listener)
        exerciseDataRef = reference
        exerciseDataListener = listener
    }

    private fun updateLocalExerciseData(
        exerciseKey: String,
        transform: (UserExerciseData) -> UserExerciseData
    ) {
        val currentMap = _userExerciseData.value?.toMutableMap() ?: mutableMapOf()
        val baseData = currentMap[exerciseKey] ?: UserExerciseData()
        val updatedData = transform(baseData)
        if (updatedData.noteUtente.isNullOrEmpty() && (updatedData.weightLogs == null || updatedData.weightLogs!!.isEmpty())) {
            currentMap.remove(exerciseKey)
        } else {
            currentMap[exerciseKey] = updatedData
        }
        _userExerciseData.postValue(currentMap.toMap())
    }

    override fun onCleared() {
        super.onCleared()
        exerciseDataListener?.let { listener ->
            exerciseDataRef?.removeEventListener(listener)
        }
    }

    fun getCurrentUserCode(): String? {
        if (!userCode.isNullOrBlank()) {
            return userCode
        }
        val sharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
        return sharedPreferences.getString("code", null)
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

private const val CACHED_SCHEDA_KEY = "cached_scheda"
private const val CACHED_NAME_KEY = "cached_user_name"
