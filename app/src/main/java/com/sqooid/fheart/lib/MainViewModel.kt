package com.sqooid.fheart.lib

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.util.Log
import android.util.Rational
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.core.graphics.toRect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqooid.fheart.bluetooth.BluetoothDisabledException
import com.sqooid.fheart.bluetooth.GattCharacteristics
import com.sqooid.fheart.bluetooth.GattDevice
import com.sqooid.fheart.bluetooth.GattListener
import com.sqooid.fheart.bluetooth.GattScanner
import com.sqooid.fheart.bluetooth.GattServices
import com.sqooid.fheart.bluetooth.MissingBluetoothPermissions
import com.sqooid.fheart.bluetooth.parser.BatteryLevel
import com.sqooid.fheart.bluetooth.parser.HeartRateMeasurement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class LastDevice(val address: String, val name: String)
class MainViewModel() : ViewModel() {
    // internal
    private val scanner = GattScanner()
    var displayLayout: LayoutCoordinates? = null
    private val pipBuilder: PictureInPictureParams.Builder = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(4, 3))

    // ui state
    var lastDeviceDummy: LastDevice? by mutableStateOf(null)
    var lastDevice: GattDevice? by mutableStateOf(null)
    var foundDevices = mutableStateMapOf<String, GattDevice>()
    var loadingDevice: Boolean by mutableStateOf(false)
    var scanning: Boolean by mutableStateOf(false)
    private var hrListener: GattListener<HeartRateMeasurement>? by mutableStateOf(null)
    private var batteryListener: GattListener<BatteryLevel>? by mutableStateOf(null)
    var hrValue: Int by mutableIntStateOf(0)
    var batteryValue: Int by mutableIntStateOf(0)

    fun init(context: Activity) {
        val prefs = context.getPreferences(Context.MODE_PRIVATE)
        val (lastAdd, lastName) = prefs.getString("lastUsed", ",")
            ?.split(",", ignoreCase = true, limit = 2) ?: listOf("", "")
        if (lastAdd.isNotBlank()) {
            lastDeviceDummy = LastDevice(lastAdd, lastName)
            lastDevice = scanner.getDeviceByAddress(context, lastAdd)
            lastDevice?.let {
                selectDevice(context, it)
            }
        }
    }

    fun toggleScan(context: Activity) {
        viewModelScope.launch(Dispatchers.Main) {
            if (!scanning) {
                startScan(context)
            } else {
                stopScan()
            }
        }
    }

    fun startPip(context: Activity) {
        displayLayout?.let {
            Log.d("app", displayLayout.toString())
            val pipParams = pipBuilder
                .setSourceRectHint(
                    it.boundsInWindow().toAndroidRectF().toRect()
                )
                .build()
            context.enterPictureInPictureMode(pipParams)
        }
    }

    private fun startScan(context: Activity) {
        try {
            scanner.startScan(
                context,
                filterServices = arrayOf(GattServices.HEART_RATE)
            ) {
                if (!foundDevices.contains(it.address)) {
                    foundDevices[it.address] = it
                }
            }
            scanning = true
        } catch (e: MissingBluetoothPermissions) {
            scanning = false
            Log.e("app", e.message.toString())
        } catch (e: BluetoothDisabledException) {
            scanning = false
            Log.e("app", e.message.toString())
        } catch (e: Exception) {
            scanning = false
            Log.e("app", e.message.toString())
        }
    }

    private fun stopScan() {
        scanner.stopScan()
        scanning = false
    }

    fun selectDevice(context: Activity, device: GattDevice) {
        loadingDevice = true

        // save for recall
        val address = device.address
        val name = device.name
        val lastString = "${address},${name}"
        val prefs = context.getPreferences(Context.MODE_PRIVATE)
        prefs.edit().putString("lastUsed", lastString).apply()

        // display selection
        lastDeviceDummy = LastDevice(address, name)
        lastDevice = device

        // connect for listening
        Log.d("app", "selected device ${name}")
        batteryListener = device.createListener(
            context,
            GattServices.BATTERY,
            GattCharacteristics.BATTERY_LEVEL,
            BatteryLevel(0)
        ) {
            batteryValue = it.percentage
            Log.v("app", "got battery $batteryValue")
        }
        hrListener = device.createListener(
            context,
            GattServices.HEART_RATE,
            GattCharacteristics.HEART_RATE_MEASUREMENT,
            HeartRateMeasurement(0)
        ) {
            loadingDevice = false
            hrValue = it.measurement
            Log.v("app", "got hr $hrValue")
        }
    }
}
