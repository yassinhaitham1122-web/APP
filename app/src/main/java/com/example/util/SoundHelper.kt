package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object SoundHelper {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
        } catch (e: Exception) {
            // Fallback gracefully if audio system is unavailable
        }
    }

    fun playCheckInSound(context: Context) {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
            vibrate(context, 100)
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun playCheckOutSound(context: Context) {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 250)
            vibrate(context, 150)
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun playWarningSound(context: Context) {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
            vibrate(context, 200)
        } catch (e: Exception) {
            // Ignored
        }
    }

    private fun vibrate(context: Context, durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        } catch (e: Exception) {
            // Ignored
        }
    }
}
