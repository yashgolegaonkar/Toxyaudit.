package com.toxyaudit.lite

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PackageRemovedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_PACKAGE_REMOVED) return
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return

        val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        val known = prefs.getString("known_apps", "{}") ?: "{}"
        val labels = try { JSONObject(known) } catch (_: Exception) { JSONObject() }
        val appName = labels.optString(packageName, packageName)

        val historyText = prefs.getString("removed_history", "[]") ?: "[]"
        val history = try { JSONArray(historyText) } catch (_: Exception) { JSONArray() }
        val entry = JSONObject().apply {
            put("name", appName)
            put("package", packageName)
            put("time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        }
        history.put(entry)

        labels.remove(packageName)
        prefs.edit()
            .putString("known_apps", labels.toString())
            .putString("removed_history", history.toString())
            .apply()
    }
}
