package com.sqooid.fheart.bluetooth

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.Timer
import java.util.UUID
import kotlin.concurrent.timer
import kotlin.coroutines.suspendCoroutine
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object GattServices {
    val BATTERY = getLongUuid("180f")
    val DEVICE_INFORMATION = getLongUuid("180a")
    val GAP = getLongUuid("1800")
    val GATT = getLongUuid("1801")
    val HEART_RATE = getLongUuid("180d")
}

object GattCharacteristics {
    val HEART_RATE_MEASUREMENT = getLongUuid("2a37")
    val BATTERY_LEVEL = getLongUuid("2a37")
    val MANUFACTURER_NAME_STRING = getLongUuid("2a29")
    val MODEL_NUMBER_STRING = getLongUuid("2a24")
    val SERIAL_NUMBER_STRING = getLongUuid("2a25")
    val HARDWARE_REVISION_STRING = getLongUuid("2a27")
    val FIRMWARE_REVISION_STRING = getLongUuid("2a26")
    val SOFTWARE_REVISION_STRING = getLongUuid("2a28")
}

object GattDescriptors {
    val CLIENT_CHARACTERISTIC_CONFIGURATION = getLongUuid("2902")
}

fun getLongUuid(id: String): UUID {
    return UUID.fromString("0000${id}-0000-1000-8000-00805f9b34fb")
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
    private var device: BluetoothDevice,
    private val serviceId: UUID,
    private val characteristicId: UUID,
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
            if (characteristicId == rCharacteristic.uuid) {
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
            if (characteristicId == rCharacteristic?.uuid) {
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
            if (characteristicId == rCharacteristic.uuid) {
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
            if (characteristicId == rCharacteristic?.uuid) {
                val data = dataTypeTemplate.parseFromBytes(rCharacteristic.value)
                dataCallback(data)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)
            Log.d("app", "wrote descriptor $status")
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                discoverTimer?.cancel()
                init()
            }
        }
    }

    private var readTimer: Timer? = null
    private var gatt: BluetoothGatt
    private var discoverTimer: Timer? = null
    private var retries = 0

    init {
        gatt = device.connectGatt(context, true, gattCallback)
        runBlocking { delay(600) }
        discoverTimer =
            timer(period = 4.toDuration(DurationUnit.SECONDS).inWholeMilliseconds) {
                val res = gatt.discoverServices()
                Log.d("app", "discover result: $res (attempt ${retries + 1})")
                retries += 1
                if (retries > 10) {
                    discoverTimer?.cancel()
                    Log.d("app", "service discovery took too long")
                }
            }
    }

    fun init() {
        Log.d("app", "addr: ${gatt.device.address}")
        Log.d("app", gatt.services?.map { it.uuid }.toString())
        val service = gatt.getService(serviceId)
        Log.d("app", service?.characteristics?.map { it.uuid }.toString())
        val characteristic = gatt.getService(serviceId)?.getCharacteristic(characteristicId)
            ?: throw InvalidBluetoothDevice("device does not have service $serviceId and characteristic $characteristicId")

        // use notify if available
        if (characteristic.hasNotify()) {
            Log.d("app", "has notify")
            if (!gatt.setCharacteristicNotification(characteristic, true)) {
                Log.d("app", "setCharacteristicNotification failed for ${characteristic.uuid}")
            }
            val dess = characteristic.descriptors
            characteristic.getDescriptor(GattDescriptors.CLIENT_CHARACTERISTIC_CONFIGURATION)
                ?.let {
                    writeDescriptor(gatt, it, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                }
        }

        // otherwise read on interval
        else if (characteristic.hasRead()) {
            Log.d("app", "has read")
            readTimer = timer(
                period = (readInterval ?: 1.toDuration(DurationUnit.SECONDS)).inWholeMilliseconds
            ) {
                gatt.readCharacteristic(characteristic)
            }
        }
    }

    fun close() {
        gatt.getService(serviceId)?.getCharacteristic(characteristicId)
            ?.getDescriptor(GattDescriptors.CLIENT_CHARACTERISTIC_CONFIGURATION)
            ?.let {
                writeDescriptor(gatt, it, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)
            }
        gatt.close()
        readTimer?.cancel()
    }
}