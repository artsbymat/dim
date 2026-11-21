package com.example.dim

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import kotlin.math.ln
import kotlin.math.roundToInt

internal const val MAX_OPACITY = 90

internal object DimSettings {
    var connected by mutableStateOf(false)
    var overlayFailed by mutableStateOf(false)

    fun preferences(context: Context) = context.getSharedPreferences("dim", Context.MODE_PRIVATE)
    fun enabled(context: Context) = preferences(context).getBoolean("enabled", false)
    fun automatic(context: Context) = preferences(context).getBoolean("automatic", false)
    fun opacity(context: Context) = preferences(context).getInt("opacity", 35).coerceIn(0, MAX_OPACITY)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit { putBoolean("enabled", enabled) }
    }

    fun setAutomatic(context: Context, automatic: Boolean) {
        preferences(context).edit { putBoolean("automatic", automatic) }
    }

    fun setOpacity(context: Context, opacity: Int) {
        preferences(context).edit { putInt("opacity", opacity.coerceIn(0, MAX_OPACITY)) }
    }
}

internal fun opacityForLight(limit: Int, lux: Float): Int {
    val light = lux.coerceIn(0f, 200f)
    return (limit * (1 - ln(1.0 + light) / ln(201.0))).roundToInt()
}

internal fun restoreChannel(value: Int, opacity: Int): Int =
    (value * 100f / (100 - opacity.coerceIn(0, MAX_OPACITY))).roundToInt().coerceAtMost(255)
