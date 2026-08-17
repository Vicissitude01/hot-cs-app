package com.hotcs.app.data

import android.content.Context

object Settings {
    private const val PREF = "settings"
    private const val KEY_URL = "backend_url"
    private const val KEY_INTERVAL = "interval_min"
    private const val KEY_NOTIFY = "notify"

    private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun url(c: Context): String = prefs(c).getString(KEY_URL, "") ?: ""
    fun setUrl(c: Context, v: String) = prefs(c).edit().putString(KEY_URL, v).apply()

    fun intervalMin(c: Context): Long = prefs(c).getLong(KEY_INTERVAL, 20L)
    fun setIntervalMin(c: Context, v: Long) = prefs(c).edit().putLong(KEY_INTERVAL, v).apply()

    fun notifyEnabled(c: Context): Boolean = prefs(c).getBoolean(KEY_NOTIFY, true)
    fun setNotifyEnabled(c: Context, v: Boolean) = prefs(c).edit().putBoolean(KEY_NOTIFY, v).apply()
}
