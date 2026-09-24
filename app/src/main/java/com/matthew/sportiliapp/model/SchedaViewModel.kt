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

class SchedaViewModel(
    private val context: Context,
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val preferences: SharedPreferences = context.getSharedPreferences("shared", Context.MODE_PRIVATE)
) : ViewModel() {
    private val gson = Gson()
    private val _scheda = MutableLiveData<Scheda?>()
    private val _name = MutableLiveData<String?>()
    private val _isOfflineMode = MutableLiveData(false)
    private val _userExerciseData = MutableLiveData<Map<String, UserExerciseData>>(emptyMap())
    val scheda: LiveData<Scheda?> = _scheda
    val name: LiveData<String?> = _name
    val isOfflineMode: LiveData<Boolean> = _isOfflineMode
    val userExerciseData: LiveData<Map<String, UserExerciseData>> = _userExerciseData
    val isLoading = MutableLiveData(true)
    val loadError = MutableLiveData<String?>(null)

    private var userCode: String? = null
    private var exerciseDataRef: DatabaseReference? = null
    private var exerciseDataListener: ValueEventListener? = null

    private var generation = 0L
    private var closed = false
    private val observations = mutableMapOf<String, Pair<DatabaseReference, ValueEventListener>>()
    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (!closed && (key == null || key == "code")) refresh()
    }

    init {
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
        refresh()
    }

    /** One subscription set for the signed-in code, also registered when starting offline. */
    fun refresh() {
        if (closed) return
        stopObserving()
        val token = generation
        val savedCode = preferences.getString("code", "").orEmpty()
        userCode = savedCode.takeIf { it.isNotBlank() }
        loadError.value = null
        _userExerciseData.value = emptyMap()
        var cacheOwner = preferences.getString(CACHED_OWNER_KEY, null)
        // Legacy logout clears these preferences, so an untagged cache belongs to
        // the existing signed-in code. Adopt it once to preserve offline upgrades.
        if (cacheOwner == null && savedCode.isNotEmpty()) {
            cacheOwner = savedCode
            preferences.edit().putString(CACHED_OWNER_KEY, savedCode).apply()
        }
        val ownsCache = cacheOwner == savedCode && savedCode.isNotEmpty()
        if (!ownsCache) {
            preferences.edit().remove(CACHED_SCHEDA_KEY).remove(CACHED_NAME_KEY).remove(CACHED_OWNER_KEY).apply()
        }
        _scheda.value = if (ownsCache) getCachedScheda(preferences)?.sortedSnapshot() else null
        _name.value = if (ownsCache) getCachedName(preferences) else null
        isLoading.value = _scheda.value == null
        _isOfflineMode.value = !isNetworkAvailable()
        if (savedCode.isBlank() || savedCode.any { it in ".#$[]/" || it.code < 32 || it.code == 127 }) {
            isLoading.value = false
            loadError.value = "Codice utente mancante o non valido. Effettua di nuovo l'accesso."
            return
        }

        observeUserExerciseData(savedCode)
        var workoutFailed = false
        var nameFailed = false
        fun current() = !closed && generation == token && preferences.getString("code", "") == savedCode

        fun observeWorkout() {
            replaceObservation("scheda", database.getReference("users/$savedCode/scheda"), { snapshot ->
                if (current()) {
                    try {
                        val sorted = snapshot.getValue(Scheda::class.java)?.sortedSnapshot()
                        _scheda.value = sorted
                        saveSchedaToCache(preferences, sorted)
                        workoutFailed = false
                        loadError.value = null
                    } catch (_: Exception) {
                        workoutFailed = true
                        loadError.value = "Impossibile leggere la scheda. Riprova con Aggiorna."
                    }
                    isLoading.value = false
                }
            }, {
                if (current()) {
                    workoutFailed = true
                    loadError.value = "Impossibile aggiornare la scheda. Riprova con Aggiorna."
                    isLoading.value = false
                }
            })
        }
        fun observeName() {
            replaceObservation("nome", database.getReference("users/$savedCode/nome"), { snapshot ->
                if (current()) {
                    val name = snapshot.getValue(String::class.java)
                    _name.value = name
                    saveNameToCache(preferences, name)
                    nameFailed = false
                }
            }, { if (current()) nameFailed = true })
        }
        observeWorkout()
        observeName()
        var wasConnected = false
        replaceObservation("connection", database.getReference(".info/connected"), { snapshot ->
            if (current()) {
                val connected = snapshot.getValue(Boolean::class.java) == true
                _isOfflineMode.value = !connected
                if (!connected) isLoading.value = false
                if (connected && !wasConnected) {
                    if (workoutFailed) observeWorkout()
                    if (nameFailed) observeName()
                }
                wasConnected = connected
            }
        }, { if (current()) { _isOfflineMode.value = true; isLoading.value = false } })
    }

    private fun replaceObservation(
        key: String, reference: DatabaseReference,
        onData: (DataSnapshot) -> Unit, onError: (DatabaseError) -> Unit
    ) {
        observations.remove(key)?.let { (ref, listener) -> ref.removeEventListener(listener) }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (observations[key]?.second === this) onData(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {
                if (observations[key]?.second === this) onError(error)
            }
        }
        observations[key] = reference to listener
        reference.addValueEventListener(listener)
    }

    private fun stopObserving() {
        generation++
        observations.values.forEach { (ref, listener) -> ref.removeEventListener(listener) }
        observations.clear()
        exerciseDataListener?.let { exerciseDataRef?.removeEventListener(it) }
        exerciseDataListener = null
        exerciseDataRef = null
    }

    private fun saveSchedaToCache(sharedPreferences: SharedPreferences, scheda: Scheda?) {
        val editor = sharedPreferences.edit().putString(CACHED_OWNER_KEY, userCode)
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
        val editor = sharedPreferences.edit().putString(CACHED_OWNER_KEY, userCode)
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
            val sharedPreferences = preferences
            val savedCode = userCode ?: sharedPreferences.getString("code", "") ?: ""
            if (savedCode.isEmpty()) {
                onFailure("Codice utente non trovato")
                return@launch
            }


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
            val sharedPreferences = preferences
            val savedCode = userCode ?: sharedPreferences.getString("code", "") ?: ""
            if (savedCode.isEmpty()) {
                onFailure("Codice utente non trovato")
                return@launch
            }


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
            val sharedPreferences = preferences
            val savedCode = userCode ?: sharedPreferences.getString("code", "") ?: ""
            if (savedCode.isEmpty()) {
                onFailure("Codice utente non trovato")
                return@launch
            }


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
            val sharedPreferences = preferences
            val savedCode = userCode ?: sharedPreferences.getString("code", "") ?: ""
            if (savedCode.isEmpty()) {
                onFailure("Codice utente non trovato")
                return@launch
            }

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
        val reference = database.reference
            .child("users")
            .child(code)
            .child("exerciseData")

        exerciseDataListener?.let { listener ->
            exerciseDataRef?.removeEventListener(listener)
        }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (closed || exerciseDataListener !== this || preferences.getString("code", "") != code) return
                val data = snapshot.children.mapNotNull { child ->
                    val key = child.key ?: return@mapNotNull null
                    val value = child.getValue(UserExerciseData::class.java) ?: UserExerciseData()
                    val hasContent = !value.noteUtente.isNullOrEmpty() || (value.weightLogs != null && value.weightLogs!!.isNotEmpty())
                    if (hasContent) key to value else null
                }.toMap()
                _userExerciseData.value = data
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignora l'errore, mantieni i dati in cache
            }
        }

        exerciseDataRef = reference
        exerciseDataListener = listener
        reference.addValueEventListener(listener)
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
        closed = true
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
        stopObserving()
        super.onCleared()
    }

    fun getCurrentUserCode(): String? {
        if (!userCode.isNullOrBlank()) {
            return userCode
        }
        val sharedPreferences = preferences
        return sharedPreferences.getString("code", null)
    }

    fun inviaRichiestaCambioScheda(
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val sharedPreferences = preferences
        val savedCode = sharedPreferences.getString("code", "") ?: ""
        if (savedCode.isEmpty()) {
            onError(Exception("Codice utente non trovato"))
            return
        }

        val db = database.reference
        db.child("users").child(savedCode).child("scheda").child("cambioRichiesto")
            .setValue(true)
            .addOnSuccessListener {
                val updatedScheda = _scheda.value?.copy(cambioRichiesto = true)
                _scheda.postValue(updatedScheda)
                saveSchedaToCache(sharedPreferences, updatedScheda)
                onSuccess()
            }
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

private const val CACHED_OWNER_KEY = "cached_workout_user_code"
