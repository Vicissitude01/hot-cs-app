package com.hotcs.app.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ParseTest {

    @Test
    fun parsesItemFields() {
        val json = """
            {"id":"hn:1","title":"T","url":"http://x","source":"hackernews",
             "score":42.0,"published_at":"2026-08-17T00:00:00Z",
             "summary":"s","key_points":["a","b"]}
        """.trimIndent()
        val item = HotItem.fromJson(JSONObject(json))
        assertEquals("hn:1", item.id)
        assertEquals(42.0, item.score, 0.001)
        assertEquals("s", item.summary)
        assertEquals(listOf("a", "b"), item.keyPoints)
    }

    @Test
    fun defaultsMissingFields() {
        val json = """{"id":"hn:2","title":"T","url":"http://x","source":"hackernews","score":1}"""
        val item = HotItem.fromJson(JSONObject(json))
        assertEquals("", item.summary)
        assertEquals(emptyList<String>(), item.keyPoints)
        assertEquals(1.0, item.score, 0.001)
    }
}
