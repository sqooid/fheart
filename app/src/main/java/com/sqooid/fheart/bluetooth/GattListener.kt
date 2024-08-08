package com.sqooid.fheart.bluetooth

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.os.Build
import android.util.Log
import java.util.Timer
import java.util.UUID
import kotlin.concurrent.timer
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object GattServices {
    val BATTERY = getLongId("180f")
    val DEVICE_INFORMATION = getLongId("180a")
    val GAP = getLongId("1800")
    val GATT = getLongId("1801")
    val HEART_RATE = getLongId("180d")
}

object GattCharacteristics {
    val HEART_RATE_MEASUREMENT = getLongId("2a37")
    val BATTERY_LEVEL = getLongId("2a37")
    val MANUFACTURER_NAME_STRING = getLongId("2a29")
    val MODEL_NUMBER_STRING = getLongId("2a24")
    val SERIAL_NUMBER_STRING = getLongId("2a25")
    val HARDWARE_REVISION_STRING = getLongId("2a27")
    val FIRMWARE_REVISION_STRING = getLongId("2a26")
    val SOFTWARE_REVISION_STRING = getLongId("2a28")
}

object GattDescriptors {
    val CLIENT_CHARACTERISTIC_CONFIGURATION = getLongId("2902")
}

fun getLongId(id: String): String {
    return "0000${id}-0000-1000-8000-00805f9b34gb"
}

fun BluetoothGattCharacteristic.containsProperty(property: Int): Boolean =
    properties and property != 0

fun BluetoothGattCharacteristic.hasNotify(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

fun BluetoothGattCharacteristic.hasRead(): Boolean =
    containsProperty(BluetoothGattCharacteristic.PROPERTY_READ)

@SuppressLint("MissingPermission")
fun writeDescriptor(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, payload: ByteArray) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        gatt.writeDescriptor(descriptor, payload)
    } else {
        descriptor.value = payload
        gatt.legacyDescriptorWrite(descriptor, payload)
    }
}

@SuppressLint("MissingPermission")
@TargetApi(Build.VERSION_CODES.S)
@Suppress("DEPRECATION")
private fun BluetoothGatt.legacyDescriptorWrite(
    descriptor: BluetoothGattDescriptor,
    value: ByteArray
): Boolean {
    descriptor.value = value
    return writeDescriptor(descriptor)
}

interface DataParser<T> {
    fun parseFromBytes(byteArray: ByteArray): T
}

@Suppress("DEPRECATION")
@SuppressLint("MissingPermission")
class GattListener<T : DataParser<T>>(
    context: Context,
    private var gatt: BluetoothGatt,
    private val characteristic: BluetoothGattCharacteristic,
    private val dataTypeTemplate: T,
    private val readInterval: Duration?,
    private val dataCallback: (data: T) -> Unit
) {
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            rCharacteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, rCharacteristic, value)
            if (characteristic.uuid == rCharacteristic.uuid) {
                val data = dataTypeTemplate.parseFromBytes(value)
                dataCallback(data)
            }
        }

        @Deprecated("Deprecated for Android 13+")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            rCharacteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, rCharacteristic)
            if (characteristic.uuid == rCharacteristic?.uuid && rCharacteristic != null) {
                val data = dataTypeTemplate.parseFromBytes(rCharacteristic.value)
                dataCallback(data)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            rCharacteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, rCharacteristic, value, status)
            if (characteristic.uuid == rCharacteristic.uuid) {
                val data = dataTypeTemplate.parseFromBytes(value)
                dataCallback(data)
            }
        }

        @Deprecated("Deprecated for Android 13+")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            rCharacteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, rCharacteristic, status)
            if (characteristic.uuid == rCharacteristic?.uuid && rCharacteristic != null) {
                val data = dataTypeTemplate.parseFromBytes(rCharacteristic.value)
                dataCallback(data)
            }
        }
    }

    var readTimer: Timer? = null

    init {
        val newGatt = gatt.device.connectGatt(context, false, gattCallback)
        gatt.close()
        gatt = newGatt

        // use notify if available
        if (characteristic.hasNotify()) {
            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                Log.d("app", "setCharacteristicNotification failed for ${characteristic.uuid}")
            }
            characteristic.getDescriptor(UUID.fromString(GattDescriptors.CLIENT_CHARACTERISTIC_CONFIGURATION))
                ?.let {
                    writeDescriptor(gatt, it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                }
        }

        // otherwise read on interval
        else if (characteristic.hasRead()) {
            readTimer = timer(
                period = (readInterval ?: 1.toDuration(DurationUnit.SECONDS)).inWholeMilliseconds
            ) {
                gatt.readCharacteristic(characteristic)
            }
        }
    }

    fun close() {
        gatt.close()
        readTimer?.cancel()
    }
}