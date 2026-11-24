use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jdouble, jlong, jstring};
use serde::{Deserialize, Serialize};

use crate::dsp::calculate_loudness_gain_db;
use crate::ytm::{Client, Item};

#[derive(Serialize, Deserialize)]
pub struct NativeStreamInfo {
    pub url: String,
    pub duration_ms: u64,
    pub is_opus: bool,
    pub loudness_db: Option<f64>,
    pub audio_quality: String,
}

#[derive(Serialize, Deserialize)]
pub struct NativeSongItem {
    pub id: String,
    pub title: String,
    pub artist: String,
    pub album: String,
    pub duration: String,
    pub duration_ms: u64,
    pub thumbnail_url: String,
}

#[derive(Serialize, Deserialize)]
pub struct NativeLyricsResponse {
    pub synced: Vec<NativeLyricsLine>,
    pub plain: String,
    pub is_synced: bool,
    pub source: String,
}

#[derive(Serialize, Deserialize)]
pub struct NativeLyricsLine {
    pub timestamp_ms: u64,
    pub text: String,
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeGetVersion(
    env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = "AURI Core v2.5.2 (Rust / Innertube / -14.0 LUFS)";
    let output = env.new_string(version).unwrap_or_else(|_| env.new_string("").unwrap());
    output.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeCalculateLoudnessGain(
    _env: JNIEnv,
    _class: JClass,
    loudness_db: jdouble,
) -> jdouble {
    let input = if loudness_db.is_nan() || loudness_db < -100.0 {
        None
    } else {
        Some(loudness_db as f64)
    };
    calculate_loudness_gain_db(input).unwrap_or(-4.5) as jdouble
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeSearch(
    mut env: JNIEnv,
    _class: JClass,
    query: JString,
) -> jstring {
    let query_str: String = match env.get_string(&query) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    if query_str.trim().is_empty() {
        let empty_json = env.new_string("[]").unwrap();
        return empty_json.into_raw();
    }

    let client = Client::new(String::new());
    let songs: Vec<NativeSongItem> = match client.search(&query_str) {
        Ok(page) => page
            .songs()
            .into_iter()
            .map(|item| {
                let dur_ms = parse_duration_to_ms(&item.duration);
                let artist_name = if !item.subtitle.is_empty() {
                    item.subtitle.clone()
                } else if !item.artists.is_empty() {
                    item.artists.iter().map(|a| a.0.as_str()).collect::<Vec<_>>().join(", ")
                } else {
                    "Unknown Artist".to_string()
                };

                NativeSongItem {
                    id: item.video_id.clone(),
                    title: item.title.clone(),
                    artist: artist_name,
                    album: item.extra.clone(),
                    duration: item.duration.clone(),
                    duration_ms: dur_ms,
                    thumbnail_url: item.thumb.clone(),
                }
            })
            .collect(),
        Err(_) => Vec::new(),
    };

    let json = serde_json::to_string(&songs).unwrap_or_else(|_| "[]".into());
    let output = env.new_string(json).unwrap_or_else(|_| env.new_string("[]").unwrap());
    output.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeResolveStream(
    mut env: JNIEnv,
    _class: JClass,
    video_id: JString,
) -> jstring {
    let vid: String = match env.get_string(&video_id) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };

    if vid.is_empty() {
        let empty = env.new_string("{}").unwrap();
        return empty.into_raw();
    }

    let client = Client::new(String::new());
    let stream_res = client.stream(&vid);

    let json = match stream_res {
        Ok(stream) => {
            let quality_label = if stream.opus {
                "OPUS 160 kbps • 48 kHz".to_string()
            } else {
                "AAC 140 kbps • 44.1 kHz".to_string()
            };
            let info = NativeStreamInfo {
                url: stream.url,
                duration_ms: stream.duration_ms,
                is_opus: stream.opus,
                loudness_db: stream.loudness_db,
                audio_quality: quality_label,
            };
            serde_json::to_string(&info).unwrap_or_else(|_| "{}".into())
        }
        Err(e) => {
            format!("{{\"error\": \"{}\"}}", e.replace('"', "\\\""))
        }
    };

    let output = env.new_string(json).unwrap_or_else(|_| env.new_string("{}").unwrap());
    output.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeGetLyrics(
    mut env: JNIEnv,
    _class: JClass,
    video_id: JString,
    title: JString,
    artist: JString,
    duration_sec: jlong,
) -> jstring {
    let vid: String = env.get_string(&video_id).map(Into::into).unwrap_or_default();
    let title_str: String = env.get_string(&title).map(Into::into).unwrap_or_default();
    let artist_str: String = env.get_string(&artist).map(Into::into).unwrap_or_default();

    let client = Client::new(String::new());
    let song_item = Item {
        video_id: vid,
        title: title_str,
        subtitle: artist_str.clone(),
        artists: if !artist_str.is_empty() {
            vec![(artist_str, String::new())]
        } else {
            Vec::new()
        },
        ..Default::default()
    };

    let lyrics_res = client.lyrics(&song_item, duration_sec.max(0) as u64);

    let json = match lyrics_res {
        Ok(Some(lyrics)) => {
            let plain_text = lyrics
                .lines
                .iter()
                .map(|(_, line)| line.as_str())
                .collect::<Vec<_>>()
                .join("\n");

            let synced_lines: Vec<NativeLyricsLine> = lyrics
                .lines
                .into_iter()
                .map(|(ts, text)| NativeLyricsLine {
                    timestamp_ms: ts,
                    text,
                })
                .collect();

            let resp = NativeLyricsResponse {
                synced: synced_lines,
                plain: plain_text,
                is_synced: lyrics.synced,
                source: lyrics.source,
            };
            serde_json::to_string(&resp).unwrap_or_else(|_| "{}".into())
        }
        _ => "{}".into(),
    };

    let output = env.new_string(json).unwrap_or_else(|_| env.new_string("{}").unwrap());
    output.into_raw()
}

fn parse_duration_to_ms(duration_str: &str) -> u64 {
    let parts: Vec<&str> = duration_str.split(':').collect();
    match parts.len() {
        2 => {
            let m: u64 = parts[0].parse().unwrap_or(0);
            let s: u64 = parts[1].parse().unwrap_or(0);
            (m * 60 + s) * 1000
        }
        3 => {
            let h: u64 = parts[0].parse().unwrap_or(0);
            let m: u64 = parts[1].parse().unwrap_or(0);
            let s: u64 = parts[2].parse().unwrap_or(0);
            (h * 3600 + m * 60 + s) * 1000
        }
        _ => 0,
    }
}
