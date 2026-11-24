# AURI Mobile: Premium High-Fidelity Music Player

[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2024%2B)-3DDC84.svg?style=flat-square&logo=android)](https://github.com/SuryakiranMK/AURI-Mobile)
[![Engine](https://img.shields.io/badge/Core-AURI%20Rust%20Engine-DEA584.svg?style=flat-square&logo=rust)](https://github.com/SuryakiranMK/AURI)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20%7C%20Material%203-4285F4.svg?style=flat-square)](https://developer.android.com/jetpack/compose)
[![Audio](https://img.shields.io/badge/Audio-Opus%20160k%20%7C%20AAC%20256k-10B981.svg?style=flat-square)](https://github.com/SuryakiranMK/AURI-Mobile)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

**AURI Mobile** is the mobile evolution of the ultra-lightweight desktop player [AURI](https://github.com/SuryakiranMK/AURI). Re-engineered specifically for mobile audiophiles and modern users, AURI Mobile strips away desktop gaming overlays and Windows-specific hooks while preserving AURI's core superpowers: **direct high-bitrate stream extraction (Opus 160 kbps / AAC 256 kbps)**, **audiophile loudness normalization (-14.0 LUFS)**, and an **exquisite obsidian glassmorphic aesthetic**.

---

## ⚡ Core Features

- **Ported AURI Rust Engine (`auri-core`)**:
  - Uses the same unthrottled VisionOS Innertube stream resolution pipeline from desktop AURI.
  - Fetches true high-fidelity audio streams (Opus itag 251 @ 160 kbps, AAC itag 140 @ 128/256 kbps) with zero ad interruptions.
  - Built-in LRCLIB & YouTube synchronized lyrics extraction.
- **-14.0 LUFS Broadcast Loudness Normalization**:
  - Real-time ITU-R BS.1770 loudness compensation matching YouTube Music desktop broadcast levels.
  - Zero-allocation soft-knee peak limiter preventing digital clipping distortion.
- **10-Band ISO Octave Graphic Equalizer**:
  - Center frequencies: `31Hz, 63Hz, 125Hz, 250Hz, 500Hz, 1kHz, 2kHz, 4kHz, 8kHz, 16kHz`.
  - Presets for Audiophile Flat, Deep Bass Punch, Vocal Clarity, and Cinematic Dynamic.
- **Modern Jetpack Compose UI**:
  - Pure obsidian theme (`#08080B`) with neon electric violet accents (`#8B5CF6`).
  - **Home Screen**: Dynamic Quick Picks carousel, trending tracks, and mood genre filters.
  - **Search**: Instant debounced query parsing with rich track results.
  - **Docked MiniPlayer**: Persistent bottom bar with progress scrub and quick transport controls.
  - **Now Playing HUD**: Full-screen immersive player with hero album art, time scrubber, shuffle, repeat, like, and audio quality tags.
  - **Synchronized Lyrics Drawer**: Auto-scrolling lyrics with electric violet active glow.
  - **Library**: Saved/liked tracks with instant playback and live engine status.

---

## 📁 Repository Structure

```
AURI-Mobile/
├── app/                             # Native Android Application (Jetpack Compose)
│   ├── src/main/kotlin/com/auri/music/
│   │   ├── data/                    # AuriEngineRepository & data providers
│   │   ├── model/                   # Song, Lyrics, PlaybackState, EqPreset models
│   │   ├── player/                  # AuriPlayerController (ExoPlayer + StateFlow)
│   │   ├── ui/
│   │   │   ├── components/          # MiniPlayer, NowPlayingSheet, EqualizerSheet, LyricsView
│   │   │   ├── screens/             # HomeScreen, SearchScreen, LibraryScreen
│   │   │   ├── theme/               # Obsidian & Electric Violet theme palette
│   │   │   └── main/                # MainScreen, MainScreenViewModel, Navigation
│   │   └── MainActivity.kt
│   └── src/test/                    # Android Unit Test Suite
├── auri-core/                       # Ported Rust Engine Core
│   ├── src/
│   │   ├── dsp.rs                   # Normalizer, Soft Limiter & 10-band EQ
│   │   ├── model.rs                 # Data contracts & DTOs
│   │   ├── ytm.rs                   # VisionOS client bypass & Innertube scraper
│   │   └── lib.rs
│   └── Cargo.toml                   # 22 passed unit tests
└── gradle/                          # Gradle wrapper and version catalog
```

---

## 🛠️ Building & Running

### 1. Build & Test Rust Core (`auri-core`)
```powershell
cd auri-core
cargo test

# Cross-compile native JNI shared libraries for Android
cargo build --target x86_64-linux-android --release
cargo build --target aarch64-linux-android --release
```

### 2. Build & Run Android App
The compiled native binaries `libauri_core.so` are placed in `app/src/main/jniLibs/{x86_64,arm64-v8a}/` and automatically linked via `System.loadLibrary("auri_core")` into `AuriNativeEngine`.

```powershell
# Run unit test suite
./gradlew test

# Build debug APK with bundled native Rust engine
./gradlew assembleDebug

# Install on connected device or emulator
./gradlew installDebug
```

The compiled APK will be located at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📜 License

Project AURI Mobile is licensed under the [MIT License](LICENSE).
