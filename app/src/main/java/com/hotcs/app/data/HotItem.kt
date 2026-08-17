package com.hotcs.app.data

import org.json.JSONArray
import org.json.JSONObject

data class HotItem(
    val id: String,
    val title: String,
    val url: String,
    val source: String,
    val score: Double,
    val publishedAt: String,
    val summary: String = "",
    val keyPoints: List<String> = emptyList()
) {
    companion object {
        fun fromJson(o: JSONObject): HotItem {
            val kp = o.optJSONArray("key_points") ?: JSONArray()
            return HotItem(
                id = o.optString("id"),
                title = o.optString("title"),
                url = o.optString("url"),
                source = o.optString("source"),
                score = o.optDouble("score", 0.0),
                publishedAt = o.optString("published_at"),
                summary = o.optString("summary"),
                keyPoints = (0 until kp.length()).map { kp.optString(it) }
            )
        }
    }
}
