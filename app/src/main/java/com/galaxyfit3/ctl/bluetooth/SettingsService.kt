package com.galaxyfit3.ctl.bluetooth

import com.galaxyfit3.ctl.data.DeviceSettings

/**
 * Read and modify Galaxy Fit3 device settings over BLE.
 */
class SettingsService(private val conn: ConnectionManager) {

    suspend fun readSettings(): DeviceSettings {
        val dndData = conn.readCharacteristic(
            GattUuids.SAMSUNG_SETTINGS_SERVICE,
            GattUuids.SAMSUNG_DND_CHAR
        )
        val vibData = conn.readCharacteristic(
            GattUuids.SAMSUNG_SETTINGS_SERVICE,
            GattUuids.SAMSUNG_VIBRATION_CHAR
        )
        val brightData = conn.readCharacteristic(
            GattUuids.SAMSUNG_SETTINGS_SERVICE,
            GattUuids.SAMSUNG_SCREEN_BRIGHTNESS_CHAR
        )
        val faceData = conn.readCharacteristic(
            GattUuids.SAMSUNG_SETTINGS_SERVICE,
            GattUuids.SAMSUNG_WATCH_FACE_CHAR
        )

        return DeviceSettings(
            dndEnabled = dndData.isNotEmpty() && dndData[0].toInt() != 0,
            vibrationIntensity = if (vibData.isNotEmpty()) vibData[0].toInt() and 0xFF else 2,
            screenBrightness = if (brightData.isNotEmpty()) brightData[0].toInt() and 0xFF else 5,
            watchFaceIndex = if (faceData.isNotEmpty()) faceData[0].toInt() and 0xFF else 0
        )
    }

    suspend fun setDoNotDisturb(enabled: Boolean) {
        conn.writeCharacteristic(
            GattUuids.SAMSUNG_SETTINGS_SERVICE,
            GattUuids.SAMSUNG_DND_CHAR,
            byteArrayOf(if (enabled) 1 else 0)
        )
    }

    /**
     * @param intensity 0=off, 1=low, 2=medium, 3=high
     */
    suspend fun setVibrationIntensity(intensity: Int) {
        require(intensity in 0..3) { "Vibration intensity must be 0-3" }
        conn.writeCharacteristic(
            GattUuids.SAMSUNG_SETTINGS_SERVICE,
            GattUuids.SAMSUNG_VIBRATION_CHAR,
            byteArrayOf(intensity.toByte())
        )
    }

    /**
     * @param brightness 1-10
     */
    suspend fun setScreenBrightness(brightness: Int) {
        require(brightness in 1..10) { "Brightness must be 1-10" }
        conn.writeCharacteristic(
            GattUuids.SAMSUNG_SETTINGS_SERVICE,
            GattUuids.SAMSUNG_SCREEN_BRIGHTNESS_CHAR,
            byteArrayOf(brightness.toByte())
        )
    }

    suspend fun setWatchFace(index: Int) {
        require(index in 0..255) { "Watch face index must be 0-255" }
        conn.writeCharacteristic(
            GattUuids.SAMSUNG_SETTINGS_SERVICE,
            GattUuids.SAMSUNG_WATCH_FACE_CHAR,
            byteArrayOf(index.toByte())
        )
    }
}
