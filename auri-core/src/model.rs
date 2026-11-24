use serde::{Deserialize, Serialize};

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct SongItem {
    pub id: String,
    pub title: String,
    pub artist: String,
    pub album: String,
    pub duration: String,
    pub duration_seconds: u32,
    pub thumbnail_url: String,
    pub stream_url: Option<String>,
    pub is_opus: bool,
    pub bitrate_kbps: u32,
    pub sample_rate: u32,
    pub loudness_db: Option<f64>,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct SearchResults {
    pub query: String,
    pub songs: Vec<SongItem>,
    pub artists: Vec<ArtistItem>,
    pub albums: Vec<AlbumItem>,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct ArtistItem {
    pub id: String,
    pub name: String,
    pub thumbnail_url: String,
    pub subscriber_count: String,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct AlbumItem {
    pub id: String,
    pub title: String,
    pub artist: String,
    pub year: String,
    pub thumbnail_url: String,
    pub track_count: u32,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct LyricsLine {
    pub timestamp_ms: u64,
    pub text: String,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct LyricsData {
    pub synced: Vec<LyricsLine>,
    pub plain: String,
    pub source: String,
}

#[derive(Clone, Debug, Default, PartialEq, Serialize, Deserialize)]
pub struct StreamResolution {
    pub video_id: String,
    pub url: String,
    pub content_length: u64,
    pub duration_ms: u64,
    pub is_opus: bool,
    pub bitrate: u64,
    pub sample_rate: u32,
    pub audio_quality: String,
}
