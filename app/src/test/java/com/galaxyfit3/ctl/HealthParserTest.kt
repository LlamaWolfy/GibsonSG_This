package com.galaxyfit3.ctl

import com.galaxyfit3.ctl.bluetooth.HealthService
import com.galaxyfit3.ctl.data.SleepReading
import com.galaxyfit3.ctl.data.SpO2Reading
import com.galaxyfit3.ctl.data.StepsReading
import com.galaxyfit3.ctl.data.StressReading
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class HealthParserTest {

    // Use reflection to create a HealthService with null ConnectionManager
    // since we only test the parsers which are internal functions.
    // In a real project these parsers would be extracted, but here we test directly.

    private val service = HealthService::class.java
        .getDeclaredConstructor(com.galaxyfit3.ctl.bluetooth.ConnectionManager::class.java)
        .apply { isAccessible = true }
        .newInstance(null as com.galaxyfit3.ctl.bluetooth.ConnectionManager?) as HealthService

    @Test
    fun `parseSteps decodes little-endian payload`() {
        val buf = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(12345)    // steps
        buf.putInt(8900)     // distance in meters
        buf.putShort(550)    // calories
        val result = invokeParseSteps(buf.array())
        assertEquals(StepsReading(12345, 8900, 550), result.copy(timestamp = 0).let {
            StepsReading(it.steps, it.distanceMeters, it.caloriesBurned)
        })
    }

    @Test
    fun `parseSleep decodes five shorts`() {
        val buf = ByteBuffer.allocate(10).order(ByteOrder.LITTLE_ENDIAN)
        buf.putShort(420)  // total
        buf.putShort(90)   // deep
        buf.putShort(200)  // light
        buf.putShort(100)  // rem
        buf.putShort(30)   // awake
        val result = invokeParseSleep(buf.array())
        assertEquals(420, result.totalMinutes)
        assertEquals(90, result.deepMinutes)
        assertEquals(200, result.lightMinutes)
        assertEquals(100, result.remMinutes)
        assertEquals(30, result.awakeMinutes)
    }

    @Test
    fun `parseSpO2 returns percentage from first byte`() {
        val result = invokeParseSpO2(byteArrayOf(97.toByte()))
        assertEquals(97, result.percentage)
    }

    @Test
    fun `parseStress returns level from first byte`() {
        val result = invokeParseStress(byteArrayOf(42.toByte()))
        assertEquals(42, result.level)
    }

    @Test
    fun `parseSteps handles empty data gracefully`() {
        val result = invokeParseSteps(byteArrayOf())
        assertEquals(0, result.steps)
    }

    // --- Helpers to call internal parse methods via reflection ---

    private fun invokeParseSteps(data: ByteArray): StepsReading {
        val method = HealthService::class.java.getDeclaredMethod("parseSteps", ByteArray::class.java)
        method.isAccessible = true
        return method.invoke(service, data) as StepsReading
    }

    private fun invokeParseSleep(data: ByteArray): SleepReading {
        val method = HealthService::class.java.getDeclaredMethod("parseSleep", ByteArray::class.java)
        method.isAccessible = true
        return method.invoke(service, data) as SleepReading
    }

    private fun invokeParseSpO2(data: ByteArray): SpO2Reading {
        val method = HealthService::class.java.getDeclaredMethod("parseSpO2", ByteArray::class.java)
        method.isAccessible = true
        return method.invoke(service, data) as SpO2Reading
    }

    private fun invokeParseStress(data: ByteArray): StressReading {
        val method = HealthService::class.java.getDeclaredMethod("parseStress", ByteArray::class.java)
        method.isAccessible = true
        return method.invoke(service, data) as StressReading
    }
}
