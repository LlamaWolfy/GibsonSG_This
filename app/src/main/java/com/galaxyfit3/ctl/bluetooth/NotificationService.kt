package com.galaxyfit3.ctl.bluetooth

import com.galaxyfit3.ctl.data.Alarm
import com.galaxyfit3.ctl.data.Notification
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Sends notifications and manages alarms on the Galaxy Fit3.
 */
class NotificationService(private val conn: ConnectionManager) {

    /**
     * Push a notification to the Galaxy Fit3 display.
     * The payload is packed as: [packageLen(1)][package][titleLen(1)][title][body...]
     */
    suspend fun sendNotification(notification: Notification) {
        val pkg = notification.appPackage.toByteArray(Charsets.UTF_8)
        val title = notification.title.toByteArray(Charsets.UTF_8)
        val body = notification.body.toByteArray(Charsets.UTF_8)

        val payload = ByteBuffer.allocate(2 + pkg.size + title.size + body.size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .put(pkg.size.toByte())
            .put(pkg)
            .put(title.size.toByte())
            .put(title)
            .put(body)
            .array()

        conn.writeCharacteristic(
            GattUuids.SAMSUNG_NOTIFICATION_SERVICE,
            GattUuids.SAMSUNG_NOTIFY_WRITE_CHAR,
            payload
        )
    }

    /**
     * Write an alarm to the device.
     * Payload: [id(1)][hour(1)][minute(1)][enabled(1)][repeatDays(1)]
     */
    suspend fun setAlarm(alarm: Alarm) {
        val payload = byteArrayOf(
            alarm.id.toByte(),
            alarm.hour.toByte(),
            alarm.minute.toByte(),
            if (alarm.enabled) 1 else 0,
            alarm.repeatDays.toByte()
        )
        conn.writeCharacteristic(
            GattUuids.SAMSUNG_NOTIFICATION_SERVICE,
            GattUuids.SAMSUNG_ALARM_CHAR,
            payload
        )
    }

    /**
     * Read all alarms from the device.
     * Response: repeated blocks of [id(1)][hour(1)][minute(1)][enabled(1)][repeatDays(1)]
     */
    suspend fun getAlarms(): List<Alarm> {
        val data = conn.readCharacteristic(
            GattUuids.SAMSUNG_NOTIFICATION_SERVICE,
            GattUuids.SAMSUNG_ALARM_CHAR
        )
        return parseAlarms(data)
    }

    /**
     * Delete an alarm by id. Sends the alarm with enabled=false and zeroed time.
     */
    suspend fun deleteAlarm(alarmId: Int) {
        setAlarm(Alarm(alarmId, 0, 0, enabled = false, repeatDays = 0))
    }

    internal fun parseAlarms(data: ByteArray): List<Alarm> {
        val alarms = mutableListOf<Alarm>()
        val chunkSize = 5
        var offset = 0
        while (offset + chunkSize <= data.size) {
            alarms.add(
                Alarm(
                    id = data[offset].toInt() and 0xFF,
                    hour = data[offset + 1].toInt() and 0xFF,
                    minute = data[offset + 2].toInt() and 0xFF,
                    enabled = data[offset + 3].toInt() != 0,
                    repeatDays = data[offset + 4].toInt() and 0xFF
                )
            )
            offset += chunkSize
        }
        return alarms
    }
}
