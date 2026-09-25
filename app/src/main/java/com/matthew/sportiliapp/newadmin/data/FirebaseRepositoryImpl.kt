package newadmin.data

import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.matthew.sportiliapp.model.WorkoutEditBaseline
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.matthew.sportiliapp.model.Avviso
import com.matthew.sportiliapp.model.Utente
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.GruppoMuscolare
import com.matthew.sportiliapp.model.Esercizio
import com.matthew.sportiliapp.model.WorkoutIssueReport
import com.matthew.sportiliapp.newadmin.domain.FirebaseRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class FirebaseRepositoryImpl(
    private val firebaseDatabase: FirebaseDatabase
) : FirebaseRepository {

    private val usersRef = firebaseDatabase.getReference("users")
    private val alertsRef = firebaseDatabase.getReference("alerts")
    private val reportsRef = firebaseDatabase.getReference("workoutIssueReports")

    private fun withBaseline(userCode: String, exercise: Esercizio, snapshot: DataSnapshot): Esercizio =
        exercise.copy(editBaseline = WorkoutEditBaseline(
            userCode, firebaseTree(snapshot.value), firebaseTree(exercise.toMap())
        ))

    private fun withBaselines(userCode: String, group: GruppoMuscolare, snapshot: DataSnapshot): GruppoMuscolare =
        group.copy(
            esercizi = group.esercizi.mapValues { (key, exercise) ->
                withBaseline(userCode, exercise, snapshot.child("esercizi").child(key))
            },
            editBaseline = WorkoutEditBaseline(userCode, firebaseTree(snapshot.value), firebaseTree(group.toMap()))
        )

    private fun withBaselines(userCode: String, day: Giorno, snapshot: DataSnapshot): Giorno =
        day.copy(
            gruppiMuscolari = day.gruppiMuscolari.mapValues { (key, group) ->
                withBaselines(userCode, group, snapshot.child("gruppiMuscolari").child(key))
            },
            editBaseline = WorkoutEditBaseline(userCode, firebaseTree(snapshot.value), firebaseTree(day.toMap()))
        )

    private suspend fun createIfAbsent(reference: DatabaseReference, value: Any?): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            reference.runTransaction(object : Transaction.Handler {
                override fun doTransaction(data: MutableData): Transaction.Result {
                    if (data.value != null) return Transaction.abort()
                    data.value = value
                    return Transaction.success(data)
                }

                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (!cont.isActive) return
                    cont.resume(when {
                        error != null -> Result.failure(error.toException())
                        !committed -> Result.failure(IllegalStateException("Esiste già un elemento con questo identificativo."))
                        else -> Result.success(Unit)
                    })
                }
            }, false)
        }

    private suspend fun updateFromBaseline(
        reference: DatabaseReference,
        userCode: String,
        baseline: WorkoutEditBaseline?,
        edited: Any?,
        onCommitted: (DataSnapshot) -> Unit = {}
    ): Result<Unit> = suspendCancellableCoroutine { cont ->
        if (baseline == null || baseline.userCode != userCode) {
            cont.resume(Result.failure(IllegalStateException("Riapri i dati prima di salvarli.")))
            return@suspendCancellableCoroutine
        }
        reference.runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                if (data.value == null) return Transaction.success(data)
                return try {
                    data.value = mergeEditedTree(baseline, edited, data.value)
                    Transaction.success(data)
                } catch (_: WorkoutConflict) {
                    Transaction.abort()
                }
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (!cont.isActive) return
                cont.resume(when {
                    error != null -> Result.failure(error.toException())
                    !committed || snapshot?.exists() != true -> Result.failure(WorkoutConflict())
                    else -> {
                        onCommitted(snapshot)
                        Result.success(Unit)
                    }
                })
            }
        }, false)
    }

    // --- Gestione utenti ---
    override fun getUsers(): Flow<List<Utente>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val usersList = snapshot.children.mapNotNull { ds ->
                    ds.getValue(Utente::class.java)?.apply {
                        code = ds.key ?: ""
                    }
                }.sortedBy { it.nome }
                trySend(usersList)
            }
            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }
        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }

    override suspend fun addUser(utente: Utente): Result<Unit> =
        createIfAbsent(usersRef.child(utente.code), mapOf(
            "cognome" to utente.cognome,
            "nome" to utente.nome,
            "scheda" to (utente.scheda ?: Scheda()).toMap()
        )).fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { error ->
                if (error is IllegalStateException) Result.failure(IllegalStateException("Il codice è già utilizzato. Scegli un altro codice."))
                else Result.failure(error)
            }
        )

    override suspend fun updateUser(utente: Utente): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val userRef = usersRef.child(utente.code)
            val userDict = mapOf(
                "cognome" to utente.cognome,
                "nome" to utente.nome
            )
            // Profile edits must preserve the workout and exercise history.
            userRef.updateChildren(userDict)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    override suspend fun removeUser(userCode: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            usersRef.child(userCode).removeValue()
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    // --- Gestione della Scheda (Workout Card) ---
    override suspend fun updateWorkoutCard(userCode: String, scheda: Scheda): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val baseline = scheda.editBaseline
            if (baseline == null || baseline.userCode != userCode) {
                cont.resume(Result.failure(IllegalStateException("Riapri la scheda prima di salvarla.")))
                return@suspendCancellableCoroutine
            }
            val edited = firebaseTree(scheda.toMap())
            usersRef.child(userCode).child("scheda").runTransaction(object : Transaction.Handler {
                override fun doTransaction(data: MutableData): Transaction.Result {
                    // A cold cache can supply null first. A no-op commit asks the server
                    // to validate it and retry with current data; never recreate a deleted card.
                    if (data.value == null) return Transaction.success(data)
                    return try {
                        data.value = mergeWorkout(baseline, edited, firebaseTree(data.value), scheda.dayOrigins)
                        Transaction.success(data)
                    } catch (_: WorkoutConflict) { Transaction.abort() }
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                    if (!cont.isActive) return
                    cont.resume(when {
                        error != null -> Result.failure(error.toException())
                        !committed || snapshot?.exists() != true -> Result.failure(WorkoutConflict())
                        else -> Result.success(Unit)
                    })
                }
            }, false)
        }

    override suspend fun getWorkoutCard(userCode: String): Result<Scheda> {
        return suspendCancellableCoroutine { cont ->
            val schedaRef = usersRef.child(userCode).child("scheda")
            schedaRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val scheda = snapshot.getValue(Scheda::class.java)
                    if (scheda != null) {
                        if (cont.isActive) cont.resume(Result.success(scheda.copy(editBaseline = WorkoutEditBaseline(
                            userCode, firebaseTree(snapshot.value), firebaseTree(scheda.toMap())
                        ))))
                    } else {
                        cont.resume(Result.failure(NoSuchElementException("Scheda non trovata per l'utente $userCode")))
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    cont.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    // --- Gestione dei Giorni (Day) ---

    override suspend fun getDay(userCode: String, dayKey: String): Result<Giorno> {
        return suspendCancellableCoroutine { cont ->
            val dayRef = usersRef.child(userCode).child("scheda").child("giorni").child(dayKey)
            dayRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val giorno = snapshot.getValue(Giorno::class.java)
                    if (giorno != null) {
                        cont.resume(Result.success(withBaselines(userCode, giorno, snapshot)))
                    } else {
                        cont.resume(
                            Result.failure(
                                NoSuchElementException("Giorno $dayKey non trovato per l'utente $userCode")
                            )
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    cont.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    override suspend fun addDay(userCode: String, dayKey: String, giorno: Giorno): Result<Unit> =
        createIfAbsent(usersRef.child(userCode).child("scheda").child("giorni").child(dayKey), giorno.toMap())

    override suspend fun updateDay(userCode: String, dayKey: String, giorno: Giorno): Result<Unit> =
        updateFromBaseline(
            usersRef.child(userCode).child("scheda").child("giorni").child(dayKey),
            userCode, giorno.editBaseline, giorno.toMap()
        ) { snapshot ->
            giorno.editBaseline = WorkoutEditBaseline(
                userCode, firebaseTree(snapshot.value), firebaseTree(giorno.toMap())
            )
        }

    override suspend fun removeDay(userCode: String, dayKey: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            usersRef.child(userCode).child("scheda").child("giorni").child(dayKey)
                .removeValue()
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    // --- Gestione dei Gruppi Muscolari (Muscle Group) ---
    override suspend fun getMuscleGroup(
        userCode: String,
        dayKey: String,
        muscleGroupKey: String
    ): Result<GruppoMuscolare> {
        return suspendCancellableCoroutine { cont ->
            val groupRef = usersRef.child(userCode).child("scheda").child("giorni").child(dayKey)
                .child("gruppiMuscolari").child(muscleGroupKey)
            groupRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gruppo = snapshot.getValue(GruppoMuscolare::class.java)
                    if (gruppo != null) {
                        cont.resume(Result.success(withBaselines(userCode, gruppo, snapshot)))
                    } else {
                        cont.resume(
                            Result.failure(
                                NoSuchElementException(
                                    "Gruppo muscolare $muscleGroupKey non trovato per il giorno $dayKey"
                                )
                            )
                        )
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    cont.resume(Result.failure(Exception(error.message)))
                }
            })
        }
    }

    override suspend fun addMuscleGroup(userCode: String, dayKey: String, muscleGroupKey: String, gruppo: GruppoMuscolare): Result<Unit> =
        createIfAbsent(
            usersRef.child(userCode).child("scheda").child("giorni").child(dayKey)
                .child("gruppiMuscolari").child(muscleGroupKey), gruppo.toMap()
        )

    override suspend fun updateMuscleGroup(userCode: String, dayKey: String, muscleGroupKey: String, gruppo: GruppoMuscolare): Result<Unit> =
        updateFromBaseline(
            usersRef.child(userCode).child("scheda").child("giorni").child(dayKey)
                .child("gruppiMuscolari").child(muscleGroupKey),
            userCode, gruppo.editBaseline, gruppo.toMap()
        ) { snapshot ->
            gruppo.editBaseline = WorkoutEditBaseline(
                userCode, firebaseTree(snapshot.value), firebaseTree(gruppo.toMap())
            )
        }

    override suspend fun removeMuscleGroup(userCode: String, dayKey: String, muscleGroupKey: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            usersRef.child(userCode).child("scheda").child("giorni").child(dayKey)
                .child("gruppiMuscolari").child(muscleGroupKey)
                .removeValue()
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    // --- Gestione degli Esercizi (Exercise) ---
    override suspend fun addExercise(userCode: String, dayKey: String, muscleGroupKey: String, exerciseKey: String, esercizio: Esercizio): Result<Unit> =
        createIfAbsent(
            usersRef.child(userCode).child("scheda").child("giorni").child(dayKey)
                .child("gruppiMuscolari").child(muscleGroupKey).child("esercizi").child(exerciseKey),
            esercizio.toMap()
        )

    override suspend fun updateExercise(userCode: String, dayKey: String, muscleGroupKey: String, exerciseKey: String, esercizio: Esercizio): Result<Unit> =
        updateFromBaseline(
            usersRef.child(userCode).child("scheda").child("giorni").child(dayKey)
                .child("gruppiMuscolari").child(muscleGroupKey).child("esercizi").child(exerciseKey),
            userCode, esercizio.editBaseline, esercizio.toMap()
        ) { snapshot ->
            esercizio.editBaseline = WorkoutEditBaseline(
                userCode, firebaseTree(snapshot.value), firebaseTree(esercizio.toMap())
            )
        }

    override suspend fun removeExercise(userCode: String, dayKey: String, muscleGroupKey: String, exerciseKey: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            usersRef.child(userCode).child("scheda").child("giorni").child(dayKey)
                .child("gruppiMuscolari").child(muscleGroupKey)
                .child("esercizi").child(exerciseKey)
                .removeValue()
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    // --- Gestione Avvisi ---
    override fun getAlerts(): Flow<List<Avviso>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val alerts = snapshot.children.mapNotNull { ds ->
                    val alert = ds.getValue(Avviso::class.java)?.apply {
                        id = ds.key ?: ""
                    }
                    alert?.takeUnless { it.isExpired(now) }
                }.sortedWith(
                    compareByDescending<Avviso> { it.urgencyWeight() }
                        .thenBy { it.scadenza ?: Long.MAX_VALUE }
                        .thenBy { it.titolo }
                )
                trySend(alerts)
            }

            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }
        alertsRef.addValueEventListener(listener)
        awaitClose { alertsRef.removeEventListener(listener) }
    }

    override suspend fun addAlert(avviso: Avviso): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val targetRef = if (avviso.id.isBlank()) alertsRef.push() else alertsRef.child(avviso.id)
            val id = targetRef.key ?: avviso.id
            val alertToSave = avviso.copy(id = id)
            targetRef.setValue(alertToSave)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    override suspend fun updateAlert(avviso: Avviso): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            if (avviso.id.isBlank()) {
                cont.resume(Result.failure(IllegalArgumentException("Alert id cannot be empty")))
            } else {
                alertsRef.child(avviso.id)
                    .setValue(avviso)
                    .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                    .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
            }
        }

    override suspend fun removeAlert(alertId: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            alertsRef.child(alertId)
                .removeValue()
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    // --- Segnalazioni problemi scheda ---
    override fun getWorkoutIssueReports(): Flow<List<WorkoutIssueReport>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reports = snapshot.children.mapNotNull { ds ->
                    ds.getValue(WorkoutIssueReport::class.java)?.apply {
                        id = ds.key ?: id
                    }
                }.sortedWith(
                    compareBy<WorkoutIssueReport> { it.resolved }
                        .thenByDescending { it.createdAt }
                )
                trySend(reports)
            }

            override fun onCancelled(error: DatabaseError) {
                close(Exception(error.message))
            }
        }
        reportsRef.addValueEventListener(listener)
        awaitClose { reportsRef.removeEventListener(listener) }
    }

    override suspend fun addWorkoutIssueReport(report: WorkoutIssueReport): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val targetRef = if (report.id.isBlank()) reportsRef.push() else reportsRef.child(report.id)
            val id = targetRef.key ?: report.id
            val reportToSave = report.copy(id = id)
            targetRef.setValue(reportToSave)
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }

    override suspend fun updateWorkoutIssueReport(report: WorkoutIssueReport): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            if (report.id.isBlank()) {
                cont.resume(Result.failure(IllegalArgumentException("Report id cannot be empty")))
            } else {
                reportsRef.child(report.id)
                    .setValue(report)
                    .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                    .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
            }
        }

    override suspend fun removeWorkoutIssueReport(reportId: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            reportsRef.child(reportId)
                .removeValue()
                .addOnSuccessListener { cont.resume(Result.success(Unit)) }
                .addOnFailureListener { e -> cont.resume(Result.failure(e)) }
        }
}
