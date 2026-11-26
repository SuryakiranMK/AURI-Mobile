package com.auri.music.data.lyrics

import com.auri.music.data.AuriNativeEngine
import com.auri.music.db.AuriDao
import com.auri.music.db.LyricsEntity
import com.auri.music.model.LyricSyncType
import com.auri.music.model.LyricWord
import com.auri.music.model.Lyrics
import com.auri.music.model.LyricsLine
import com.auri.music.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Multi-provider lyrics waterfall:
 * Room DB cache -> BetterLyrics (TTML word-sync) -> SimpMusic Lyrics -> LRCLIB -> KuGou -> Rust Innertube.
 */
class LyricsWaterfallRepository(
    private val dao: AuriDao?
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun getLyrics(song: Song): Lyrics = withContext(Dispatchers.IO) {
        dao?.getLyrics(song.id)?.let { entity ->
            runCatching { json.decodeFromString<Lyrics>(entity.lyricsJson) }.getOrNull()?.let { cached ->
                if (cached.syncedLines.isNotEmpty() || cached.plainText.isNotEmpty()) {
                    return@withContext cached
                }
            }
        }

        val fetched = fetchFromBetterLyrics(song)
            ?: fetchFromSimpMusic(song)
            ?: fetchFromLrcLib(song)
            ?: fetchFromKuGou(song)
            ?: AuriNativeEngine.getLyrics(
                videoId = song.id,
                title = song.title,
                artist = song.artist,
                durationSec = song.durationMs / 1000L
            )?.takeIf { it.syncedLines.isNotEmpty() || it.plainText.isNotEmpty() }

        if (fetched != null) {
            dao?.upsertLyrics(
                LyricsEntity(
                    id = song.id,
                    lyricsJson = json.encodeToString(fetched),
                    source = fetched.source,
                    syncType = fetched.syncType.name
                )
            )
            return@withContext fetched
        }

        Lyrics(
            syncedLines = emptyList(),
            plainText = "",
            source = "No lyrics found for \"${song.title}\"",
            syncType = LyricSyncType.UNSYNCED
        )
    }

    private fun fetchFromBetterLyrics(song: Song): Lyrics? {
        return runCatching {
            val title = URLEncoder.encode(song.title, "UTF-8")
            val artist = URLEncoder.encode(song.artist, "UTF-8")
            val dur = (song.durationMs / 1000L).coerceAtLeast(1L)
            val url = "https://lyrics-api.boidu.dev/getLyrics?s=$title&a=$artist&d=$dur"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val obj = JSONObject(body)
                val ttml = obj.optString("ttml", "")
                if (ttml.isBlank()) return@use null
                parseTtmlLyrics(ttml, "BetterLyrics (Word-Sync)")
            }
        }.getOrNull()
    }

    private fun fetchFromSimpMusic(song: Song): Lyrics? {
        return runCatching {
            val url = "https://api-lyrics.simpmusic.org/v1/${song.id}"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val obj = JSONObject(body)
                val data = obj.optJSONArray("data") ?: return@use null
                if (data.length() == 0) return@use null
                val first = data.getJSONObject(0)
                val synced = first.optString("syncedLyrics", "")
                val plain = first.optString("plainLyrics", "")
                when {
                    synced.isNotBlank() -> parseLrc(synced, "SimpMusic Lyrics")
                    plain.isNotBlank() -> Lyrics(
                        syncedLines = emptyList(),
                        plainText = plain,
                        source = "SimpMusic Lyrics",
                        syncType = LyricSyncType.UNSYNCED
                    )
                    else -> null
                }
            }
        }.getOrNull()
    }

    private fun fetchFromLrcLib(song: Song): Lyrics? {
        return runCatching {
            val title = URLEncoder.encode(song.title, "UTF-8")
            val artist = URLEncoder.encode(song.artist, "UTF-8")
            val dur = (song.durationMs / 1000L).coerceAtLeast(1L)
            val url = "https://lrclib.net/api/get?track_name=$title&artist_name=$artist&duration=$dur"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "AURI-Mobile/1.2.0")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val obj = JSONObject(resp.body?.string() ?: return@use null)
                val synced = obj.optString("syncedLyrics", "")
                val plain = obj.optString("plainLyrics", "")
                when {
                    synced.isNotBlank() -> parseLrc(synced, "LRCLIB")
                    plain.isNotBlank() -> Lyrics(
                        syncedLines = emptyList(),
                        plainText = plain,
                        source = "LRCLIB (Plain)",
                        syncType = LyricSyncType.UNSYNCED
                    )
                    else -> null
                }
            }
        }.getOrNull()
    }

    private fun fetchFromKuGou(song: Song): Lyrics? {
        return runCatching {
            val keyword = URLEncoder.encode("${song.artist} - ${song.title}", "UTF-8")
            val searchUrl = "https://lyrics.kugou.com/search?ver=1&man=yes&client=pc&keyword=$keyword&duration=${song.durationMs}"
            val req = Request.Builder().url(searchUrl).get().build()
            val (id, accesskey) = client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@runCatching null
                val obj = JSONObject(resp.body?.string() ?: return@runCatching null)
                val candidates: JSONArray = obj.optJSONArray("candidates") ?: return@runCatching null
                if (candidates.length() == 0) return@runCatching null
                val best = candidates.getJSONObject(0)
                best.optString("id") to best.optString("accesskey")
            }
            if (id.isBlank() || accesskey.isBlank()) return@runCatching null
            val dlUrl = "https://lyrics.kugou.com/download?ver=1&client=pc&id=$id&accesskey=$accesskey&fmt=lrc&charset=utf8"
            client.newCall(Request.Builder().url(dlUrl).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val obj = JSONObject(resp.body?.string() ?: return@use null)
                val b64 = obj.optString("content", "")
                if (b64.isBlank()) return@use null
                val decoded = String(android.util.Base64.decode(b64, android.util.Base64.DEFAULT), Charsets.UTF_8)
                parseLrc(decoded, "KuGou")
            }
        }.getOrNull()
    }

    internal fun parseLrc(lrc: String, source: String): Lyrics? {
        val regex = Regex("""\[(\d{1,2}):(\d{2})(?:\.(\d{2,3}))?\](.*)""")
        val lines = ArrayList<LyricsLine>()
        for (raw in lrc.lines()) {
            val m = regex.find(raw.trim()) ?: continue
            val min = m.groupValues[1].toLongOrNull() ?: continue
            val sec = m.groupValues[2].toLongOrNull() ?: continue
            val fracStr = m.groupValues[3]
            val msPart = when (fracStr.length) {
                2 -> fracStr.toLongOrNull()?.times(10) ?: 0L
                3 -> fracStr.toLongOrNull() ?: 0L
                else -> 0L
            }
            val text = m.groupValues[4].trim()
            if (text.isEmpty()) continue
            val ts = (min * 60L + sec) * 1000L + msPart
            lines.add(LyricsLine(timestampMs = ts, text = text))
        }
        if (lines.isEmpty()) return null
        val sorted = lines.sortedBy { it.timestampMs }
        return Lyrics(
            syncedLines = sorted,
            plainText = sorted.joinToString("\n") { it.text },
            source = source,
            syncType = LyricSyncType.LINE_SYNCED
        )
    }

    internal fun parseTtmlLyrics(ttml: String, source: String): Lyrics? {
        val pRegex = Regex("""<p[^>]*begin="([^"]+)"[^>]*end="([^"]+)"[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
        val spanRegex = Regex("""<span[^>]*begin="([^"]+)"[^>]*end="([^"]+)"[^>]*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
        val lines = ArrayList<LyricsLine>()
        var hasWords = false

        for (pm in pRegex.findAll(ttml)) {
            val pStart = parseTtmlTimeMs(pm.groupValues[1]) ?: continue
            val pEnd = parseTtmlTimeMs(pm.groupValues[2])
            val inner = pm.groupValues[3]

            val words = ArrayList<LyricWord>()
            for (sm in spanRegex.findAll(inner)) {
                val wStart = parseTtmlTimeMs(sm.groupValues[1]) ?: continue
                val wEnd = parseTtmlTimeMs(sm.groupValues[2]) ?: (wStart + 250L)
                val wText = sm.groupValues[3].replace(Regex("<[^>]+>"), "").trim()
                if (wText.isNotEmpty()) {
                    words.add(LyricWord(startMs = wStart, endMs = wEnd, text = wText))
                }
            }

            val lineText = if (words.isNotEmpty()) {
                hasWords = true
                words.joinToString(" ") { it.text }
            } else {
                inner.replace(Regex("<[^>]+>"), "").trim()
            }
            if (lineText.isNotEmpty()) {
                lines.add(LyricsLine(timestampMs = pStart, text = lineText, endMs = pEnd, words = words))
            }
        }
        if (lines.isEmpty()) return null
        val sorted = lines.sortedBy { it.timestampMs }
        return Lyrics(
            syncedLines = sorted,
            plainText = sorted.joinToString("\n") { it.text },
            source = source,
            syncType = if (hasWords) LyricSyncType.RICH_SYNCED else LyricSyncType.LINE_SYNCED
        )
    }

    private fun parseTtmlTimeMs(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.endsWith("s")) {
            return trimmed.dropLast(1).toDoubleOrNull()?.let { (it * 1000.0).toLong() }
        }
        val parts = trimmed.split(":")
        return when (parts.size) {
            2 -> {
                val m = parts[0].toLongOrNull() ?: return null
                val s = parts[1].toDoubleOrNull() ?: return null
                (m * 60_000L) + (s * 1000.0).toLong()
            }
            3 -> {
                val h = parts[0].toLongOrNull() ?: return null
                val m = parts[1].toLongOrNull() ?: return null
                val s = parts[2].toDoubleOrNull() ?: return null
                (h * 3_600_000L) + (m * 60_000L) + (s * 1000.0).toLong()
            }
            else -> trimmed.toDoubleOrNull()?.let { (it * 1000.0).toLong() }
        }
    }
}
