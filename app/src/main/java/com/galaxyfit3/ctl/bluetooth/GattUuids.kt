package com.galaxyfit3.ctl.bluetooth

import java.util.UUID

/**
 * BLE GATT service and characteristic UUIDs for Galaxy Fit3.
 *
 * Standard BLE SIG UUIDs are used where applicable. Samsung-proprietary
 * service UUIDs were obtained from BLE sniffing of the Galaxy Fit3
 * advertising data and GATT table.
 */
object GattUuids {

    // --- Standard BLE SIG services ---
    val HEART_RATE_SERVICE: UUID            = uuid16(0x180D)
    val HEART_RATE_MEASUREMENT: UUID        = uuid16(0x2A37)
    val BODY_SENSOR_LOCATION: UUID          = uuid16(0x2A38)

    val DEVICE_INFO_SERVICE: UUID           = uuid16(0x180A)
    val MANUFACTURER_NAME: UUID             = uuid16(0x2A29)
    val MODEL_NUMBER: UUID                  = uuid16(0x2A24)
    val FIRMWARE_REVISION: UUID             = uuid16(0x2A26)
    val HARDWARE_REVISION: UUID             = uuid16(0x2A27)
    val SOFTWARE_REVISION: UUID             = uuid16(0x2A28)

    val BATTERY_SERVICE: UUID               = uuid16(0x180F)
    val BATTERY_LEVEL: UUID                 = uuid16(0x2A19)

    val CURRENT_TIME_SERVICE: UUID          = uuid16(0x1805)
    val CURRENT_TIME: UUID                  = uuid16(0x2A2B)

    // Standard descriptor for enabling notifications / indications
    val CCC_DESCRIPTOR: UUID                = uuid16(0x2902)

    // --- Samsung proprietary services (Galaxy Fit3) ---
    val SAMSUNG_HEALTH_SERVICE: UUID =
        UUID.fromString("34800001-7185-4d5d-b431-630e7050e8f0")
    val SAMSUNG_STEPS_CHAR: UUID =
        UUID.fromString("34800002-7185-4d5d-b431-630e7050e8f0")
    val SAMSUNG_SLEEP_CHAR: UUID =
        UUID.fromString("34800003-7185-4d5d-b431-630e7050e8f0")
    val SAMSUNG_SPO2_CHAR: UUID =
        UUID.fromString("34800004-7185-4d5d-b431-630e7050e8f0")
    val SAMSUNG_STRESS_CHAR: UUID =
        UUID.fromString("34800005-7185-4d5d-b431-630e7050e8f0")

    val SAMSUNG_NOTIFICATION_SERVICE: UUID =
        UUID.fromString("34801001-7185-4d5d-b431-630e7050e8f0")
    val SAMSUNG_NOTIFY_WRITE_CHAR: UUID =
        UUID.fromString("34801002-7185-4d5d-b431-630e7050e8f0")
    val SAMSUNG_ALARM_CHAR: UUID =
        UUID.fromString("34801003-7185-4d5d-b431-630e7050e8f0")

    val SAMSUNG_SETTINGS_SERVICE: UUID =
        UUID.fromString("34802001-7185-4d5d-b431-630e7050e8f0")
    val SAMSUNG_DND_CHAR: UUID =
        UUID.fromString("34802002-7185-4d5d-b431-630e7050e8f0")
    val SAMSUNG_VIBRATION_CHAR: UUID =
        UUID.fromString("34802003-7185-4d5d-b431-630e7050e8f0")
    val SAMSUNG_SCREEN_BRIGHTNESS_CHAR: UUID =
        UUID.fromString("34802004-7185-4d5d-b431-630e7050e8f0")
    val SAMSUNG_WATCH_FACE_CHAR: UUID =
        UUID.fromString("34802005-7185-4d5d-b431-630e7050e8f0")

    private fun uuid16(shortId: Int): UUID {
        return UUID.fromString(
            String.format("%08x-0000-1000-8000-00805f9b34fb", shortId)
        )
    }
}
