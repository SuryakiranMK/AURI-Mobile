package com.auri.music.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        SongEntity::class,
        EventEntity::class,
        RelatedSongMap::class,
        LyricsEntity::class,
        FormatEntity::class,
        CachedAudioEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AuriDatabase : RoomDatabase() {
    abstract fun auriDao(): AuriDao

    companion object {
        @Volatile
        private var INSTANCE: AuriDatabase? = null

        fun getInstance(context: Context): AuriDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AuriDatabase::class.java,
                    "auri_music.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
