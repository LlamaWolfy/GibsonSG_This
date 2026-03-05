package com.galaxyfit3.ctl.data

data class HeartRateReading(
    val bpm: Int,
    val sensorContact: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class StepsReading(
    val steps: Int,
    val distanceMeters: Int,
    val caloriesBurned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class SleepReading(
    val totalMinutes: Int,
    val deepMinutes: Int,
    val lightMinutes: Int,
    val remMinutes: Int,
    val awakeMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class SpO2Reading(
    val percentage: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class StressReading(
    val level: Int,    // 0-100
    val timestamp: Long = System.currentTimeMillis()
)

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val firmwareRevision: String,
    val hardwareRevision: String,
    val softwareRevision: String,
    val batteryLevel: Int
)

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean,
    val repeatDays: Int  // bitmask: bit0=Sun, bit1=Mon, ..., bit6=Sat
)

data class Notification(
    val appPackage: String,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class DeviceSettings(
    val dndEnabled: Boolean,
    val vibrationIntensity: Int,  // 0=off, 1=low, 2=medium, 3=high
    val screenBrightness: Int,    // 1-10
    val watchFaceIndex: Int
)
