package com.sqooid.fheart

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqooid.fheart.bluetooth.BluetoothDisabledException
import com.sqooid.fheart.bluetooth.GattDevice
import com.sqooid.fheart.bluetooth.GattScanner
import com.sqooid.fheart.bluetooth.MissingBluetoothPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class Device(val name: String, val address: String)

@Composable
fun DeviceItem(device: GattDevice, onClick: (_: GattDevice) -> Unit) {
    var name by remember {
        mutableStateOf("")
    }
    val coroutineScope = rememberCoroutineScope()
    fun loadName() {
        coroutineScope.launch {
            name = device.name
        }
    }
    loadName()

    TextButton(
        onClick = { onClick(device) },
        modifier = Modifier
            .clip(RoundedCornerShape(1.dp))
            .fillMaxWidth(),
        shape = RectangleShape
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_monitor_heart_24),
                contentDescription = "Heart rate monitor icon",
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = name.ifBlank { "Unnamed" }
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun DeviceSection(content: @Composable() () -> Unit) {
    Surface(
        tonalElevation = 16.dp,
        shape = RoundedCornerShape(2.dp),
        shadowElevation = 8.dp,
        modifier = Modifier.animateContentSize()
    ) {
        content()
    }
}

@SuppressLint("MissingPermission")
@Composable
fun DeviceSelector(context: Activity, scanner: GattScanner, onSelect: (_: GattDevice) -> Unit) {
    var scanning by remember {
        mutableStateOf(false)
    }

    val foundDevices = remember {
        mutableStateMapOf<String, GattDevice>(
        )
    }
    val coroutineScope = rememberCoroutineScope()

    val prefs = context.getPreferences(Context.MODE_PRIVATE)
    val lastUsed = prefs.getString("lastUsed", ",")
        ?.split(",", ignoreCase = true, limit = 2)
    val lastUsedId = lastUsed?.get(0)
    val lastUsedName = lastUsed?.get(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
//            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Heart rate monitor", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(modifier = Modifier.animateContentSize(),
                onClick = {
                    coroutineScope.launch(Dispatchers.IO) {
                        Log.d("app", Thread.currentThread().getName())
                        if (!scanning) {
                            try {
                                scanner.startScan(context) {
                                    Log.d("app", Thread.currentThread().getName())
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
                            }

                        } else {
                            scanner.stopScan()
                            scanning = false
                        }
                    }

                }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (scanning) "Scanning" else "Start scan")
                    if (scanning) {
                        Spacer(modifier = Modifier.width(4.dp))
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Last used device
//        if (!lastUsedId.isNullOrEmpty() && !lastUsedName.isNullOrEmpty()) {
//            Text(text = "Last used device")
//            Spacer(modifier = Modifier.height(8.dp))
//            DeviceSection {
//                DeviceItem(device = Device(lastUsedName, lastUsedId), onClick = onSelect)
//            }
//            Spacer(modifier = Modifier.height(8.dp))
//        }

        // Other devices
        Text(text = "Available devices")
        Spacer(modifier = Modifier.height(8.dp))
        DeviceSection {
            LazyColumn {
                foundDevices.forEach {
                    item {
                        DeviceItem(device = it.value, onClick = onSelect)
                    }
                }
            }
        }
    }

}