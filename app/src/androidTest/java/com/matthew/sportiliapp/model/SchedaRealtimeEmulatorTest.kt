package com.matthew.sportiliapp.model

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.database.FirebaseDatabase
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

/** Real SDK, separate demo Firebase apps and isolated preferences; no default production app. */
@RunWith(AndroidJUnit4::class)
class SchedaRealtimeEmulatorTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val apps = mutableListOf<FirebaseApp>()
    private val databases = mutableListOf<FirebaseDatabase>()
    private val stores = mutableListOf<ViewModelStore>()
    private lateinit var preferences: SharedPreferences
    private var lastModel: SchedaViewModel? = null
    private lateinit var reader: FirebaseDatabase
    private lateinit var writer: FirebaseDatabase

    private fun database(): FirebaseDatabase {
        val options = FirebaseOptions.Builder().setApplicationId("1:123456789:android:localtest")
            .setApiKey("fake-local-key").setProjectId("demo-sportili-compat")
            .setDatabaseUrl("https://demo-sportili-compat.firebaseio.com").build()
        val app = FirebaseApp.initializeApp(instrumentation.targetContext, options, "realtime-${UUID.randomUUID()}")
        apps += app
        return FirebaseDatabase.getInstance(app).apply { useEmulator("10.0.2.2", 19000); databases += this }
    }
    private fun <T> main(action: () -> T): T {
        val result = AtomicReference<T>()
        instrumentation.runOnMainSync { result.set(action()) }
        return result.get()
    }
    private fun viewModel(): Pair<ViewModelStore, SchedaViewModel> = main {
        val store = ViewModelStore().also { stores += it }
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return SchedaViewModel(instrumentation.targetContext, reader, preferences) as T
            }
        }
        store to ViewModelProvider(store, factory)[SchedaViewModel::class.java].also { lastModel = it }
    }
    private suspend fun until(label: String, condition: () -> Boolean) {
        try {
            withTimeout(12_000) { while (!main(condition)) delay(25) }
        } catch (error: kotlinx.coroutines.TimeoutCancellationException) {
            throw AssertionError("$label: " + main {
                "card=${lastModel?.scheda?.value}, loading=${lastModel?.isLoading?.value}, offline=${lastModel?.isOfflineMode?.value}, error=${lastModel?.loadError?.value}"
            }, error)
        }
    }
    private fun payload(duration: Int) = mapOf("nome" to "Test locale", "scheda" to mapOf(
        "dataInizio" to "2026-09-01T12:00:00+0200", "durata" to duration,
        "giorni" to mapOf("giorno3" to mapOf("name" to "Giorno reale")), "unknown" to "keep"))

    @Before fun setup() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("sportiliLocalDatabase") == "1")
        reader = database(); writer = database()
        // Complete SDK initialization before teardown can delete an unused fixture app.
        runBlocking { withTimeout(15_000) {
            databases.forEach { it.getReference("users/test-ready").get().await() }
        } }
        preferences = instrumentation.targetContext.getSharedPreferences("realtime-${UUID.randomUUID()}", Context.MODE_PRIVATE)
    }
    @After fun teardown() {
        main { stores.forEach { it.clear() } }
        if (::preferences.isInitialized) preferences.edit().clear().commit()
        databases.forEach { it.goOffline() }
        apps.forEach { it.delete() }
    }

    @Test fun offlineStartupReconnectLiveAdminUpdateRefreshAndRelease() = runBlocking<Unit> {
        val code = "live-${UUID.randomUUID()}"
        val ref = writer.getReference("users/$code")
        ref.setValue(payload(4)).await()
        main { preferences.edit().putString("code", code).commit() }
        reader.goOffline()
        val (store, vm) = viewModel()
        until("vm.isOfflineMode.value == true && vm.isLoading.value == false") { vm.isOfflineMode.value == true && vm.isLoading.value == false }
        assertNull(main { vm.scheda.value })
        reader.goOnline()
        until("vm.scheda.value?.durata == 4 && vm.isOfflineMode.value == false") { vm.scheda.value?.durata == 4 && vm.isOfflineMode.value == false }
        main { repeat(5) { vm.refresh() } }
        until("vm.scheda.value?.durata == 4") { vm.scheda.value?.durata == 4 }
        ref.child("scheda/durata").setValue(6).await()
        until("vm.scheda.value?.durata == 6") { vm.scheda.value?.durata == 6 }
        assertEquals(setOf("giorno3"), main { vm.scheda.value!!.giorni.keys })
        assertEquals("keep", ref.child("scheda/unknown").get().await().value)
        reader.goOffline()
        until("vm.isOfflineMode.value == true") { vm.isOfflineMode.value == true }
        main { store.clear() }
        val (replacementStore, replacement) = viewModel()
        assertEquals(6, main { replacement.scheda.value!!.durata })
        ref.child("scheda/durata").setValue(8).await()
        reader.goOnline()
        until("replacement.scheda.value?.durata == 8") { replacement.scheda.value?.durata == 8 }
        main { replacementStore.clear() }
        ref.child("scheda/durata").setValue(10).await()
        delay(250)
        assertEquals(8, main { replacement.scheda.value!!.durata })
        assertEquals(6, main { vm.scheda.value!!.durata })
        ref.removeValue().await()
    }

    @Test fun changingUserAndRemoteDeletionCannotShowPreviousCard() = runBlocking<Unit> {
        val first = "first-${UUID.randomUUID()}"
        val second = "second-${UUID.randomUUID()}"
        writer.getReference("users/$first").setValue(payload(4)).await()
        writer.getReference("users/$second").setValue(payload(9)).await()
        main { preferences.edit().putString("code", first).commit() }
        val (_, vm) = viewModel()
        until("vm.scheda.value?.durata == 4") { vm.scheda.value?.durata == 4 }
        reader.goOffline()
        main { preferences.edit().putString("code", second).commit() }
        until("vm.scheda.value == null && vm.isLoading.value == false") { vm.scheda.value == null && vm.isLoading.value == false }
        reader.goOnline()
        until("vm.scheda.value?.durata == 9") { vm.scheda.value?.durata == 9 }
        writer.getReference("users/$first/scheda/durata").setValue(12).await()
        delay(250)
        assertEquals(9, main { vm.scheda.value!!.durata })
        writer.getReference("users/$second/scheda").removeValue().await()
        until("vm.scheda.value == null") { vm.scheda.value == null }
        assertNull(preferences.getString("cached_scheda", null))
        main { preferences.edit().remove("code").commit() }
        until("vm.isLoading.value == false && vm.scheda.value == null") { vm.isLoading.value == false && vm.scheda.value == null }
        writer.getReference("users/$first").removeValue().await()
        writer.getReference("users/$second").removeValue().await()
    }

    @Test fun legacyCacheRemainsAvailableWhenUpgradingOffline() = runBlocking<Unit> {
        reader.goOffline()
        main { preferences.edit().putString("code", "legacy-local")
            .putString("cached_scheda", com.google.gson.Gson().toJson(Scheda("2026-09-01T12:00:00+0200", 7)))
            .putString("cached_user_name", "Nome offline").commit() }
        val (_, vm) = viewModel()
        until("legacy cache available") { vm.scheda.value?.durata == 7 && vm.isLoading.value == false }
        assertEquals("Nome offline", main { vm.name.value })
        assertEquals("legacy-local", preferences.getString("cached_workout_user_code", null))
    }

    @Test fun deniedReadEndsLoadingAndExplicitRetryIsAvailable() = runBlocking<Unit> {
        main { preferences.edit().putString("code", "read-denied").commit() }
        val (_, vm) = viewModel()
        until("vm.loadError.value != null && vm.isLoading.value == false") { vm.loadError.value != null && vm.isLoading.value == false }
        main { vm.refresh() }
        until("vm.loadError.value != null && vm.isLoading.value == false") { vm.loadError.value != null && vm.isLoading.value == false }
        assertNull(main { vm.scheda.value })
    }
}
