package com.example.dim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DimMathTest {
    @Test
    fun automaticOpacityRespectsLimitAndFallsWithLight() {
        assertEquals(90, opacityForLight(MAX_OPACITY, 0f))
        assertTrue(opacityForLight(70, 1f) > opacityForLight(70, 20f))
        assertEquals(0, opacityForLight(70, 1000f))
        assertEquals(0, opacityForLight(0, 0f))
    }

    @Test
    fun restorationReversesDimmingAndClampsBrightPixels() {
        assertEquals(200, restoreChannel(130, 35))
        assertEquals(255, restoreChannel(200, 70))
        assertEquals(42, restoreChannel(42, 0))
        assertEquals(200, restoreChannel(20, MAX_OPACITY))
        assertEquals(0, restoreChannel(0, MAX_OPACITY))
    }
}
