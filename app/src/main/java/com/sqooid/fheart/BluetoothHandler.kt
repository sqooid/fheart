package com.sqooid.fheart

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import com.sqooid.fheart.ui.getLongId
import java.util.UUID

class BluetoothHandler() {
    private var init = false
    private var scanning = false

    private var scanner: BluetoothLeScanner? = null
    private var adapter: BluetoothAdapter? = null
    private var scanCallback: ScanCallback? = null
    private var gattCallback: BluetoothGattCallback? = null

    private val hrServiceId = "180d"
    private val hrCharId = "2a37"

    private fun init(context: Activity) {
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            adapter = manager.adapter
            scanner = manager.adapter.bluetoothLeScanner
            init = true
        }
    }

    private val services = mapOf("heart rate" to getLongId(hrServiceId))

    enum class ScanStatus {
        STARTED,
        BLUETOOTH_DISABLED,
        MISSING_PERMISSIONS
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        Log.d("app", "stopped scan")
        scanning = false
        scanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun scan(
        context: Activity,
        callback: (callbackType: Int, result: ScanResult) -> Unit
    ): ScanStatus {
        init(context)

        // if bluetooth no turned on
        if (!init || scanner == null) return ScanStatus.BLUETOOTH_DISABLED

        // if bluetooth scanning not permitted
        val hasPerms = permissionCheckAndRequest(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) && permissionCheckAndRequest(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) && permissionCheckAndRequest(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (!hasPerms) return ScanStatus.MISSING_PERMISSIONS

        // toggle scan
        if (!scanning) {
            scanCallback = object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    super.onScanResult(callbackType, result)
                    Log.d("app", result.toString())
                    callback(callbackType, result)

                    gattCallback = object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(
                            gatt: BluetoothGatt?,
                            status: Int,
                            newState: Int
                        ) {
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                Log.d("app", "connected to gatt server")
                                gatt?.discoverServices()
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                Log.d("app", "disconnected from gatt server")
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                            super.onServicesDiscovered(gatt, status)
                            gatt?.services?.forEach {
                                Log.d("app", "service: ${it.uuid}")
                                val chars = it.characteristics
                                chars.forEach {
                                    Log.d("app", it.uuid.toString())
                                    Log.d("app", it.properties.toString())
                                    gatt.setCharacteristicNotification(it,true)
                                    gatt.readCharacteristic(it)
                                }
                            }
                            Log.d("app", gatt?.services?.map { it.uuid }.toString())
                        }

                        override fun onCharacteristicRead(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            value: ByteArray,
                            status: Int
                        ) {
                            super.onCharacteristicRead(gatt, characteristic, value, status)
                            val str = value.decodeToString()
                            Log.d("app","${characteristic.uuid} $str")
                        }

                        override fun onCharacteristicChanged(
                            gatt: BluetoothGatt,
                            characteristic: BluetoothGattCharacteristic,
                            value: ByteArray
                        ) {
                            super.onCharacteristicChanged(gatt, characteristic, value)
                            val str = value.decodeToString()
                            Log.d("app","${characteristic.uuid} $str $value")
                        }
                    }
                    val gatt = result.device.connectGatt(context, false, gattCallback)


                }
            }
            Log.d("app", "started scan")
            val filter = services.values.map {
                ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(UUID.fromString(it)))
                    .build()
            }
            scanning = true
            scanner?.startScan(filter, ScanSettings.Builder().build(), scanCallback)
        }
        return ScanStatus.STARTED
    }


    @SuppressLint("MissingPermission")
    fun connect(
        context: Activity,
        address: String,
        callback: (gatt: BluetoothGatt?, status: Int, newState: Int) -> Unit
    ): Boolean {
        adapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                gattCallback = object : BluetoothGattCallback() {
                    override fun onConnectionStateChange(
                        gatt: BluetoothGatt?,
                        status: Int,
                        newState: Int
                    ) {
                        callback(gatt, status, newState)
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            Log.d("app", "connected to gatt server")
//                            gatt.setCharacteristicNotification(Blu)
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            Log.d("app", "disconnected from gatt server")
                        }
                    }


                }
                val hasPerms =
                    permissionCheckAndRequest(context, Manifest.permission.BLUETOOTH_CONNECT)
                if (!hasPerms) return false
                val gatt = device.connectGatt(context, false, gattCallback)
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w("app", "device with address $address not found")
                return false
            }
        } ?: run {
            Log.w("app", "BluetoothAdapter not initialized")
            return false
        }
    }
}