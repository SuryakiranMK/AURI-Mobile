use jni::JNIEnv;
use jni::objects::{JByteBuffer, JClass, JFloatArray, JString};
use jni::sys::{jboolean, jdouble, jfloat, jint, jlong, jstring, JNI_TRUE};
use serde::{Deserialize, Serialize};
use std::sync::OnceLock;

use crate::dsp::{calculate_loudness_gain_db, AudioPipeline};
use crate::ytm::{Client, Item};

fn shared_client() -> &'static Client {
    static CLIENT: OnceLock<Client> = OnceLock::new();
    CLIENT.get_or_init(|| Client::new(String::new()))
}

#[derive(Serialize, Deserialize)]
pub struct NativeStreamInfo {
    pub url: String,
    pub duration_ms: u64,
    pub is_opus: bool,
    pub loudness_db: Option<f64>,
    pub audio_quality: String,
    pub itag: u64,
    pub mime_type: String,
    pub codecs: String,
    pub bitrate: u64,
    pub sample_rate: u32,
    pub content_length: u64,
    pub expires_in_seconds: u64,
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

    let client = shared_client();
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
    resolve_stream_jni(&mut env, video_id, None)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeResolveStreamWithLang(
    mut env: JNIEnv,
    _class: JClass,
    video_id: JString,
    preferred_lang: JString,
) -> jstring {
    resolve_stream_jni(&mut env, video_id, Some(preferred_lang))
}

fn resolve_stream_jni(
    env: &mut JNIEnv,
    video_id: JString,
    preferred_lang: Option<JString>,
) -> jstring {
    let vid: String = match env.get_string(&video_id) {
        Ok(s) => s.into(),
        Err(_) => String::new(),
    };
    let lang: String = preferred_lang
        .and_then(|s| env.get_string(&s).ok().map(Into::into))
        .unwrap_or_default();

    if vid.is_empty() {
        let empty = env.new_string("{}").unwrap();
        return empty.into_raw();
    }

    let client = shared_client();
    let stream_res = client.stream_with_language(&vid, &lang);

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
                itag: stream.itag,
                mime_type: stream.mime_type,
                codecs: stream.codecs,
                bitrate: stream.bitrate,
                sample_rate: stream.sample_rate,
                content_length: stream.len,
                expires_in_seconds: stream.expires_in_seconds,
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
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeDspCreate(
    _env: JNIEnv,
    _class: JClass,
    sample_rate: jint,
) -> jlong {
    let pipeline = Box::new(AudioPipeline::new(sample_rate.max(8_000) as u32));
    Box::into_raw(pipeline) as jlong
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeDspDestroy(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        unsafe {
            drop(Box::from_raw(handle as *mut AudioPipeline));
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeDspReset(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        let pipeline = unsafe { &mut *(handle as *mut AudioPipeline) };
        pipeline.reset();
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeDspConfigure(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    eq_gains: JFloatArray,
    preamp_db: jfloat,
    auto_headroom: jboolean,
    loudness_gain_db: jdouble,
    normalizer_enabled: jboolean,
) {
    if handle == 0 {
        return;
    }
    let mut gains = [0.0_f32; 10];
    let _ = env.get_float_array_region(&eq_gains, 0, &mut gains);
    let pipeline = unsafe { &mut *(handle as *mut AudioPipeline) };
    pipeline.configure(
        gains,
        preamp_db as f32,
        auto_headroom == JNI_TRUE,
        loudness_gain_db as f64,
        normalizer_enabled == JNI_TRUE,
    );
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeDspProcessPcm16(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    direct_buf: JByteBuffer,
    byte_offset: jint,
    byte_len: jint,
    channels: jint,
    sample_rate: jint,
) {
    if handle == 0 || byte_len <= 1 || channels <= 0 {
        return;
    }
    if let Ok(ptr) = env.get_direct_buffer_address(&direct_buf) {
        let sample_count = (byte_len as usize) / 2;
        if sample_count == 0 {
            return;
        }
        let pcm_ptr = unsafe { ptr.add(byte_offset.max(0) as usize) as *mut i16 };
        let pcm = unsafe { std::slice::from_raw_parts_mut(pcm_ptr, sample_count) };
        let pipeline = unsafe { &mut *(handle as *mut AudioPipeline) };
        pipeline.process_pcm16(pcm, channels as usize, sample_rate.max(8_000) as u32);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_auri_music_data_AuriNativeEngine_nativeDspProcessPcmFloat(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    direct_buf: JByteBuffer,
    byte_offset: jint,
    byte_len: jint,
    channels: jint,
    sample_rate: jint,
) {
    if handle == 0 || byte_len <= 3 || channels <= 0 {
        return;
    }
    if let Ok(ptr) = env.get_direct_buffer_address(&direct_buf) {
        let sample_count = (byte_len as usize) / 4;
        if sample_count == 0 {
            return;
        }
        let pcm_ptr = unsafe { ptr.add(byte_offset.max(0) as usize) as *mut f32 };
        let pcm = unsafe { std::slice::from_raw_parts_mut(pcm_ptr, sample_count) };
        let pipeline = unsafe { &mut *(handle as *mut AudioPipeline) };
        pipeline.process_pcm_float(pcm, channels as usize, sample_rate.max(8_000) as u32);
    }
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

    let client = shared_client();
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
