package com.sqooid.fheart.bluetooth

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build

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

class GattListener<T : DataParser<T>>(
    val gatt: BluetoothGatt,
    val characteristic: BluetoothGattCharacteristic
) {
    init {
        if (characteristic.hasNotify()) {

        } else if (characteristic.hasRead()) {
        }
    }
}