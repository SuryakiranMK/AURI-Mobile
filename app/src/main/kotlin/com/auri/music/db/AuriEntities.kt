package com.auri.music.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.auri.music.model.Song

@Entity(tableName = "song")
data class SongEntity(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val duration: String = "3:30",
    val durationMs: Long = 210000L,
    val thumbnailUrl: String = "",
    val isOpus: Boolean = true,
    val audioQuality: String = "OPUS 160 kbps • 48 kHz",
    val loudnessDb: Double? = null,
    val liked: Boolean = false,
    val likedAt: Long? = null,
    val inLibrary: Long? = null,
    val totalPlayTimeMs: Long = 0L
) {
    fun toModel(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        durationMs = durationMs,
        thumbnailUrl = thumbnailUrl,
        streamUrl = "",
        isOpus = isOpus,
        audioQuality = audioQuality,
        loudnessDb = loudnessDb,
        isLiked = liked
    )

    companion object {
        fun fromModel(song: Song, likedAt: Long? = null): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            duration = song.duration,
            durationMs = song.durationMs,
            thumbnailUrl = song.thumbnailUrl,
            isOpus = song.isOpus,
            audioQuality = song.audioQuality,
            loudnessDb = song.loudnessDb,
            liked = song.isLiked,
            likedAt = if (song.isLiked) (likedAt ?: System.currentTimeMillis()) else null
        )
    }
}

@Entity(
    tableName = "event",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("songId"), Index("timestampMs")]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val timestampMs: Long,
    val playTimeMs: Long
)

@Entity(
    tableName = "related_song_map",
    indices = [Index("songId"), Index("relatedSongId")]
)
data class RelatedSongMap(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,
    val relatedSongId: String
)

@Entity(tableName = "lyrics")
data class LyricsEntity(
    @PrimaryKey val id: String,
    val lyricsJson: String,
    val source: String,
    val syncType: String,
    val offsetMs: Long = 0L,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "format")
data class FormatEntity(
    @PrimaryKey val id: String,
    val itag: Int,
    val mimeType: String,
    val codecs: String,
    val bitrate: Long,
    val sampleRate: Int,
    val contentLength: Long,
    val loudnessDb: Double?,
    val streamUrl: String,
    val expiresAtMs: Long
)

@Entity(tableName = "cached_audio")
data class CachedAudioEntity(
    @PrimaryKey val id: String,
    val formatItag: Int,
    val contentLength: Long,
    val isFullyCached: Boolean = false,
    val isDownloaded: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis()
)
