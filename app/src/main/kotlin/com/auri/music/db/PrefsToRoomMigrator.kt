package com.auri.music.db

import android.content.Context
import org.json.JSONArray

object PrefsToRoomMigrator {
    private const val PREFS_NAME = "auri_library_prefs"
    private const val KEY_LIKED_IDS = "liked_song_ids"
    private const val KEY_LIKED_METADATA = "liked_songs_metadata"
    private const val KEY_MIGRATED = "room_v1_migrated"

    suspend fun migrateIfNeeded(context: Context, dao: AuriDao) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED, false)) return

        val likedIds = prefs.getStringSet(KEY_LIKED_IDS, emptySet()) ?: emptySet()
        val rawMeta = prefs.getString(KEY_LIKED_METADATA, null)
        val now = System.currentTimeMillis()

        if (!rawMeta.isNullOrBlank()) {
            try {
                val arr = JSONArray(rawMeta)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val id = obj.optString("id", "")
                    if (id.isEmpty()) continue
                    val entity = SongEntity(
                        id = id,
                        title = obj.optString("title", "Unknown Track"),
                        artist = obj.optString("artist", "Unknown Artist"),
                        album = obj.optString("album", ""),
                        duration = obj.optString("duration", "3:30"),
                        durationMs = obj.optLong("durationMs", 210000L),
                        thumbnailUrl = obj.optString("thumbnailUrl", ""),
                        isOpus = obj.optBoolean("isOpus", true),
                        audioQuality = obj.optString("audioQuality", "OPUS 160 kbps • 48 kHz"),
                        loudnessDb = if (obj.has("loudnessDb") && !obj.isNull("loudnessDb")) obj.getDouble("loudnessDb") else null,
                        liked = likedIds.contains(id),
                        likedAt = if (likedIds.contains(id)) now else null
                    )
                    dao.insertSongIfAbsent(entity)
                    if (likedIds.contains(id)) {
                        dao.setLiked(id, true, now)
                    }
                }
            } catch (_: Throwable) {}
        }

        prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
    }
}
