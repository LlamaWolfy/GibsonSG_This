package com.galaxyfit3.ctl.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class ScannedDevice(
    val name: String?,
    val address: String,
    val rssi: Int
)

/**
 * Scans for nearby BLE devices, filtering for Galaxy Fit3.
 */
class BleScanner(context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    companion object {
        private const val SCAN_TIMEOUT_MS = 15_000L
        private val FIT3_NAMES = setOf(
            "Galaxy Fit3",
            "Galaxy Fit 3",
            "SM-R390",
        )
    }

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    /**
     * Returns a cold [Flow] of [ScannedDevice]s discovered during a BLE scan.
     * The scan stops automatically after [SCAN_TIMEOUT_MS] or when the flow
     * collector is cancelled.
     */
    @SuppressLint("MissingPermission")
    fun scan(): Flow<ScannedDevice> = callbackFlow {
        val scanner = bluetoothAdapter?.bluetoothLeScanner
            ?: throw IllegalStateException("Bluetooth LE scanner not available")

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // We don't use a service-UUID filter so the user can see the raw
        // device name; filtering happens in the callback.
        val filters = emptyList<ScanFilter>()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device
                val name = device.name ?: result.scanRecord?.deviceName
                if (name != null && FIT3_NAMES.any { name.contains(it, ignoreCase = true) }) {
                    trySend(ScannedDevice(name, device.address, result.rssi))
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(RuntimeException("BLE scan failed with error code $errorCode"))
            }
        }

        scanner.startScan(filters, settings, callback)

        // Auto-stop after timeout
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({ scanner.stopScan(callback); close() }, SCAN_TIMEOUT_MS)

        awaitClose {
            scanner.stopScan(callback)
            handler.removeCallbacksAndMessages(null)
        }
    }
}
