package com.toxyaudit.lite

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject
import org.json.JSONArray

class MainActivity : Activity() {
    companion object { const val PREFS = "toxy_audit_local" }

    private val bg = 0xFF0B1220.toInt()
    private val panel = 0xFF121D2E.toInt()
    private val white = 0xFFEDF4FF.toInt()
    private val muted = 0xFF9AACC3.toInt()
    private val cyan = 0xFF54D6D0.toInt()
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        refreshInventory()
        buildScreen()
    }

    override fun onResume() {
        super.onResume()
        if (::content.isInitialized) buildScreen()
    }

    private fun refreshInventory() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val known = try { JSONObject(prefs.getString("known_apps", "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (ri in resolved) {
            val pkg = ri.activityInfo?.packageName ?: continue
            if (pkg == packageName) continue
            val label = try { ri.loadLabel(pm).toString() } catch (_: Exception) { pkg }
            known.put(pkg, label)
        }
        prefs.edit().putString("known_apps", known.toString()).apply()
    }

    private fun buildScreen() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val known = try { JSONObject(prefs.getString("known_apps", "{}") ?: "{}") } catch (_: Exception) { JSONObject() }
        val history = try { JSONArray(prefs.getString("removed_history", "[]") ?: "[]") } catch (_: Exception) { JSONArray() }

        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 28, 22, 28)
            setBackgroundColor(bg)
        }
        scroll.addView(content)
        setContentView(scroll)

        text("TOXY AUDIT", 25, white, true)
        text("Installed app inventory & uninstall history", 14, muted, false)
        gap(18)
        card {
            text("REMOVED APPS", 12, muted, true)
            text(history.length().toString(), 34, cyan, true)
            text("Uninstalls recorded since Toxy Audit was installed", 12, muted, false)
        }
        gap(12)
        button("Refresh installed app list") { refreshInventory(); buildScreen() }
        gap(16)
        sectionTitle("Uninstall history")
        if (history.length() == 0) text("No app removals detected yet. If you uninstall an app after installing Toxy Audit, it should appear here when Android delivers the uninstall event.", 14, muted, false)
        for (i in history.length() - 1 downTo 0) {
            val item = history.optJSONObject(i) ?: continue
            card {
                text(item.optString("name"), 16, white, true)
                text(item.optString("package"), 12, muted, false)
                text(item.optString("time"), 12, cyan, false)
            }
            gap(8)
        }
        gap(14)
        sectionTitle("Currently known apps (${known.length()})")
        val keys = known.keys().asSequence().toList().sortedBy { known.optString(it).lowercase() }
        if (keys.isEmpty()) text("No launchable apps found.", 14, muted, false)
        keys.forEach { pkg ->
            card {
                text(known.optString(pkg, pkg), 15, white, true)
                text(pkg, 11, muted, false)
            }
            gap(6)
        }
        gap(16)
        text("Privacy: records stay on this device. This app does not read messages, photos, or usage activity. Android only provides uninstall events after this app is installed; earlier deletions cannot be reconstructed.", 12, muted, false)
    }

    private fun sectionTitle(s: String) { text(s, 18, white, true); gap(8) }
    private fun text(s: String, size: Int, color: Int, bold: Boolean) {
        val v = TextView(this).apply {
            text = s; textSize = size.toFloat(); setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 3, 0, 3)
        }
        content.addView(v)
    }
    private fun gap(h: Int) { content.addView(View(this).apply { layoutParams = LinearLayout.LayoutParams(1, h) }) }
    private fun card(block: LinearLayout.() -> Unit) {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 13, 16, 13)
            setBackgroundColor(panel)
        }
        box.block()
        content.addView(box, LinearLayout.LayoutParams(-1, -2))
    }
    private fun button(label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label; setTextColor(bg); setBackgroundColor(cyan)
            setOnClickListener { action() }
        }
        content.addView(b, LinearLayout.LayoutParams(-1, -2))
    }
}
