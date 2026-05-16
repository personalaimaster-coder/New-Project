package com.example.petmeds.ui.play

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around the platform [Vibrator] for the Treat Catcher game.
 * Picks the appropriate API per OS version (minSdk 26) and respects the
 * user's "Game vibration" preference (gated by the caller via [setEnabled]).
 */
@Singleton
class GameHaptics @Inject constructor(
    @ApplicationContext context: Context,
) {

    @Volatile
    private var enabled: Boolean = true

    private val vibrator: Vibrator? = resolveVibrator(context)

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    /** Short, snappy tick for lane-change / treat-catch. */
    fun lightTick() {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        } else {
            VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        try { v.vibrate(effect) } catch (_: Throwable) {}
    }

    /** Slightly more substantial pulse for game start. */
    fun start() {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        try {
            v.vibrate(VibrationEffect.createOneShot(40L, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Throwable) {}
    }

    /** Heavy double-pulse for poison death. */
    fun heavy() {
        val v = vibrator ?: return
        if (!enabled || !v.hasVibrator()) return
        try {
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0L, 60L, 40L, 120L), -1))
        } catch (_: Throwable) {}
    }

    private fun resolveVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                ?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
