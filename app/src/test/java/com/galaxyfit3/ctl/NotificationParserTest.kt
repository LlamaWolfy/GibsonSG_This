package com.galaxyfit3.ctl

import com.galaxyfit3.ctl.bluetooth.NotificationService
import com.galaxyfit3.ctl.data.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationParserTest {

    private val service = NotificationService::class.java
        .getDeclaredConstructor(com.galaxyfit3.ctl.bluetooth.ConnectionManager::class.java)
        .apply { isAccessible = true }
        .newInstance(null as com.galaxyfit3.ctl.bluetooth.ConnectionManager?) as NotificationService

    @Test
    fun `parseAlarms decodes multiple alarm blocks`() {
        // Two alarms: id=1 07:30 ON daily, id=2 22:00 OFF once
        val data = byteArrayOf(
            1, 7, 30, 1, 0x7F.toByte(),  // alarm 1
            2, 22, 0, 0, 0               // alarm 2
        )
        val method = NotificationService::class.java
            .getDeclaredMethod("parseAlarms", ByteArray::class.java)
        method.isAccessible = true
        val result = method.invoke(service, data) as List<*>

        assertEquals(2, result.size)
        val a1 = result[0] as Alarm
        assertEquals(1, a1.id)
        assertEquals(7, a1.hour)
        assertEquals(30, a1.minute)
        assertEquals(true, a1.enabled)
        assertEquals(0x7F, a1.repeatDays)

        val a2 = result[1] as Alarm
        assertEquals(2, a2.id)
        assertEquals(22, a2.hour)
        assertEquals(0, a2.minute)
        assertEquals(false, a2.enabled)
    }

    @Test
    fun `parseAlarms handles empty data`() {
        val method = NotificationService::class.java
            .getDeclaredMethod("parseAlarms", ByteArray::class.java)
        method.isAccessible = true
        val result = method.invoke(service, byteArrayOf()) as List<*>
        assertEquals(0, result.size)
    }
}
