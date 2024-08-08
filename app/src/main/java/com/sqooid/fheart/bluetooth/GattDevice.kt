package com.sqooid.fheart.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.util.Log
import com.sqooid.fheart.permissionCheckAndRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration


class GattDevice(context: Activity, private val device: BluetoothDevice) {
    init {
        if (!permissionCheckAndRequest(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            throw MissingBluetoothPermissions("Bluetooth connect permission not enabled")
        }
    }

    val name: String
        @SuppressLint("MissingPermission")
        get() = device.name ?: ""

    val address: String
        get() = device.address

    @SuppressLint("MissingPermission")
    fun <T : DataParser<T>> createListener(
        context: Activity,
        serviceId: UUID,
        characteristicId: UUID,
        dataTypeTemplate: T,
        readInterval: Duration? = null,
        dataCallback: (data: T) -> Unit
    ): GattListener<T>? {
        return try {
            GattListener(
                context,
                device,
                serviceId,
                characteristicId,
                dataTypeTemplate,
                readInterval,
                dataCallback
            )
        } catch (e: InvalidBluetoothDevice) {
            null
        }

    }
}