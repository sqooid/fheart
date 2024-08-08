package com.sqooid.fheart.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import com.sqooid.fheart.permissionCheckAndRequest
import java.util.UUID
import kotlin.coroutines.coroutineContext

class BluetoothDisabledException(message: String) : Exception(message)
class MissingBluetoothPermissions(message: String) : Exception(message)

class GattScanner {
    private var scanner: BluetoothLeScanner? = null
    private var adapter: BluetoothAdapter? = null
    private var scanCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    fun startScan(
        context: Activity,
        filterServices: Array<String> = arrayOf(),
        filterCharacteristics: Array<String> = arrayOf(),
        scanResultCallback: (device: GattDevice) -> Unit
    ) {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = manager.adapter
        scanner = manager.adapter.bluetoothLeScanner

        // checks
        if (scanner == null) {
            throw BluetoothDisabledException("Bluetooth is disabled")
        }
        val hasPerms = permissionCheckAndRequest(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) && permissionCheckAndRequest(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) && permissionCheckAndRequest(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (!hasPerms) {
            throw MissingBluetoothPermissions("Required permissions are missing")
        }

        if (scanCallback != null) return

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {

                super.onScanResult(callbackType, result)
                Log.d("app", result.toString())
                Log.d("app", Thread.currentThread().getName())
                val device = GattDevice(context, result.device)
                scanResultCallback(device)
            }
        }
        val serviceFilters = filterServices.map {
            ScanFilter.Builder().setServiceUuid(ParcelUuid(UUID.fromString(it))).build()
        }
        scanner?.startScan(
            serviceFilters,
            ScanSettings.Builder().build(),
            scanCallback
        )
        Log.d("app", "started scan")
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (scanCallback == null) return

        scanner?.stopScan(scanCallback)
        Log.d("app", "stopped scan")
        scanCallback = null
    }

    fun getDeviceByAddress(context: Activity, address: String): GattDevice? {
        val device = adapter?.getRemoteDevice(address) ?: return null
        return GattDevice(context, device)
    }
}