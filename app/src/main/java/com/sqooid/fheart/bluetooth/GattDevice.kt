package com.sqooid.fheart.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import com.sqooid.fheart.permissionCheckAndRequest
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
    properties and property != 0

fun BluetoothGattCharacteristic.hasNotify(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

fun BluetoothGattCharacteristic.hasRead(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

class GattDevice(context: Activity, val device: BluetoothDevice) {
    init {
        if (!permissionCheckAndRequest(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            throw MissingBluetoothPermissions("Bluetooth connect permission not enabled")
        }
    }

    var gattCallback: BluetoothGattCallback? = null
    var gatt: BluetoothGatt? = null

    val name: String
        @SuppressLint("MissingPermission")
        get() = device.name

    @SuppressLint("MissingPermission")
    suspend fun <T : DataParser<T>> createListener(
        context: Activity,
        serviceId: String,
        characteristicId: String,
        dataCallback: (data: T) -> Unit
    ): GattListener<T>? {
        return suspendCoroutine { continuation ->
            gattCallback = object : BluetoothGattCallback() {
                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    val characteristic = gatt?.getService(UUID.fromString(serviceId))
                        ?.getCharacteristic(UUID.fromString(characteristicId))
                    if (characteristic == null) {
                        continuation.resume(null)
                        return
                    }

                    if (!characteristic.hasRead() && !characteristic.hasNotify()) {
                        continuation.resume(null)
                    }

                    continuation.resume(GattListener(gatt, characteristic))
                }
            }

            gatt = device.connectGatt(context, false, gattCallback)
        }
    }
}