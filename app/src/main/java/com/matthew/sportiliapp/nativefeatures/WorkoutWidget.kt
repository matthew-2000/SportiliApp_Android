package com.matthew.sportiliapp.nativefeatures

import android.app.PendingIntent
import android.appwidget.*
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.SizeF
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
        ids.forEach { widget ->
            val views = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                RemoteViews(mapOf(
                    SizeF(140f, 60f) to createViews(c, widget, compact = true),
                    SizeF(180f, 140f) to createViews(c, widget, compact = false)
                ))
            } else {
                val options = manager.getAppWidgetOptions(widget)
                createViews(c, widget, compact = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 140) < 140 ||
                    options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180) < 180)
            }
            manager.updateAppWidget(widget, views)
        }
    }

    override fun onAppWidgetOptionsChanged(c: Context, manager: AppWidgetManager, id: Int, newOptions: Bundle) {
        onUpdate(c, manager, intArrayOf(id))
    }

    /** The same local-only RemoteViews used by launchers and the debug preview. */
    internal fun createViews(c: Context, widget: Int, compact: Boolean): RemoteViews {
        val p = c.getSharedPreferences("workout_surface", Context.MODE_PRIVATE)
        val id = p.getString("id", null)
        val title = p.getString("name", null)?.takeIf { it.isNotBlank() && id != null }
            ?: c.getString(R.string.widget_empty_title)
        val count = p.getInt("count", 0)
        val detail = if (id == null) c.getString(R.string.widget_empty_detail)
            else c.resources.getQuantityString(R.plurals.widget_exercise_count, count, count)
        val view = RemoteViews(c.packageName, if (compact) R.layout.workout_widget_compact else R.layout.workout_widget)
        view.setTextViewText(R.id.widget_title, title)
        view.setTextViewText(R.id.widget_detail, detail)
        view.setTextViewText(R.id.widget_action, c.getString(if (id == null) R.string.widget_open_app else R.string.widget_open))
        view.setContentDescription(R.id.widget_root, "$title. $detail. ${c.getString(if (id == null) R.string.widget_open_app else R.string.widget_open)}")
        val open = Intent(c, MainActivity::class.java).putExtra("workout_day", id)
            .putExtra("workout_widget", true).putExtra("workout_session", WorkoutSurface.session(c))
        view.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(c, widget, open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        return view
    }
}
