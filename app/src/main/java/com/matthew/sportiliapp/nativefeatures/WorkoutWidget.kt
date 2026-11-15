package com.matthew.sportiliapp.nativefeatures

import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.widget.RemoteViews
import com.matthew.sportiliapp.MainActivity
import com.matthew.sportiliapp.R
import com.matthew.sportiliapp.model.Giorno

object WorkoutSurface {
    private fun prefs(c: Context) = c.getSharedPreferences("workout_surface", Context.MODE_PRIVATE)
    fun save(c: Context, id: String, day: Giorno) {
        prefs(c).edit().putString("id", id).putString("name", day.name).putInt("count", day.gruppiMuscolari.values.sumOf { it.esercizi.size }).commit()
        refresh(c)
    }
    fun clear(c: Context) { prefs(c).edit().clear().putString("session", java.util.UUID.randomUUID().toString()).commit(); RestTimer.clear(c); refresh(c) }
    fun session(c: Context): String? = prefs(c).getString("session", null)
    fun update(c: Context, days: Map<String, Giorno>?) {
        val id = prefs(c).getString("id", null) ?: return
        val day = days?.get(id)
        if (day == null) { prefs(c).edit().remove("id").remove("name").remove("count").commit(); refresh(c) }
        else save(c, id, day)
    }
    fun refresh(c: Context) { val m = AppWidgetManager.getInstance(c); WorkoutWidget().onUpdate(c, m, m.getAppWidgetIds(ComponentName(c, WorkoutWidget::class.java))) }
}
class WorkoutWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, manager: AppWidgetManager, ids: IntArray) {
        val p = c.getSharedPreferences("workout_surface", Context.MODE_PRIVATE)
        val id = p.getString("id", null)
        ids.forEach { widget ->
            val view = RemoteViews(c.packageName, R.layout.workout_widget)
            view.setTextViewText(R.id.widget_title, p.getString("name", "Apri la scheda"))
            view.setTextViewText(R.id.widget_detail, if (id == null) "Scegli un giorno nell’app" else "${p.getInt("count", 0)} esercizi · Giorno scelto")
            val open = Intent(c, MainActivity::class.java).putExtra("workout_day", id).putExtra("workout_widget", true).putExtra("workout_session", WorkoutSurface.session(c))
            view.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(c, widget, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            manager.updateAppWidget(widget, view)
        }
    }
}
