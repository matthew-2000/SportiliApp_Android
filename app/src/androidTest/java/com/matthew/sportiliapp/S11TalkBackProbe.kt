package com.matthew.sportiliapp

import android.app.UiAutomation
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/** Observer for an agent-guided TalkBack tour. Keeps the actual accessibility service enabled.
 * Launch separately, perform real gestures/activation, then set debug.sportili.s11_done=true.
 * Does not infer speech quality from the tree. Captures focus events and native node snapshots.
 */
class S11TalkBackProbe {
    @Test fun recordGuidedTourWithRealService() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val ui = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "S11/talkback").apply { mkdirs() }
        val log = File(directory, "events.jsonl")
        log.writeText("")
        val focused = AtomicInteger()
        ui.setOnAccessibilityEventListener { event ->
            if (event.packageName?.toString() == "com.matthew.sportiliapp") {
                if (event.eventType == AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED) focused.incrementAndGet()
                val source = event.source
                val row = JSONObject().put("type", AccessibilityEvent.eventTypeToString(event.eventType))
                    .put("text", event.text.joinToString(" · ")).put("description", event.contentDescription ?: "")
                    .put("sourceText", source?.text ?: "").put("sourceDescription", source?.contentDescription ?: "")
                    .put("selected", source?.isSelected ?: false).put("enabled", source?.isEnabled ?: false)
                synchronized(log) { log.appendText(row.toString() + "\n") }
            }
        }
        fun shell(command: String): String = ui.executeShellCommand(command).let {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().use { reader -> reader.readText() }
        }
        assertTrue("TalkBack must actually be enabled", shell("settings get secure enabled_accessibility_services").contains("TalkBackService"))
        shell("setprop debug.sportili.s11_done false")
        val started = SystemClock.elapsedRealtime()
        while (SystemClock.elapsedRealtime() - started < 300_000 && !shell("getprop debug.sportili.s11_done").trim().equals("true")) {
            val nodes = JSONArray()
            fun walk(node: AccessibilityNodeInfo) {
                val bounds = Rect(); node.getBoundsInScreen(bounds)
                nodes.put(JSONObject().put("text", node.text ?: "").put("description", node.contentDescription ?: "")
                    .put("clickable", node.isClickable).put("scrollable", node.isScrollable).put("focused", node.isAccessibilityFocused)
                    .put("selected", node.isSelected).put("enabled", node.isEnabled).put("visible", node.isVisibleToUser)
                    .put("bounds", JSONArray(listOf(bounds.left, bounds.top, bounds.right, bounds.bottom))))
                for (index in 0 until node.childCount) node.getChild(index)?.let { walk(it) }
            }
            ui.rootInActiveWindow?.let { walk(it) }
            File(directory, "current-tree.json").writeText(nodes.toString(2))
            SystemClock.sleep(400)
        }
        ui.setOnAccessibilityEventListener(null)
        assertTrue("Tour must traverse real accessibility focus, not only screenshots", focused.get() >= 12)
        File(directory, "result.txt").writeText("TalkBack focus events: ${focused.get()}\nService preserved with FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES\nSpeech/audio quality is not asserted.\n")
    }
}
