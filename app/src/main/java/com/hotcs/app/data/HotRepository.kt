package com.hotcs.app.data

import android.content.Context
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

fun parseItems(text: String): List<HotItem> {
    val arr = JSONArray(text)
    return (0 until arr.length()).map { HotItem.fromJson(arr.getJSONObject(it)) }
}

class HotRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("hotcs", Context.MODE_PRIVATE)

    fun fetch(baseUrl: String): List<HotItem> {
        val conn = URL(baseUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000
        try {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val items = parseItems(text)
            prefs.edit().putString("cache", text).apply()
            return items
        } finally {
            conn.disconnect()
        }
    }

    fun cached(): List<HotItem> {
        val text = prefs.getString("cache", null) ?: return emptyList()
        return runCatching { parseItems(text) }.getOrDefault(emptyList())
    }

    fun lastNotifiedIds(): Set<String> =
        prefs.getStringSet("notified", emptySet())!!.toSet()

    fun saveNotifiedIds(ids: Set<String>) =
        prefs.edit().putStringSet("notified", ids).apply()
}
