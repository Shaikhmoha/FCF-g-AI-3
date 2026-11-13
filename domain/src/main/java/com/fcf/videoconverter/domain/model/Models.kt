package com.fcf.videoconverter.domain.model

enum class VideoContainer(val ext: String, val mimeType: String, val media3Supported: Boolean) {
    MP4("mp4", "video/mp4", true),
    MOV("mov", "video/quicktime", false),
    MKV("mkv", "video/x-matroska", false),
    AVI("avi", "video/x-msvideo", false),
    WEBM("webm", "video/webm", false),
    THREE_GP("3gp", "video/3gpp", false)
}

enum class AudioOutputFormat(val ext: String, val mimeType: String) {
    MP3("mp3", "audio/mpeg"),
    WAV("wav", "audio/wav"),
    AAC("m4a", "audio/mp4")
}

enum class ResolutionPreset(val label: String, val height: Int) {
    P360("360p", 360),
    P480("480p", 480),
    P720("720p (Recommended)", 720),
    P1080("1080p", 1080),
    P2160("4K UHD", 2160)
}

enum class VideoCodecOption(val label: String, val mimeType: String) {
    H264("H.264 (AVC)", "video/avc"),
    H265("H.265 (HEVC)", "video/hevc"),
    VP9("VP9", "video/x-vnd.on2.vp9")
}

enum class AspectRatioOption(val label: String, val ratioWidth: Int?, val ratioHeight: Int?) {
    ORIGINAL("Original", null, null),
    RATIO_16_9("16:9", 16, 9),
    RATIO_9_16("9:16", 9, 16),
    RATIO_4_3("4:3", 4, 3),
    RATIO_1_1("1:1", 1, 1)
}

data class ConversionConfig(
    val sourceUri: String,
    val outputName: String,
    val container: VideoContainer = VideoContainer.MP4,
    val resolution: ResolutionPreset = ResolutionPreset.P720,
    val videoBitrateKbps: Int = 2500,
    val frameRate: Int = 30,
    val codec: VideoCodecOption = VideoCodecOption.H264,
    val aspectRatio: AspectRatioOption = AspectRatioOption.ORIGINAL,
    val audioBitrateKbps: Int = 128
)

data class CompressionConfig(
    val sourceUri: String,
    val outputName: String,
    val resolution: ResolutionPreset = ResolutionPreset.P720,
    val targetVideoBitrateKbps: Int = 1400,
    val audioBitrateKbps: Int = 96
)

enum class DspVoicePreset(val label: String, val pitchSemitones: Float, val speedFactor: Float, val toneGainDb: Float) {
    CUSTOM("Custom DSP", 0f, 1.0f, 0f),
    MALE_DEEP("Male Voice Effect (DSP)", -4.0f, 1.0f, 1.5f),
    FEMALE_BRIGHT("Female Voice Effect (DSP)", 4.5f, 1.0f, 2.0f)
}

data class VoiceEffectConfig(
    val preset: DspVoicePreset = DspVoicePreset.CUSTOM,
    val pitchSemitones: Float = 0f,
    val speedFactor: Float = 1.0f,
    val toneGainDb: Float = 0f,
    val noiseReductionEnabled: Boolean = true,
    val voiceEnhancementEnabled: Boolean = true,
    val normalizeAudio: Boolean = true,
    val outputFormat: AudioOutputFormat = AudioOutputFormat.WAV
)

enum class AiLanguage(val code: String, val displayName: String) {
    BANGLA("bn", "বাংলা (Bangla)"),
    ENGLISH("en", "English"),
    HINDI("hi", "हिन्दी (Hindi)")
}

enum class LanguagePair(val source: AiLanguage, val target: AiLanguage) {
    BN_TO_EN(AiLanguage.BANGLA, AiLanguage.ENGLISH),
    EN_TO_BN(AiLanguage.ENGLISH, AiLanguage.BANGLA),
    BN_TO_HI(AiLanguage.BANGLA, AiLanguage.HINDI),
    HI_TO_BN(AiLanguage.HINDI, AiLanguage.BANGLA),
    EN_TO_HI(AiLanguage.ENGLISH, AiLanguage.HINDI),
    HI_TO_EN(AiLanguage.HINDI, AiLanguage.ENGLISH)
}

data class SubtitleCue(
    val index: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String
)

data class SubtitleStyle(
    val fontSizeSp: Int = 20,
    val colorHex: String = "#FFFFFF",
    val positionBottomPercent: Int = 10
)

data class DubbingConfig(
    val sourceVideoUri: String,
    val targetLanguage: AiLanguage,
    val keepOriginalAudio: Boolean = true,
    val originalVolume: Float = 0.25f,
    val dubbedVolume: Float = 1.0f,
    val preferFemaleVoice: Boolean = false,
    val generateSubtitles: Boolean = true
)

enum class AiModelCapability {
    ASR_SPEECH_TO_TEXT,
    TRANSLATION,
    TTS_VOICE_SYNTHESIS
}

enum class AiModelStatus {
    NOT_INSTALLED,
    VERIFIED_READY,
    CORRUPTED,
    INSUFFICIENT_RAM
}

data class AiModelSpec(
    val id: String,
    val displayName: String,
    val capability: AiModelCapability,
    val supportedLanguages: List<AiLanguage>,
    val expectedSizeBytes: Long,
    val estimatedRamMb: Long,
    val license: String,
    val formatHint: String
)

data class InstalledAiModel(
    val specId: String,
    val localPath: String,
    val fileSizeBytes: Long,
    val sha256: String,
    val status: AiModelStatus,
    val installedAtEpochMs: Long
)

enum class ProjectTaskType {
    CONVERT, COMPRESS, VOICE_DSP, TRANSLATE, DUBBING, SUBTITLES, EXTRACT_AUDIO, EDIT_TRIM, EDIT_MERGE
}

enum class ProjectStatus {
    QUEUED, RUNNING, COMPLETED, CANCELLED, FAILED
}

data class ProjectRecord(
    val id: String,
    val title: String,
    val taskType: ProjectTaskType,
    val sourceUri: String,
    val outputUri: String?,
    val originalSizeBytes: Long,
    val outputSizeBytes: Long,
    val status: ProjectStatus,
    val progressPercent: Int,
    val resumablePayloadJson: String?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long
)

sealed interface ProcessingState {
    data object Idle : ProcessingState
    data class Running(
        val progressPercent: Int,
        val etaSeconds: Long?,
        val stageLabel: String
    ) : ProcessingState
    data class Completed(val outputUri: String, val outputSizeBytes: Long) : ProcessingState
    data class Failed(val message: String, val recoverable: Boolean = true) : ProcessingState
    data object Cancelled : ProcessingState
}
