package com.example.petmeds.ui.play

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class GameSound { CATCH, HIT, START }

/**
 * Synthesises short mono PCM tones at runtime and plays them through
 * [AudioTrack]. Tone buffers are computed once on first use and cached, so
 * playback is allocation-free after warmup. No binary audio assets ship
 * with the app.
 *
 * Respects the device ringer mode (skipped when SILENT) and the user's
 * "Game sound" preference (gated by the caller via [setEnabled]).
 */
@Singleton
class GameAudio @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    @Volatile
    private var enabled: Boolean = true

    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "GameAudio").apply { isDaemon = true }
    }

    private val buffers: Map<GameSound, ShortArray> by lazy {
        mapOf(
            GameSound.CATCH to renderCatch(),
            GameSound.HIT to renderHit(),
            GameSound.START to renderStart(),
        )
    }

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    fun play(sound: GameSound) {
        if (!enabled) return
        if (audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT) return
        val pcm = buffers[sound] ?: return
        executor.execute {
            try {
                playPcmBlocking(pcm)
            } catch (_: Throwable) {
                // Audio is a nice-to-have — never let a playback hiccup crash the game loop.
            }
        }
    }

    fun release() {
        executor.shutdownNow()
    }

    private fun playPcmBlocking(pcm: ShortArray) {
        val byteCount = pcm.size * Short.SIZE_BYTES
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_HZ,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        ).coerceAtLeast(byteCount)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE_HZ)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            val durationMs = (pcm.size * 1000L) / SAMPLE_RATE_HZ + 50L
            Thread.sleep(durationMs)
        } finally {
            try { track.stop() } catch (_: Throwable) {}
            track.release()
        }
    }

    // ── Tone rendering ───────────────────────────────────────────────────────

    /** Bright ~120 ms catch ding: 880 Hz sine with short attack + exponential decay. */
    private fun renderCatch(): ShortArray {
        val durationMs = 130
        val samples = SAMPLE_RATE_HZ * durationMs / 1000
        val out = ShortArray(samples)
        val freq = 880.0
        val attackSamples = SAMPLE_RATE_HZ * 8 / 1000
        val decayTau = samples * 0.30
        for (i in 0 until samples) {
            val t = i.toDouble() / SAMPLE_RATE_HZ
            val attack = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
            val decay = exp(-(i - attackSamples).coerceAtLeast(0).toDouble() / decayTau)
            val envelope = attack * decay
            val sample = sin(2.0 * PI * freq * t) * envelope * 0.65
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** Heavy ~420 ms death tone: descending sine 440→110 Hz, slow decay. */
    private fun renderHit(): ShortArray {
        val durationMs = 420
        val samples = SAMPLE_RATE_HZ * durationMs / 1000
        val out = ShortArray(samples)
        val fStart = 440.0
        val fEnd = 110.0
        val decayTau = samples * 0.55
        var phase = 0.0
        for (i in 0 until samples) {
            val progress = i.toDouble() / samples
            val freq = fStart + (fEnd - fStart) * progress
            phase += 2.0 * PI * freq / SAMPLE_RATE_HZ
            val envelope = exp(-i.toDouble() / decayTau)
            val sample = sin(phase) * envelope * 0.75
            out[i] = (sample * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** Two quick beeps: 660 Hz then 990 Hz, ~60 ms each, 30 ms gap. */
    private fun renderStart(): ShortArray {
        val beepMs = 60
        val gapMs = 30
        val beepSamples = SAMPLE_RATE_HZ * beepMs / 1000
        val gapSamples = SAMPLE_RATE_HZ * gapMs / 1000
        val total = beepSamples * 2 + gapSamples
        val out = ShortArray(total)

        fun writeBeep(offset: Int, freq: Double) {
            for (i in 0 until beepSamples) {
                val t = i.toDouble() / SAMPLE_RATE_HZ
                val attack = if (i < SAMPLE_RATE_HZ * 4 / 1000) {
                    i.toDouble() / (SAMPLE_RATE_HZ * 4 / 1000)
                } else 1.0
                val release = 1.0 - i.toDouble() / beepSamples
                val envelope = attack * release.coerceAtLeast(0.0)
                val sample = sin(2.0 * PI * freq * t) * envelope * 0.6
                out[offset + i] = (sample * Short.MAX_VALUE).toInt().toShort()
            }
        }

        writeBeep(0, 660.0)
        writeBeep(beepSamples + gapSamples, 990.0)
        return out
    }

    private companion object {
        const val SAMPLE_RATE_HZ = 22_050
    }
}
