package com.example.dim

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import java.lang.ref.WeakReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class DimService : AccessibilityService(), SensorEventListener {
    private val windows by lazy { getSystemService(WindowManager::class.java) }
    private val sensors by lazy { getSystemService(SensorManager::class.java) }
    private val lightSensor by lazy { sensors.getDefaultSensor(Sensor.TYPE_LIGHT) }
    private val main = Handler(Looper.getMainLooper())
    private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> refresh() }
    private var overlay: View? = null
    private var lux: Float? = null
    private var readingLight = false
    private var takingScreenshot = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = WeakReference(this)
        DimSettings.connected = true
        DimSettings.preferences(this).registerOnSharedPreferenceChangeListener(settingsListener)
        refresh()
    }

    @Suppress("DEPRECATION")
    private fun refresh() {
        val enabled = DimSettings.enabled(this)
        val light = lightSensor
        val needLight = enabled && DimSettings.automatic(this) && light != null
        if (needLight && !readingLight) {
            readingLight = sensors.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
        } else if (!needLight && readingLight) {
            sensors.unregisterListener(this)
            readingLight = false
            lux = null
        }

        val limit = DimSettings.opacity(this)
        val opacity = if (enabled && !takingScreenshot) {
            lux?.let { opacityForLight(limit, it) } ?: limit
        } else {
            0
        }
        if (opacity == 0) {
            removeOverlay()
            DimSettings.overlayFailed = false
            return
        }

        val existing = overlay
        if (existing != null) {
            existing.alpha = opacity / 100f
            DimSettings.appliedOpacity = opacity
            return
        }

        val layer = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = opacity / 100f
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            if (Build.VERSION.SDK_INT < 30) {
                systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            when {
                Build.VERSION.SDK_INT >= 30 -> {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    setFitInsetsTypes(0)
                }
                Build.VERSION.SDK_INT >= 28 -> {
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                }
            }
        }
        try {
            windows.addView(layer, params)
            overlay = layer
            DimSettings.appliedOpacity = opacity
            DimSettings.overlayFailed = false
        } catch (error: WindowManager.BadTokenException) {
            Log.w("Dim", "Overlay token unavailable", error)
            DimSettings.overlayFailed = true
        }
    }

    private fun removeOverlay() {
        overlay?.let(windows::removeViewImmediate)
        overlay = null
        DimSettings.appliedOpacity = 0
    }

    internal fun capture() {
        if (Build.VERSION.SDK_INT < 31 || takingScreenshot) return
        if (!performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)) {
            Toast.makeText(this, R.string.capture_failed, Toast.LENGTH_SHORT).show()
            return
        }

        takingScreenshot = true
        refresh()
        val timeout = Runnable {
            takingScreenshot = false
            refresh()
            Toast.makeText(this, R.string.capture_failed, Toast.LENGTH_SHORT).show()
        }
        main.postDelayed(timeout, 5000)
        // shortcut: allow the shade animation to settle, replace with a completion signal if available.
        main.postDelayed({
            val callback = object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val pending = main.hasCallbacks(timeout)
                    main.removeCallbacks(timeout)
                    if (!pending) {
                        result.hardwareBuffer.close()
                        return
                    }
                    takingScreenshot = false
                    refresh()
                    Dispatchers.IO.asExecutor().execute {
                        val saved = result.hardwareBuffer.use { buffer ->
                            runCatching {
                                val bitmap = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                                    ?: return@runCatching false
                                try {
                                    saveScreenshot(this@DimService, bitmap)
                                } finally {
                                    bitmap.recycle()
                                }
                            }.getOrDefault(false)
                        }
                        main.post {
                            if (current === this@DimService) {
                                Toast.makeText(
                                    this@DimService,
                                    if (saved) R.string.capture_saved else R.string.capture_failed,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    if (main.hasCallbacks(timeout)) {
                        main.removeCallbacks(timeout)
                        timeout.run()
                    }
                }
            }
            try {
                takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, callback)
            } catch (error: SecurityException) {
                Log.w("Dim", "Screenshot permission unavailable", error)
                callback.onFailure(ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS)
            }
        }, 500)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val reading = event.values[0]
        if (readingLight && reading.isFinite()) {
            lux = reading
            refresh()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        DimSettings.preferences(this).unregisterOnSharedPreferenceChangeListener(settingsListener)
        sensors.unregisterListener(this)
        removeOverlay()
        if (current === this) {
            instance.clear()
            DimSettings.connected = false
            DimSettings.overlayFailed = false
        }
        super.onDestroy()
    }

    companion object {
        private var instance = WeakReference<DimService>(null)
        internal val current: DimService? get() = instance.get()
    }
}
