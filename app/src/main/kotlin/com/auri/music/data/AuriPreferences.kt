package com.auri.music.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.auri.music.model.EqPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.auriDataStore: DataStore<Preferences> by preferencesDataStore(name = "auri_settings")

enum class LibrarySortMode { TITLE, ARTIST, RECENT }
enum class LibraryViewMode { LIST, GRID }

data class AuriSettingsSnapshot(
    val eqPreset: EqPreset = EqPreset.FLAT,
    val customEqGains: List<Float> = List(10) { 0f },
    val preampDb: Float = 0f,
    val autoHeadroomEnabled: Boolean = true,
    val normalizerEnabled: Boolean = true,
    val crossfadeEnabled: Boolean = false,
    val crossfadeDurationMs: Int = 4000,
    val preferredAudioLanguage: String = "",
    val maxSongCacheSizeMb: Int = 1024,
    val smartDownloadsEnabled: Boolean = false,
    val smartDownloadsLimit: Int = 25,
    val sponsorBlockEnabled: Boolean = false,
    val pauseListenHistory: Boolean = false,
    val globalLyricsOffsetMs: Long = 0L,
    val librarySortMode: LibrarySortMode = LibrarySortMode.RECENT,
    val libraryViewMode: LibraryViewMode = LibraryViewMode.LIST
)

class AuriPreferences(context: Context) {
    private val dataStore = context.applicationContext.auriDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    val settings: StateFlow<AuriSettingsSnapshot> = dataStore.data
        .map { prefs ->
            val presetName = prefs[KEY_EQ_PRESET] ?: EqPreset.FLAT.name
            val preset = runCatching { EqPreset.valueOf(presetName) }.getOrDefault(EqPreset.FLAT)
            val customGains = prefs[KEY_CUSTOM_EQ_GAINS]?.let { raw ->
                runCatching { json.decodeFromString<List<Float>>(raw) }.getOrNull()
            }?.takeIf { it.size == 10 } ?: List(10) { 0f }

            val sortMode = runCatching {
                LibrarySortMode.valueOf(prefs[KEY_LIBRARY_SORT] ?: LibrarySortMode.RECENT.name)
            }.getOrDefault(LibrarySortMode.RECENT)
            val viewMode = runCatching {
                LibraryViewMode.valueOf(prefs[KEY_LIBRARY_VIEW] ?: LibraryViewMode.LIST.name)
            }.getOrDefault(LibraryViewMode.LIST)

            AuriSettingsSnapshot(
                eqPreset = preset,
                customEqGains = customGains,
                preampDb = prefs[KEY_PREAMP_DB] ?: 0f,
                autoHeadroomEnabled = prefs[KEY_AUTO_HEADROOM] ?: true,
                normalizerEnabled = prefs[KEY_NORMALIZER] ?: true,
                crossfadeEnabled = prefs[KEY_CROSSFADE] ?: false,
                crossfadeDurationMs = prefs[KEY_CROSSFADE_MS] ?: 4000,
                preferredAudioLanguage = prefs[KEY_PREFERRED_LANG] ?: "",
                maxSongCacheSizeMb = prefs[KEY_MAX_CACHE_MB] ?: 1024,
                smartDownloadsEnabled = prefs[KEY_SMART_DOWNLOADS] ?: false,
                smartDownloadsLimit = prefs[KEY_SMART_DOWNLOADS_LIMIT] ?: 25,
                sponsorBlockEnabled = prefs[KEY_SPONSORBLOCK] ?: false,
                pauseListenHistory = prefs[KEY_PAUSE_HISTORY] ?: false,
                globalLyricsOffsetMs = prefs[KEY_LYRICS_OFFSET_MS] ?: 0L,
                librarySortMode = sortMode,
                libraryViewMode = viewMode
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, AuriSettingsSnapshot())

    suspend fun setEqPreset(preset: EqPreset) {
        dataStore.edit { it[KEY_EQ_PRESET] = preset.name }
    }

    suspend fun setCustomEqGains(gains: List<Float>) {
        dataStore.edit { it[KEY_CUSTOM_EQ_GAINS] = json.encodeToString(gains) }
    }

    suspend fun setNormalizerEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NORMALIZER] = enabled }
    }

    suspend fun setAutoHeadroomEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_AUTO_HEADROOM] = enabled }
    }

    suspend fun setCrossfade(enabled: Boolean, durationMs: Int = 4000) {
        dataStore.edit {
            it[KEY_CROSSFADE] = enabled
            it[KEY_CROSSFADE_MS] = durationMs.coerceIn(1000, 12000)
        }
    }

    suspend fun setSponsorBlockEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_SPONSORBLOCK] = enabled }
    }

    suspend fun setSmartDownloads(enabled: Boolean, limit: Int = 25) {
        dataStore.edit {
            it[KEY_SMART_DOWNLOADS] = enabled
            it[KEY_SMART_DOWNLOADS_LIMIT] = limit
        }
    }

    suspend fun setGlobalLyricsOffsetMs(offsetMs: Long) {
        dataStore.edit { it[KEY_LYRICS_OFFSET_MS] = offsetMs.coerceIn(-10000L, 10000L) }
    }

    suspend fun setLibrarySortMode(mode: LibrarySortMode) {
        dataStore.edit { it[KEY_LIBRARY_SORT] = mode.name }
    }

    suspend fun setLibraryViewMode(mode: LibraryViewMode) {
        dataStore.edit { it[KEY_LIBRARY_VIEW] = mode.name }
    }

    companion object {
        private val KEY_EQ_PRESET = stringPreferencesKey("eq_preset")
        private val KEY_CUSTOM_EQ_GAINS = stringPreferencesKey("custom_eq_gains")
        private val KEY_PREAMP_DB = floatPreferencesKey("preamp_db")
        private val KEY_AUTO_HEADROOM = booleanPreferencesKey("auto_headroom")
        private val KEY_NORMALIZER = booleanPreferencesKey("normalizer_enabled")
        private val KEY_CROSSFADE = booleanPreferencesKey("crossfade_enabled")
        private val KEY_CROSSFADE_MS = intPreferencesKey("crossfade_ms")
        private val KEY_PREFERRED_LANG = stringPreferencesKey("preferred_audio_lang")
        private val KEY_MAX_CACHE_MB = intPreferencesKey("max_cache_mb")
        private val KEY_SMART_DOWNLOADS = booleanPreferencesKey("smart_downloads")
        private val KEY_SMART_DOWNLOADS_LIMIT = intPreferencesKey("smart_downloads_limit")
        private val KEY_SPONSORBLOCK = booleanPreferencesKey("sponsorblock_enabled")
        private val KEY_PAUSE_HISTORY = booleanPreferencesKey("pause_listen_history")
        private val KEY_LYRICS_OFFSET_MS = longPreferencesKey("global_lyrics_offset_ms")
        private val KEY_LIBRARY_SORT = stringPreferencesKey("library_sort_mode")
        private val KEY_LIBRARY_VIEW = stringPreferencesKey("library_view_mode")
    }
}
