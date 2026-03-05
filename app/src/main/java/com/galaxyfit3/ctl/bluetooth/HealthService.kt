package com.galaxyfit3.ctl.bluetooth

import com.galaxyfit3.ctl.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Reads health data from the Galaxy Fit3 over BLE.
 *
 * Heart rate uses the standard BLE Heart Rate Measurement characteristic
 * (0x2A37). Steps, sleep, SpO2, and stress use Samsung proprietary
 * characteristics under the Samsung Health Service.
 */
class HealthService(private val conn: ConnectionManager) {

    private val _heartRate = MutableStateFlow<HeartRateReading?>(null)
    val heartRate: StateFlow<HeartRateReading?> = _heartRate.asStateFlow()

    /**
     * Subscribe to real-time heart rate notifications.
     */
    fun startHeartRateMonitor() {
        conn.enableNotifications(
            GattUuids.HEART_RATE_SERVICE,
            GattUuids.HEART_RATE_MEASUREMENT
        ) { data ->
            val reading = parseHeartRate(data)
            _heartRate.value = reading
        }
    }

    fun stopHeartRateMonitor() {
        conn.disableNotifications(
            GattUuids.HEART_RATE_SERVICE,
            GattUuids.HEART_RATE_MEASUREMENT
        )
    }

    suspend fun readSteps(): StepsReading {
        val data = conn.readCharacteristic(
            GattUuids.SAMSUNG_HEALTH_SERVICE,
            GattUuids.SAMSUNG_STEPS_CHAR
        )
        return parseSteps(data)
    }

    suspend fun readSleep(): SleepReading {
        val data = conn.readCharacteristic(
            GattUuids.SAMSUNG_HEALTH_SERVICE,
            GattUuids.SAMSUNG_SLEEP_CHAR
        )
        return parseSleep(data)
    }

    suspend fun readSpO2(): SpO2Reading {
        val data = conn.readCharacteristic(
            GattUuids.SAMSUNG_HEALTH_SERVICE,
            GattUuids.SAMSUNG_SPO2_CHAR
        )
        return parseSpO2(data)
    }

    suspend fun readStress(): StressReading {
        val data = conn.readCharacteristic(
            GattUuids.SAMSUNG_HEALTH_SERVICE,
            GattUuids.SAMSUNG_STRESS_CHAR
        )
        return parseStress(data)
    }

    suspend fun readDeviceInfo(): DeviceInfo {
        val manufacturer = conn.readCharacteristic(
            GattUuids.DEVICE_INFO_SERVICE, GattUuids.MANUFACTURER_NAME
        ).decodeToString()
        val model = conn.readCharacteristic(
            GattUuids.DEVICE_INFO_SERVICE, GattUuids.MODEL_NUMBER
        ).decodeToString()
        val firmware = conn.readCharacteristic(
            GattUuids.DEVICE_INFO_SERVICE, GattUuids.FIRMWARE_REVISION
        ).decodeToString()
        val hardware = conn.readCharacteristic(
            GattUuids.DEVICE_INFO_SERVICE, GattUuids.HARDWARE_REVISION
        ).decodeToString()
        val software = conn.readCharacteristic(
            GattUuids.DEVICE_INFO_SERVICE, GattUuids.SOFTWARE_REVISION
        ).decodeToString()
        val batteryData = conn.readCharacteristic(
            GattUuids.BATTERY_SERVICE, GattUuids.BATTERY_LEVEL
        )
        val battery = if (batteryData.isNotEmpty()) batteryData[0].toInt() and 0xFF else -1

        return DeviceInfo(manufacturer, model, firmware, hardware, software, battery)
    }

    // --- Parsers ---

    private fun parseHeartRate(data: ByteArray): HeartRateReading {
        if (data.isEmpty()) return HeartRateReading(0, false)
        val flags = data[0].toInt() and 0xFF
        val is16Bit = (flags and 0x01) != 0
        val sensorContact = (flags and 0x06) == 0x06
        val bpm = if (is16Bit && data.size >= 3) {
            (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
        } else if (data.size >= 2) {
            data[1].toInt() and 0xFF
        } else {
            0
        }
        return HeartRateReading(bpm, sensorContact)
    }

    internal fun parseSteps(data: ByteArray): StepsReading {
        if (data.size < 10) return StepsReading(0, 0, 0)
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val steps = buf.getInt(0)
        val distance = buf.getInt(4)
        val calories = buf.getShort(8).toInt() and 0xFFFF
        return StepsReading(steps, distance, calories)
    }

    internal fun parseSleep(data: ByteArray): SleepReading {
        if (data.size < 10) return SleepReading(0, 0, 0, 0, 0)
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val total = buf.getShort(0).toInt() and 0xFFFF
        val deep = buf.getShort(2).toInt() and 0xFFFF
        val light = buf.getShort(4).toInt() and 0xFFFF
        val rem = buf.getShort(6).toInt() and 0xFFFF
        val awake = buf.getShort(8).toInt() and 0xFFFF
        return SleepReading(total, deep, light, rem, awake)
    }

    internal fun parseSpO2(data: ByteArray): SpO2Reading {
        if (data.isEmpty()) return SpO2Reading(0)
        return SpO2Reading(data[0].toInt() and 0xFF)
    }

    internal fun parseStress(data: ByteArray): StressReading {
        if (data.isEmpty()) return StressReading(0)
        return StressReading(data[0].toInt() and 0xFF)
    }
}
