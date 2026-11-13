# FCF Video Converter — Offline AI-Powered Android Application

**FCF Video Converter** is a privacy-first, 100% offline Android multimedia studio built in **Kotlin** and **Jetpack Compose** with **Clean Architecture + MVVM**, targeting **Android 8.0 (API 26)+** and specifically tuned for mid-range devices with **6 GB RAM and 128 GB internal storage**.

---

## 1. Strict Offline & Privacy Guarantee

- **Zero `INTERNET` permission** in `AndroidManifest.xml`.
- Zero cloud servers, zero telemetry, zero mandatory login, zero silent downloads.
- All media files are accessed via Android's **Storage Access Framework (SAF)** and exported via **MediaStore** / SAF without modifying or deleting original source files.
- All AI model packages are imported manually from local storage (`.fcfmodel` / `.onnx` / `.bin`) and verified via **SHA-256 checksum** before use.

---

## 2. Multi-Module Architecture (15 Modules)

| Module | Responsibility |
| :--- | :--- |
| `:app` | Compose entry point, 12-feature dashboard, navigation host, `FcfApplication` |
| `:core` | Purple Material 3 Light/Dark theme, `MemoryMonitor` (6 GB RAM guard), `StorageManager`, `LocaleManager` (EN/BN) |
| `:domain` | Pure Kotlin domain models, configuration specs, repository & engine contracts |
| `:data` | Room Database (`FcfDatabase`, `ProjectDao`, `InstalledModelDao`), settings persistence |
| `:media_engine` | `Media3VideoProcessor` (Apache 2.0), `DspVoiceProcessor` (PCM DSP), `SrtCodec`, `MediaProcessingWorker` (WorkManager) |
| `:ai_engine` | `OnnxSessionManager` (single-model memory guard), `ChunkedAudioPipeline`, `ModelIntegrityVerifier`, ASR/Translation/TTS adapters |
| `:feature_converter` | Format/codec/resolution/bitrate/FPS/aspect-ratio video conversion UI & ViewModel |
| `:feature_compressor` | Quality preset, custom resolution/bitrate, estimated size & before/after comparison |
| `:feature_voice` | Mic recording, WAV/audio import, offline DSP effects (explicitly labeled DSP vs. AI) |
| `:feature_translation` | Offline Bangla ↔ English ↔ Hindi translation workbench (requires local model install) |
| `:feature_dubbing` | Audio extraction → transcription → translation → TTS → timeline mix pipeline |
| `:feature_subtitles` | Cue editor, timing offset, style controls, SRT export, subtitle burn-in |
| `:feature_editor` | Trim, merge, crop, resize, rotate, aspect ratio, text overlay, filters, audio extraction |
| `:feature_projects` | Room-backed project history, export history, thumbnails, resume/cleanup |
| `:feature_settings` | Light/Dark theme, English/Bangla language, storage manager, AI Model Manager |

---

## 3. Licensing & Engine Compatibility Audit

| Component | License | Android API 26+ Status | Role |
| :--- | :--- | :--- | :--- |
| **AndroidX Media3 Transformer (`1.4.1`)** | Apache 2.0 | Verified hardware `MediaCodec` | Primary MP4/H.264/HEVC/AAC convert, compress, trim, crop, rotate, merge, audio extract |
| **Optional FFmpeg JNI (`MediaEngineStrategy`)** | LGPL 2.1+ / GPL v3 (depending on build flags) | Requires local prebuilt `.so` AAR | Extended containers (`MKV`, `AVI`, `WEBM`, `3GP`), `libmp3lame` MP3 encoding, `libass` SRT burn-in |
| **ONNX Runtime Mobile (`1.19.2`)** | MIT | arm64-v8a / armeabi-v7a | Local quantized INT8/FP16 inference for translation & TTS models |
| **Whisper.cpp (GGML JNI adapter)** | MIT | arm64-v8a NEON | Multilingual ASR (`bn`, `en`, `hi`) using `ggml-tiny-q5_1.bin` (~32 MB) or `ggml-base-q5_1.bin` (~60 MB) |
| **VITS / Piper ONNX Voices** | MIT | arm64-v8a | Local TTS speech synthesis (installable per language pack) |
| **NLLB-200-distilled-600M (INT8)** | CC-BY-NC 4.0 | ~620 MB quantized | Multilingual `bn ↔ en ↔ hi` translation; requires sequential loading on 6 GB RAM |

> **Honest Engineering Note on 6 GB RAM**:
> Android typically grants ~2.5–3.2 GB of available native+Java memory budget to a foreground app on a 6 GB RAM device. Running Whisper + NLLB + VITS simultaneously would trigger `LMK` (Low Memory Killer). Therefore, `OnnxSessionManager` enforces **strict sequential model loading** (`unloadCurrentModel()` before loading the next stage) and `ChunkedAudioPipeline` splits long videos into **30-second 16 kHz mono PCM windows**.

---

## 4. Offline AI Model Installation (No Internet Required)

1. Copy the verified offline model archive (e.g., `whisper-base-q5_1.bin`, `nllb-600m-int8.onnx`, `piper-bn-in.onnx`) onto your phone's `Download/FCFModels/` folder via USB cable, SD card, or local file transfer.
2. Open **FCF Video Converter → Settings → AI Model Manager**.
3. Tap **Install from Local Storage** and pick the file using Android's system file picker.
4. The app computes its **SHA-256 digest**, registers it in Room Database, and unlocks the corresponding offline AI pipeline.

---

## 5. Build Instructions (Android Studio / Gradle)

### Prerequisites
- Android Studio Ladybug (2024.2+) or newer
- JDK 17
- Android SDK Platform 35 + Build Tools 35.0.0 (Min SDK 26)

### Build Debug & Release APKs
```bash
cd android-project
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# For Release APK (configure signingConfigs in app/build.gradle.kts first):
./gradlew assembleRelease
```

---

## 6. Feature Implementation Status

- **Implemented in Kotlin/Compose**: Full 15-module architecture, Material 3 purple Light/Dark themes, English + Bangla (`values-bn`) string resources, Room DB persistence, SAF/MediaStore I/O, Media3 Transformer conversion/compression/editing/audio extraction, real PCM DSP voice processor (`DspVoiceProcessor`), SRT parser/editor/exporter, `MemoryMonitor`, `StorageManager`, `WorkManager` foreground processing, and SHA-256 local model installer.
- **Requires Local Model Package & Device Testing**: Whisper/NLLB/TTS native `.so`/`.onnx` model files are not bundled inside source control due to binary size (32 MB–620 MB each) and must be imported locally via AI Model Manager and benchmarked on physical 6 GB RAM hardware.
- **Note on Compilation in AI Studio Web Sandbox**: The AI Studio cloud container includes Node.js and FFmpeg (powering the live localhost FCF Video Converter web workbench), while Android SDK/Gradle/JDK are not installed in this container; therefore APK compilation must be run locally in Android Studio or a CI runner with Android SDK 35.
