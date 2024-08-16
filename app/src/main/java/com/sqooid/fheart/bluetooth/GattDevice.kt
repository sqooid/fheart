package com.sqooid.fheart.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.sqooid.fheart.lib.permissionCheckAndRequest
import java.util.UUID
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
        connectionCallback: (connected: Boolean) -> Unit = {},
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
                connectionCallback,
                dataCallback
            )
        } catch (e: InvalidBluetoothDevice) {
            Log.e("app", "failed to create listener $e")
            null
        }

    }
}