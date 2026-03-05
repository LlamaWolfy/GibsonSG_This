package com.galaxyfit3.ctl.bluetooth

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder

/**
 * Foreground-capable service that owns the [ConnectionManager] so the BLE
 * connection survives configuration changes and background usage.
 */
class BleService : Service() {

    inner class LocalBinder : Binder() {
        val service: BleService get() = this@BleService
    }

    private val binder = LocalBinder()
    lateinit var connectionManager: ConnectionManager
        private set

    override fun onCreate() {
        super.onCreate()
        connectionManager = ConnectionManager(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        connectionManager.disconnect()
        super.onDestroy()
    }
}
