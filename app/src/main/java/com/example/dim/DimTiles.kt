package com.example.dim

import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

abstract class DimTile : TileService() {
    protected open fun available() = DimSettings.connected
    protected abstract fun active(): Boolean
    protected abstract fun toggle()

    override fun onStartListening() = update()
    override fun onTileAdded() = update()

    override fun onClick() {
        unlockAndRun {
            if (available()) {
                toggle()
                update()
            }
        }
    }

    private fun update() {
        qsTile?.apply {
            state = when {
                !available() -> Tile.STATE_UNAVAILABLE
                active() -> Tile.STATE_ACTIVE
                else -> Tile.STATE_INACTIVE
            }
            updateTile()
        }
    }
}

class FilterTile : DimTile() {
    override fun active() = DimSettings.enabled(this) && !DimSettings.overlayFailed
    override fun toggle() = DimSettings.setEnabled(this, !DimSettings.enabled(this))
}

class AutomaticTile : DimTile() {
    override fun available() = super.available() &&
        getSystemService(SensorManager::class.java).getDefaultSensor(Sensor.TYPE_LIGHT) != null

    override fun active() = DimSettings.automatic(this)
    override fun toggle() = DimSettings.setAutomatic(this, !active())
}

class ScreenshotTile : DimTile() {
    override fun available() = super.available() && Build.VERSION.SDK_INT >= 31
    override fun active() = false
    override fun toggle() {
        DimService.current?.capture()
    }
}
