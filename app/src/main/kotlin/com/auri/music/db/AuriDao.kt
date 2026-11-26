package com.auri.music.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AuriDao {
    @Query("SELECT * FROM song WHERE id = :songId LIMIT 1")
    suspend fun getSong(songId: String): SongEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongIfAbsent(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSong(song: SongEntity)

    @Query("UPDATE song SET liked = :liked, likedAt = :likedAt WHERE id = :songId")
    suspend fun setLiked(songId: String, liked: Boolean, likedAt: Long?)

    @Query("UPDATE song SET totalPlayTimeMs = totalPlayTimeMs + :deltaMs WHERE id = :songId")
    suspend fun addPlayTime(songId: String, deltaMs: Long)

    @Query("SELECT * FROM song WHERE liked = 1 ORDER BY likedAt DESC")
    fun getLikedSongs(): Flow<List<SongEntity>>

    @Query(
        """
        SELECT s.* FROM song s
        INNER JOIN cached_audio c ON s.id = c.id
        WHERE c.isFullyCached = 1
        ORDER BY c.updatedAtMs DESC
        """
    )
    fun getCachedSongs(): Flow<List<SongEntity>>

    @Query(
        """
        SELECT s.* FROM song s
        INNER JOIN cached_audio c ON s.id = c.id
        WHERE c.isDownloaded = 1
        ORDER BY c.updatedAtMs DESC
        """
    )
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM song ORDER BY totalPlayTimeMs DESC, likedAt DESC")
    fun getAllKnownSongs(): Flow<List<SongEntity>>

    @Insert
    suspend fun insertEvent(event: EventEntity)

    @Query("SELECT COUNT(*) FROM event")
    suspend fun getEventCount(): Int

    @Query(
        """
        SELECT s.* FROM song s
        INNER JOIN event e ON s.id = e.songId
        GROUP BY s.id
        ORDER BY SUM(e.playTimeMs) DESC, MAX(e.timestampMs) DESC
        LIMIT :limit
        """
    )
    suspend fun getKeepListening(limit: Int = 10): List<SongEntity>

    @Query(
        """
        SELECT s.* FROM song s
        INNER JOIN event e ON s.id = e.songId
        WHERE e.timestampMs < :beforeMs
        GROUP BY s.id
        ORDER BY COUNT(e.id) DESC
        LIMIT :limit
        """
    )
    suspend fun getForgottenFavorites(beforeMs: Long, limit: Int = 10): List<SongEntity>

    @Query(
        """
        SELECT s.* FROM song s
        LEFT JOIN event e ON s.id = e.songId
        WHERE s.liked = 1 OR e.id IS NOT NULL
        GROUP BY s.id
        ORDER BY COALESCE(SUM(e.playTimeMs), 0) DESC, s.likedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getQuickPicks(limit: Int = 10): List<SongEntity>

    @Query(
        """
        SELECT * FROM event
        WHERE timestampMs >= :sinceMs
        ORDER BY timestampMs DESC
        """
    )
    suspend fun getEventsSince(sinceMs: Long): List<EventEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRelated(mapping: RelatedSongMap)

    @Query(
        """
        SELECT s.* FROM song s
        INNER JOIN related_song_map r ON s.id = r.relatedSongId
        WHERE r.songId = :songId
        LIMIT :limit
        """
    )
    suspend fun getRelatedSongs(songId: String, limit: Int = 10): List<SongEntity>

    @Query("SELECT * FROM lyrics WHERE id = :songId LIMIT 1")
    suspend fun getLyrics(songId: String): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLyrics(lyrics: LyricsEntity)

    @Query("UPDATE lyrics SET offsetMs = :offsetMs WHERE id = :songId")
    suspend fun updateLyricsOffset(songId: String, offsetMs: Long)

    @Query("SELECT * FROM format WHERE id = :songId LIMIT 1")
    suspend fun getFormat(songId: String): FormatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFormat(format: FormatEntity)

    @Query("SELECT * FROM cached_audio WHERE id = :songId LIMIT 1")
    suspend fun getCachedAudio(songId: String): CachedAudioEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCachedAudio(cachedAudio: CachedAudioEntity)
}
