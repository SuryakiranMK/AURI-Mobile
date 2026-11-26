package com.auri.music.data.sponsorblock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class SponsorSegment(
    val startMs: Long,
    val endMs: Long,
    val category: String
)

class SponsorBlockRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, List<SponsorSegment>>()

    suspend fun getSkipSegments(videoId: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        if (videoId.isBlank() || videoId.startsWith("auri_")) return@withContext emptyList()
        cache[videoId]?.let { return@withContext it }

        val prefix4 = sha256Hex(videoId).take(4)
        val url = "https://sponsor.ajay.app/api/skipSegments/$prefix4?categories=[\"music_offtopic\",\"sponsor\",\"intro\",\"outro\"]"

        val segments = runCatching {
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val body = resp.body?.string() ?: return@use emptyList()
                parseSegmentsForVideo(body, videoId)
            }
        }.getOrDefault(emptyList())

        cache[videoId] = segments
        segments
    }

    private fun parseSegmentsForVideo(jsonBody: String, videoId: String): List<SponsorSegment> {
        val arr = JSONArray(jsonBody)
        val out = ArrayList<SponsorSegment>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            if (item.optString("videoID") != videoId) continue
            val segs = item.optJSONArray("segments") ?: continue
            for (j in 0 until segs.length()) {
                val s = segs.getJSONObject(j)
                val range = s.optJSONArray("segment") ?: continue
                if (range.length() < 2) continue
                val startMs = (range.getDouble(0) * 1000.0).toLong()
                val endMs = (range.getDouble(1) * 1000.0).toLong()
                if (endMs > startMs) {
                    out.add(
                        SponsorSegment(
                            startMs = startMs,
                            endMs = endMs,
                            category = s.optString("category", "music_offtopic")
                        )
                    )
                }
            }
        }
        return out.sortedBy { it.startMs }
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
