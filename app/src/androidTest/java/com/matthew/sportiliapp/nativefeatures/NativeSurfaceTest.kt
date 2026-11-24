package com.matthew.sportiliapp.nativefeatures

import android.content.Context
import android.content.Intent
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Local fixtures only: no sign-in and no Firebase reads/writes. */
@RunWith(AndroidJUnit4::class)
class NativeSurfaceTest {
    // NotificationManager enqueues publication in system_server after our receiver returns.
    private fun awaitRestNotifications(c: Context): List<android.service.notification.StatusBarNotification> {
        val manager = c.getSystemService(NotificationManager::class.java)
        val limit = android.os.SystemClock.elapsedRealtime() + 3000
        var active = manager.activeNotifications.filter { it.id == 7301 }
        while (RestTimer.allowed(c) && active.none { it.notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE) == "Recupero terminato" }
            && android.os.SystemClock.elapsedRealtime() < limit) {
            Thread.sleep(25)
            active = manager.activeNotifications.filter { it.id == 7301 }
        }
        return active
    }
    @Test fun deadlineReplacementCancellationAndSessionClear() {
        val c = InstrumentationRegistry.getInstrumentation().targetContext
        val shared = c.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val oldCode = shared.getString("code", null)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
        try {
            shared.edit().putString("code", "LOCAL_NATIVE_QA").commit()
            RestTimer.start(c, 30, exerciseName = "Panca piana")
            val first = RestTimer.deadline(c)
            RestTimer.restore(c)
            assertEquals(first, RestTimer.deadline(c))
            assertTrue(RestTimer.remaining(c) in 29..30)
            RestTimer.start(c, 60, exerciseName = "Panca piana")
            val second = RestTimer.deadline(c)
            assertTrue(second > first)
            RestReceiver().onReceive(c, Intent().setAction("finish").putExtra("deadline", first))
            assertEquals(second, RestTimer.deadline(c))
            RestReceiver().onReceive(c, Intent().setAction("extend").putExtra("deadline", second))
            assertTrue(RestTimer.remaining(c) in 89..90)
            assertEquals(90, RestTimer.total(c))
            assertEquals("Panca piana", RestTimer.exerciseName(c))
            RestTimer.clear(c)
            assertEquals(0, RestTimer.remaining(c))
            assertNull(RestTimer.exerciseName(c))
            RestReceiver().onReceive(c, Intent().setAction("finish").putExtra("deadline", second))
            assertFalse(c.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == 7301 })
            RestTimer.start(c, 30)
            WorkoutSurface.save(c, "local-day", com.matthew.sportiliapp.model.Giorno(name = "Giorno A"))
            val previousSession = WorkoutSurface.session(c)
            shared.edit().putString("code", "LOCAL_NATIVE_OTHER").commit()
            assertEquals(0, RestTimer.deadline(c))
            assertNotEquals(previousSession, WorkoutSurface.session(c))
            assertNull(c.getSharedPreferences("workout_surface", Context.MODE_PRIVATE).getString("id", null))
            RestTimer.start(c, 1, exerciseName = "Rematore")
            val end = RestTimer.deadline(c)
            Thread.sleep(1200)
            RestTimer.finish(c, end)
            assertEquals(0L, RestTimer.deadline(c))
            val delivered = awaitRestNotifications(c)
            assertEquals(if (RestTimer.allowed(c)) 1 else 0, delivered.size)
            if (delivered.isNotEmpty()) {
                val text = delivered.single().notification.extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT).toString()
                assertTrue(text.contains("Rematore") && text.contains("prossima serie"))
            }
            RestTimer.finish(c, end)
            assertEquals(delivered.size, c.getSystemService(NotificationManager::class.java).activeNotifications.count { it.id == 7301 })
        } finally {
            WorkoutSurface.clear(c)
            if (oldCode == null) shared.edit().remove("code").commit() else shared.edit().putString("code", oldCode).commit()
        }
        }
    }
    @Test fun systemDeliversOneBackgroundAlarm() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val c = instrumentation.targetContext
        val alarms = c.getSystemService(android.app.AlarmManager::class.java)
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms())
        val shared = c.getSharedPreferences("shared", Context.MODE_PRIVATE)
        val previous = shared.getString("code", null)
        try {
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                shared.edit().putString("code", "LOCAL_BACKGROUND_QA").commit()
                RestTimer.start(c, 1)
            }
            val limit = android.os.SystemClock.elapsedRealtime() + 5000
            while (RestTimer.deadline(c) != 0L && android.os.SystemClock.elapsedRealtime() < limit) Thread.sleep(50)
            assertEquals("The OS receiver must expire the timer without a visible timer screen", 0L, RestTimer.deadline(c))
            val active = awaitRestNotifications(c)
            assertEquals(if (RestTimer.allowed(c)) 1 else 0, active.size)
            if (active.isNotEmpty()) assertEquals("Recupero terminato", active.single().notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE))
        } finally {
            instrumentation.runOnMainSync {
                WorkoutSurface.clear(c)
                if (previous == null) shared.edit().remove("code").commit() else shared.edit().putString("code", previous).commit()
            }
        }
    }

    @Test fun widgetRendersBothSizesAndClearsPreviousDay() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val c = instrumentation.targetContext
        instrumentation.runOnMainSync {
            try {
                val day = com.matthew.sportiliapp.model.Giorno("Giorno A", mapOf("local" to
                    com.matthew.sportiliapp.model.GruppoMuscolare("Spinta", mapOf("one" to
                        com.matthew.sportiliapp.model.Esercizio("Panca piana", "3 x 10")))))
                WorkoutSurface.save(c, "local-day", day)
                var lightTitleColor = 0
                var darkTitleColor = 0
                for (dark in listOf(false, true)) {
                    val config = android.content.res.Configuration(c.resources.configuration).apply {
                        uiMode = (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or
                            (if (dark) android.content.res.Configuration.UI_MODE_NIGHT_YES else android.content.res.Configuration.UI_MODE_NIGHT_NO)
                    }
                    val themed = c.createConfigurationContext(config)
                    for (compact in listOf(false, true)) {
                        val view = WorkoutWidget().createViews(themed, 9001, compact).apply(themed, null)
                        val title = view.findViewById<android.widget.TextView>(com.matthew.sportiliapp.R.id.widget_title)
                        assertEquals("Giorno A", title.text.toString())
                        assertEquals("1 esercizio", view.findViewById<android.widget.TextView>(com.matthew.sportiliapp.R.id.widget_detail).text.toString())
                        if (dark) darkTitleColor = title.currentTextColor else lightTitleColor = title.currentTextColor
                    }
                }
                assertNotEquals(lightTitleColor, darkTitleColor)
                WorkoutSurface.clear(c)
                val empty = WorkoutWidget().createViews(c, 9001, false).apply(c, null)
                assertEquals("La tua scheda", empty.findViewById<android.widget.TextView>(com.matthew.sportiliapp.R.id.widget_title).text.toString())
                assertFalse(empty.contentDescription.toString().contains("Giorno A"))
            } finally { WorkoutSurface.clear(c) }
        }
    }

}
