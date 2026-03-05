package com.galaxyfit3.ctl.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, DISCOVERING_SERVICES, READY }

/**
 * Manages the BLE connection lifecycle for a single Galaxy Fit3 device.
 * Provides coroutine-friendly read/write/notify helpers that serialise
 * GATT operations (Android BLE only allows one outstanding operation at a time).
 */
@SuppressLint("MissingPermission")
class ConnectionManager(private val context: Context) {

    companion object {
        private const val TAG = "Fit3Conn"
    }

    private var gatt: BluetoothGatt? = null

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    // Operation queue — Android BLE requires serial GATT ops
    private val opQueue = ConcurrentLinkedQueue<BleOperation>()
    private var pendingOp: BleOperation? = null

    // Continuations waiting for GATT callbacks
    private var readCont: CompletableDeferred<ByteArray>? = null
    private var writeCont: CompletableDeferred<Unit>? = null
    private var descriptorWriteCont: CompletableDeferred<Unit>? = null

    // Notification listeners keyed by characteristic UUID
    private val notifyListeners = mutableMapOf<UUID, (ByteArray) -> Unit>()

    // ---------------------------------------------------------------
    // Connection
    // ---------------------------------------------------------------

    fun connect(address: String) {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        val device = adapter.getRemoteDevice(address)
        _state.value = ConnectionState.CONNECTING
        gatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    fun disconnect() {
        gatt?.disconnect()
    }

    // ---------------------------------------------------------------
    // GATT operations (coroutine-safe)
    // ---------------------------------------------------------------

    suspend fun readCharacteristic(serviceUuid: UUID, charUuid: UUID): ByteArray {
        val char = findCharacteristic(serviceUuid, charUuid) ?: error("Characteristic $charUuid not found")
        return enqueueRead(char)
    }

    suspend fun writeCharacteristic(serviceUuid: UUID, charUuid: UUID, value: ByteArray) {
        val char = findCharacteristic(serviceUuid, charUuid) ?: error("Characteristic $charUuid not found")
        enqueueWrite(char, value)
    }

    fun enableNotifications(serviceUuid: UUID, charUuid: UUID, listener: (ByteArray) -> Unit) {
        val char = findCharacteristic(serviceUuid, charUuid) ?: return
        gatt?.setCharacteristicNotification(char, true)
        val desc = char.getDescriptor(GattUuids.CCC_DESCRIPTOR)
        if (desc != null) {
            desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt?.writeDescriptor(desc)
        }
        notifyListeners[charUuid] = listener
    }

    fun disableNotifications(serviceUuid: UUID, charUuid: UUID) {
        val char = findCharacteristic(serviceUuid, charUuid) ?: return
        gatt?.setCharacteristicNotification(char, false)
        notifyListeners.remove(charUuid)
    }

    fun getDiscoveredServices(): List<BluetoothGattService> {
        return gatt?.services.orEmpty()
    }

    // ---------------------------------------------------------------
    // Internals
    // ---------------------------------------------------------------

    private fun findCharacteristic(serviceUuid: UUID, charUuid: UUID): BluetoothGattCharacteristic? {
        return gatt?.getService(serviceUuid)?.getCharacteristic(charUuid)
    }

    private suspend fun enqueueRead(char: BluetoothGattCharacteristic): ByteArray {
        val deferred = CompletableDeferred<ByteArray>()
        opQueue.add(BleOperation.Read(char, deferred))
        drainQueue()
        return deferred.await()
    }

    private suspend fun enqueueWrite(char: BluetoothGattCharacteristic, value: ByteArray) {
        val deferred = CompletableDeferred<Unit>()
        opQueue.add(BleOperation.Write(char, value, deferred))
        drainQueue()
        deferred.await()
    }

    @Synchronized
    private fun drainQueue() {
        if (pendingOp != null) return
        val op = opQueue.poll() ?: return
        pendingOp = op
        when (op) {
            is BleOperation.Read -> {
                readCont = op.deferred
                gatt?.readCharacteristic(op.char)
            }
            is BleOperation.Write -> {
                writeCont = op.deferred
                op.char.value = op.value
                gatt?.writeCharacteristic(op.char)
            }
        }
    }

    private fun operationComplete() {
        pendingOp = null
        drainQueue()
    }

    // ---------------------------------------------------------------
    // GATT Callback
    // ---------------------------------------------------------------

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server")
                    _state.value = ConnectionState.DISCOVERING_SERVICES
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server")
                    _state.value = ConnectionState.DISCONNECTED
                    gatt?.close()
                    gatt = null
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered: ${g.services.size} services")
                _state.value = ConnectionState.READY
            } else {
                Log.w(TAG, "Service discovery failed: $status")
                _state.value = ConnectionState.CONNECTED
            }
        }

        override fun onCharacteristicRead(
            g: BluetoothGatt,
            char: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readCont?.complete(char.value ?: byteArrayOf())
            } else {
                readCont?.completeExceptionally(RuntimeException("Read failed: $status"))
            }
            readCont = null
            operationComplete()
        }

        override fun onCharacteristicWrite(
            g: BluetoothGatt,
            char: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                writeCont?.complete(Unit)
            } else {
                writeCont?.completeExceptionally(RuntimeException("Write failed: $status"))
            }
            writeCont = null
            operationComplete()
        }

        override fun onCharacteristicChanged(
            g: BluetoothGatt,
            char: BluetoothGattCharacteristic
        ) {
            val data = char.value ?: return
            notifyListeners[char.uuid]?.invoke(data)
        }

        override fun onDescriptorWrite(
            g: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            descriptorWriteCont?.let {
                if (status == BluetoothGatt.GATT_SUCCESS) it.complete(Unit)
                else it.completeExceptionally(RuntimeException("Descriptor write failed: $status"))
                descriptorWriteCont = null
            }
        }
    }

    // ---------------------------------------------------------------
    // Operation sealed class
    // ---------------------------------------------------------------

    private sealed class BleOperation {
        data class Read(
            val char: BluetoothGattCharacteristic,
            val deferred: CompletableDeferred<ByteArray>
        ) : BleOperation()

        data class Write(
            val char: BluetoothGattCharacteristic,
            val value: ByteArray,
            val deferred: CompletableDeferred<Unit>
        ) : BleOperation()
    }
}
