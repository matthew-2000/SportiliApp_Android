package com.matthew.sportiliapp.newadmin

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import com.matthew.sportiliapp.model.Scheda
import com.matthew.sportiliapp.model.Utente
import java.util.UUID
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

    @Test fun deniedProfileEditReturnsFailureAndCreatesNoUser() = runBlocking<Unit> {
        withTimeout(15_000) {
            val db = database!!
            val result = FirebaseRepositoryImpl(db).updateUser(Utente("denied", "Test", "Negato"))
            assertTrue(result.isFailure)
            assertFalse(db.getReference("users/denied").get().await().exists())
        }
    }
}
