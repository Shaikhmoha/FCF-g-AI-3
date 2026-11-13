package com.fcf.videoconverter.media_engine

import com.fcf.videoconverter.domain.model.VoiceEffectConfig
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Genuine offline PCM 16-bit Digital Signal Processing (DSP) engine.
 * Intentionally separated from neural/AI voice conversion so pitch/formant shifts
 * are never falsely presented as AI voice cloning.
 */
class DspVoiceProcessor {

    fun processPcm16ToWav(
        inputSamples: ShortArray,
        sampleRateHz: Int,
        config: VoiceEffectConfig,
        outputWavFile: File
    ): File {
        var working = FloatArray(inputSamples.size) { inputSamples[it] / 32768f }

        if (config.noiseReductionEnabled) {
            working = applyNoiseGateAndHighPass(working)
        }

        // Pitch shift via resampling + simple time compensation
        val pitchFactor = 2.0.pow(config.pitchSemitones / 12.0).toFloat().coerceIn(0.5f, 2.0f)
        if (abs(pitchFactor - 1.0f) > 0.01f) {
            working = resampleLinear(working, pitchFactor)
        }

        if (abs(config.speedFactor - 1.0f) > 0.01f) {
            working = resampleLinear(working, config.speedFactor.coerceIn(0.5f, 2.0f))
        }

        if (config.voiceEnhancementEnabled || abs(config.toneGainDb) > 0.1f) {
            working = applyPresenceEmphasis(working, config.toneGainDb)
        }

        if (config.normalizeAudio) {
            working = peakNormalize(working, targetPeak = 0.89f)
        }

        val pcmOut = ShortArray(working.size) { i ->
            (working[i].coerceIn(-1.0f, 1.0f) * 32767f).roundToInt().toShort()
        }
        writeWavFile(outputWavFile, pcmOut, sampleRateHz)
        return outputWavFile
    }

    private fun applyNoiseGateAndHighPass(samples: FloatArray): FloatArray {
        val out = FloatArray(samples.size)
        var prevX = 0f
        var prevY = 0f
        val alpha = 0.96f
        val gateThreshold = 0.014f
        for (i in samples.indices) {
            val hp = alpha * (prevY + samples[i] - prevX)
            prevX = samples[i]
            prevY = hp
            out[i] = if (abs(hp) < gateThreshold) hp * 0.2f else hp
        }
        return out
    }

    private fun resampleLinear(samples: FloatArray, rate: Float): FloatArray {
        val outLen = (samples.size / rate).roundToInt().coerceAtLeast(1)
        val out = FloatArray(outLen)
        for (i in 0 until outLen) {
            val srcPos = i * rate
            val idx0 = srcPos.toInt().coerceIn(0, samples.lastIndex)
            val idx1 = (idx0 + 1).coerceIn(0, samples.lastIndex)
            val frac = srcPos - idx0
            out[i] = samples[idx0] * (1f - frac) + samples[idx1] * frac
        }
        return out
    }

    private fun applyPresenceEmphasis(samples: FloatArray, toneGainDb: Float): FloatArray {
        val gain = 10.0.pow(toneGainDb / 20.0).toFloat().coerceIn(0.6f, 2.2f)
        val out = FloatArray(samples.size)
        var last = 0f
        for (i in samples.indices) {
            val diff = samples[i] - last
            last = samples[i]
            out[i] = (samples[i] + diff * 0.18f * gain).coerceIn(-1f, 1f)
        }
        return out
    }

    private fun peakNormalize(samples: FloatArray, targetPeak: Float): FloatArray {
        var maxAbs = 0.0001f
        for (s in samples) {
            val a = abs(s)
            if (a > maxAbs) maxAbs = a
        }
        val scale = targetPeak / maxAbs
        return FloatArray(samples.size) { (samples[it] * scale).coerceIn(-1f, 1f) }
    }

    private fun writeWavFile(file: File, pcm: ShortArray, sampleRateHz: Int) {
        val byteDataLen = pcm.size * 2
        val totalDataLen = byteDataLen + 36
        RandomAccessFile(file, "rw").use { raf ->
            raf.setLength(0)
            raf.writeBytes("RIFF")
            raf.writeIntLe(totalDataLen)
            raf.writeBytes("WAVE")
            raf.writeBytes("fmt ")
            raf.writeIntLe(16)
            raf.writeShortLe(1)
            raf.writeShortLe(1)
            raf.writeIntLe(sampleRateHz)
            raf.writeIntLe(sampleRateHz * 2)
            raf.writeShortLe(2)
            raf.writeShortLe(16)
            raf.writeBytes("data")
            raf.writeIntLe(byteDataLen)
            val bytes = ByteArray(byteDataLen)
            for (i in pcm.indices) {
                val s = pcm[i].toInt()
                bytes[i * 2] = (s and 0xFF).toByte()
                bytes[i * 2 + 1] = ((s shr 8) and 0xFF).toByte()
            }
            raf.write(bytes)
        }
    }

    private fun RandomAccessFile.writeIntLe(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
        write((value shr 16) and 0xFF)
        write((value shr 24) and 0xFF)
    }

    private fun RandomAccessFile.writeShortLe(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }
}
