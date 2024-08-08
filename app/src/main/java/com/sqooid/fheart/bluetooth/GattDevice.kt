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
import kotlin.time.Duration


class GattDevice(context: Activity, val device: BluetoothDevice) {
    init {
        if (!permissionCheckAndRequest(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            throw MissingBluetoothPermissions("Bluetooth connect permission not enabled")
        }
    }

    val name: String
        @SuppressLint("MissingPermission")
        get() = device.name

    @SuppressLint("MissingPermission")
    suspend fun <T : DataParser<T>> createListener(
        context: Activity,
        serviceId: String,
        characteristicId: String,
        dataTypeTemplate: T,
        readInterval: Duration? = null,
        dataCallback: (data: T) -> Unit
    ): GattListener<T>? {
        return suspendCoroutine { continuation ->
            val gattCallback = object : BluetoothGattCallback() {
                lateinit var characteristic: BluetoothGattCharacteristic
                override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                    super.onServicesDiscovered(gatt, status)
                    val newCharacteristic = gatt?.getService(UUID.fromString(serviceId))
                        ?.getCharacteristic(UUID.fromString(characteristicId))
                    if (newCharacteristic == null) {
                        continuation.resume(null)
                        return
                    }
                    characteristic = newCharacteristic

                    if (!characteristic.hasRead() && !characteristic.hasNotify()) {
                        continuation.resume(null)
                    }

                    continuation.resume(
                        GattListener(
                            context,
                            gatt,
                            characteristic,
                            dataTypeTemplate,
                            readInterval,
                            dataCallback
                        )
                    )
                }


            }

            device.connectGatt(context, false, gattCallback)
        }
    }
}