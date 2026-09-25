package com.matthew.sportiliapp.newadmin

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Giorno
import com.matthew.sportiliapp.model.Utente
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import newadmin.data.FirebaseRepositoryImpl
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in test against a local RTDB emulator using a separate, demo-only FirebaseApp. */
@RunWith(AndroidJUnit4::class)
class FirebaseRepositoryEmulatorTest {
    private var app: FirebaseApp? = null
    private var database: FirebaseDatabase? = null

    @Before fun connectToLocalEmulator() {
        assumeTrue(
            "Run with npm run test:android-repository in firebase-compatibility",
            InstrumentationRegistry.getArguments().getString("sportiliLocalDatabase") == "1"
        )
        val options = FirebaseOptions.Builder()
            .setApplicationId("1:123456789:android:localtest")
            .setApiKey("fake-local-key")
            .setProjectId("demo-sportili-compat")
            .setDatabaseUrl("https://demo-sportili-compat.firebaseio.com")
            .build()
        app = FirebaseApp.initializeApp(
            InstrumentationRegistry.getInstrumentation().targetContext,
            options, "local-repository-${UUID.randomUUID()}"
        )
        database = FirebaseDatabase.getInstance(app!!).apply {
            useEmulator("10.0.2.2", 19000) // Android Emulator alias for this computer.
        }
    }

    @After fun disconnect() {
        database?.goOffline()
        app?.delete()
    }

    @Test fun profileEditPreservesServerWorkoutHistoryAndUnknownFields() = runBlocking<Unit> {
        withTimeout(15_000) {
            val db = database!!
            val userRef = db.getReference("users/native-test")
            val currentWorkout = mapOf("dataInizio" to "2026-09-01T00:00:00+0000", "durata" to 6, "cambioRichiesto" to true)
            userRef.setValue(mapOf(
                "nome" to "Prima", "cognome" to "Test", "scheda" to currentWorkout,
                "exerciseData" to mapOf("panca" to mapOf(
                    "noteUtente" to "Conservare",
                    "weightLogs" to mapOf("entry1" to mapOf("weight" to 42.5, "timestamp" to 1790000000000L))
                )), "unknownField" to "Conservare anche questo"
            )).await()
            val result = FirebaseRepositoryImpl(db).updateUser(
                Utente("native-test", "Corretto", "Nuovo", Scheda("old snapshot", 1))
            )
            assertTrue(result.toString(), result.isSuccess)
            val saved = userRef.get().await()
            assertEquals("Nuovo", saved.child("nome").value)
            assertEquals("Corretto", saved.child("cognome").value)
            assertEquals(6L, saved.child("scheda/durata").value)
            assertEquals(true, saved.child("scheda/cambioRichiesto").value)
            assertEquals("2026-09-01T00:00:00+0000", saved.child("scheda/dataInizio").value)
            assertEquals("Conservare", saved.child("exerciseData/panca/noteUtente").value)
            assertEquals(42.5, saved.child("exerciseData/panca/weightLogs/entry1/weight").value)
            assertEquals(1790000000000L, saved.child("exerciseData/panca/weightLogs/entry1/timestamp").value)
            assertEquals("Conservare anche questo", saved.child("unknownField").value)
            userRef.removeValue().await()
        }
    }

    @Test fun concurrentCreationHasExactlyOneWinner() = runBlocking<Unit> {
        withTimeout(20_000) {
            val db = database!!
            val otherApp = FirebaseApp.initializeApp(
                InstrumentationRegistry.getInstrumentation().targetContext, app!!.options, "other-${UUID.randomUUID()}"
            )
            val other = FirebaseDatabase.getInstance(otherApp).apply { useEmulator("10.0.2.2", 19000) }
            try {
                val code = "race-${UUID.randomUUID()}"
                val attempts = listOf(
                    async { FirebaseRepositoryImpl(db).addUser(Utente(code, "Test", "Uno")) },
                    async { FirebaseRepositoryImpl(other).addUser(Utente(code, "Test", "Due")) }
                ).awaitAll()
                assertEquals(1, attempts.count { it.isSuccess })
                val saved = db.getReference("users/$code").get().await()
                assertEquals(if (attempts[0].isSuccess) "Uno" else "Due", saved.child("nome").value)
                db.getReference("users/$code").removeValue().await()
            } finally { other.goOffline(); otherApp.delete() }
        }
    }

    @Test fun concurrentWorkoutEditsHaveOneWinner() = runBlocking<Unit> {
        withTimeout(20_000) {
            val db = database!!
            val otherApp = FirebaseApp.initializeApp(
                InstrumentationRegistry.getInstrumentation().targetContext, app!!.options, "editor-${UUID.randomUUID()}"
            )
            val other = FirebaseDatabase.getInstance(otherApp).apply { useEmulator("10.0.2.2", 19000) }
            try {
                val code = "edit-race-${UUID.randomUUID()}"
                val ref = db.getReference("users/$code/scheda")
                ref.setValue(mapOf("dataInizio" to "2026-09-01T12:00:00+0200", "durata" to 4,
                    "future" to Long.MAX_VALUE)).await()
                val first = FirebaseRepositoryImpl(db)
                val second = FirebaseRepositoryImpl(other)
                val a = first.getWorkoutCard(code).getOrThrow()
                val b = second.getWorkoutCard(code).getOrThrow()
                val results = listOf(async { first.updateWorkoutCard(code, a.copy(durata = 6)) },
                    async { second.updateWorkoutCard(code, b.copy(durata = 8)) }).awaitAll()
                assertEquals(1, results.count { it.isSuccess })
                assertEquals(if (results[0].isSuccess) 6L else 8L, ref.child("durata").get().await().value)
                assertEquals(Long.MAX_VALUE, ref.child("future").get().await().value)
                db.getReference("users/$code").removeValue().await()
            } finally { other.goOffline(); otherApp.delete() }
        }
    }

    @Test fun staleWorkoutMergesIndependentChangesAndRejectsConflicts() = runBlocking<Unit> {
        withTimeout(20_000) {
            val db = database!!
            val code = "stale-${UUID.randomUUID()}"
            val ref = db.getReference("users/$code")
            val exercise = mapOf("name" to "Panca", "serie" to "3x10", "noteUtente" to "prima", "future" to "keep")
            ref.setValue(mapOf("nome" to "Test", "exerciseData" to mapOf("panca" to mapOf("weightLogs" to mapOf("a" to 42))),
                "scheda" to mapOf("dataInizio" to "2026-09-01T12:00:00+0200", "durata" to 4,
                    "future" to "keep", "giorni" to mapOf("giorno1" to mapOf("name" to "A", "future" to "day",
                        "gruppiMuscolari" to mapOf("gruppo1" to mapOf("nome" to "Petto", "esercizi" to mapOf("esercizio1" to exercise))))))
            )).await()
            val repository = FirebaseRepositoryImpl(db)
            val original = repository.getWorkoutCard(code).getOrThrow()
            val notePath = "scheda/giorni/giorno1/gruppiMuscolari/gruppo1/esercizi/esercizio1/noteUtente"
            ref.updateChildren(mapOf("scheda/cambioRichiesto" to true, notePath to "nuova", "scheda/anotherFuture" to 9)).await()
            assertTrue(repository.updateWorkoutCard(code, original.copy(durata = 6)).isSuccess)
            val saved = ref.get().await()
            assertEquals(true, saved.child("scheda/cambioRichiesto").value)
            assertEquals("nuova", saved.child(notePath).value)
            assertEquals("keep", saved.child("scheda/future").value)
            assertEquals(9L, saved.child("scheda/anotherFuture").value)
            assertEquals(42L, saved.child("exerciseData/panca/weightLogs/a").value)
            assertFalse(saved.child("scheda/editBaseline").exists())
            assertTrue(repository.updateWorkoutCard(code, original.copy(durata = 8)).isFailure)
            assertTrue(repository.updateWorkoutCard(code, original.copy(giorni = emptyMap())).isFailure)
            val fresh = repository.getWorkoutCard(code).getOrThrow()
            assertTrue(repository.updateWorkoutCard(code, fresh.copy(giorni = emptyMap(), cambioRichiesto = false)).isSuccess)
            assertFalse(ref.child("scheda/giorni").get().await().exists())
            assertEquals(false, ref.child("scheda/cambioRichiesto").get().await().value)
            ref.removeValue().await()
            assertTrue(repository.updateWorkoutCard(code, fresh.copy(durata = 10)).isFailure)
            assertFalse(ref.get().await().exists())
        }
    }

    @Test fun staleDayEditorMergesIndependentChangesAndRejectsConflicts() = runBlocking<Unit> {
        withTimeout(20_000) {
            val db = database!!
            val code = "day-stale-${UUID.randomUUID()}"
            val dayRef = db.getReference("users/$code/scheda/giorni/giorno1")
            dayRef.setValue(mapOf(
                "name" to "A",
                "future" to "keep",
                "gruppiMuscolari" to mapOf("gruppo1" to mapOf(
                    "nome" to "Petto",
                    "esercizi" to mapOf("esercizio1" to mapOf(
                        "name" to "Panca", "serie" to "3x10", "noteUtente" to "prima",
                        "weightLogs" to mapOf("log1" to mapOf("weight" to 40, "timestamp" to 1790000000000L)),
                        "futureExercise" to true
                    )),
                    "futureGroup" to 7
                ))
            )).await()
            val repository = FirebaseRepositoryImpl(db)
            val original = repository.getDay(code, "giorno1").getOrThrow()
            dayRef.updateChildren(mapOf(
                "gruppiMuscolari/gruppo1/esercizi/esercizio1/noteUtente" to "remota",
                "remoteRequest" to true
            )).await()
            assertTrue(repository.updateDay(code, "giorno1", original.copy(name = "B")).isSuccess)
            val saved = dayRef.get().await()
            assertEquals("B", saved.child("name").value)
            assertEquals("remota", saved.child("gruppiMuscolari/gruppo1/esercizi/esercizio1/noteUtente").value)
            assertEquals(40L, saved.child("gruppiMuscolari/gruppo1/esercizi/esercizio1/weightLogs/log1/weight").value)
            assertEquals(true, saved.child("gruppiMuscolari/gruppo1/esercizi/esercizio1/futureExercise").value)
            assertEquals(7L, saved.child("gruppiMuscolari/gruppo1/futureGroup").value)
            assertEquals("keep", saved.child("future").value)
            assertEquals(true, saved.child("remoteRequest").value)
            assertFalse(saved.child("editBaseline").exists())
            assertTrue(repository.updateDay(code, "giorno1", original.copy(name = "C")).isFailure)

            val group = repository.getMuscleGroup(code, "giorno1", "gruppo1").getOrThrow()
            val exercise = group.esercizi.getValue("esercizio1")
            assertTrue(repository.updateExercise(
                code, "giorno1", "gruppo1", "esercizio1", exercise.copy(serie = "4x8")
            ).isSuccess)
            val savedExercise = dayRef.child("gruppiMuscolari/gruppo1/esercizi/esercizio1").get().await()
            assertEquals("4x8", savedExercise.child("serie").value)
            assertEquals("remota", savedExercise.child("noteUtente").value)
            assertEquals(40L, savedExercise.child("weightLogs/log1/weight").value)
            assertEquals(true, savedExercise.child("futureExercise").value)
            assertTrue(repository.updateExercise(
                code, "giorno1", "gruppo1", "esercizio1", exercise.copy(serie = "5x5")
            ).isFailure)
            db.getReference("users/$code").removeValue().await()
        }
    }

    @Test fun concurrentDayCreationHasExactlyOneWinner() = runBlocking<Unit> {
        withTimeout(20_000) {
            val db = database!!
            val code = "day-race-${UUID.randomUUID()}"
            val repository = FirebaseRepositoryImpl(db)
            val attempts = listOf(
                async { repository.addDay(code, "giorno1", Giorno("Uno")) },
                async { repository.addDay(code, "giorno1", Giorno("Due")) }
            ).awaitAll()
            assertEquals(1, attempts.count { it.isSuccess })
            assertTrue(dayName(db, code) in setOf("Uno", "Due"))
            db.getReference("users/$code").removeValue().await()
        }
    }

    private suspend fun dayName(db: FirebaseDatabase, code: String): String? =
        db.getReference("users/$code/scheda/giorni/giorno1/name").get().await().getValue(String::class.java)

    @Test fun deniedProfileEditReturnsFailureAndCreatesNoUser() = runBlocking<Unit> {
        withTimeout(15_000) {
            val db = database!!
            val result = FirebaseRepositoryImpl(db).updateUser(Utente("denied", "Test", "Negato"))
            assertTrue(result.isFailure)
            assertFalse(db.getReference("users/denied").get().await().exists())
        }
    }
}
