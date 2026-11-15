package com.matthew.sportiliapp.nativefeatures

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.matthew.sportiliapp.MainActivity

/** One persisted deadline. No service or background loop is required. */
object RestTimer {
    private const val ID = 7301
    private fun prefs(c: Context) = c.getSharedPreferences("native_surfaces", Context.MODE_PRIVATE)
    private fun owner(c: Context) = c.getSharedPreferences("shared", Context.MODE_PRIVATE).getString("code", "").orEmpty()
    fun deadline(c: Context): Long {
        val end = prefs(c).getLong("deadline", 0)
        if (end == 0L) return 0
        if (prefs(c).getString("owner", "") != owner(c) || owner(c).isEmpty()) { clear(c); return 0 }
        return end
    }
    fun remaining(c: Context): Int = ((deadline(c) - System.currentTimeMillis() + 999) / 1000).coerceAtLeast(0).toInt()
    private fun action(c: Context, name: String, generation: Long = prefs(c).getLong("deadline", 0)): PendingIntent =
        PendingIntent.getBroadcast(c, name.hashCode(), Intent(c, RestReceiver::class.java).setAction(name).putExtra("deadline", generation), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun total(c: Context): Int = prefs(c).getInt("total", 0)
    fun start(c: Context, seconds: Int, total: Int = seconds) {
        clear(c)
        if (owner(c).isEmpty() || seconds <= 0) return
        val end = System.currentTimeMillis() + seconds.toLong() * 1000
        prefs(c).edit().putString("owner", owner(c)).putLong("deadline", end).putInt("total", total).commit()
        schedule(c, end)
        show(c, end, false)
    }
    private fun schedule(c: Context, end: Long) {
        val alarms = c.getSystemService(AlarmManager::class.java)
        val pending = action(c, "finish", end)
        try {
            if (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pending)
            else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pending)
        } catch (_: SecurityException) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, pending) }
    }
    fun restore(c: Context) {
        val end = deadline(c)
        if (end > System.currentTimeMillis()) { schedule(c, end); show(c, end, false) }
        else if (end > 0) finish(c, end)
    }

    fun clear(c: Context) {
        c.getSystemService(AlarmManager::class.java).cancel(action(c, "finish"))
        prefs(c).edit().remove("deadline").remove("owner").remove("total").commit()
        c.getSystemService(NotificationManager::class.java).cancel(ID)
    }
    fun finish(c: Context, expected: Long) {
        if (deadline(c) != expected || expected == 0L || remaining(c) > 0) return
        clear(c)
        show(c, 0, true)
    }
    fun refresh(c: Context) {
        val end = deadline(c)
        if (end > 0 && remaining(c) > 0) show(c, end, false)
        else if (end > 0) finish(c, end)
    }
    fun allowed(c: Context) = NotificationManagerCompat.from(c).areNotificationsEnabled() &&
        c.getSystemService(NotificationManager::class.java).getNotificationChannel("rest_finished")?.importance != NotificationManager.IMPORTANCE_NONE
    private fun show(c: Context, end: Long, finished: Boolean) {
        val manager = c.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("rest_running", "Recupero in corso", NotificationManager.IMPORTANCE_LOW))
        manager.createNotificationChannel(NotificationChannel("rest_finished", "Fine recupero", NotificationManager.IMPORTANCE_HIGH).apply { enableVibration(true) })
        val open = PendingIntent.getActivity(c, ID, Intent(c, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val b = NotificationCompat.Builder(c, if (finished) "rest_finished" else "rest_running")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(if (finished) "Recupero terminato" else "Recupero")
            .setContentIntent(open).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setAutoCancel(finished)
        if (!finished) b.setTimeoutAfter((end - System.currentTimeMillis()).coerceAtLeast(1)).setWhen(end).setUsesChronometer(true).setChronometerCountDown(true).setOngoing(true)
            .addAction(0, "Interrompi", action(c, "stop")).addAction(0, "+30 sec", action(c, "extend"))
        try { manager.notify(ID, b.build()) } catch (_: SecurityException) { /* UI explains denied notifications. */ }
    }
}
class RestReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            RestTimer.restore(context)
            return
        }
        val expected = intent.getLongExtra("deadline", 0)
        if (RestTimer.deadline(context) != expected || expected == 0L) return
        when (intent.action) {
            "stop" -> RestTimer.clear(context)
            "extend" -> if (RestTimer.remaining(context) > 0) RestTimer.start(context, RestTimer.remaining(context) + 30, RestTimer.total(context) + 30)
            "finish" -> RestTimer.finish(context, expected)
        }
    }
}
