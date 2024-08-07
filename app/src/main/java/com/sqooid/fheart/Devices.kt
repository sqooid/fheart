package com.sqooid.fheart

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Device(val name: String, val address: String)

@Composable
fun DeviceCard(device: Device, onClick: (_: Device) -> Unit) {
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
                text = device.name
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun DeviceCard(content: @Composable() () -> Unit) {
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
fun DeviceSelector(context: Activity, handler: BluetoothHandler, onSelect: (_: Device) -> Unit) {
    var scanning by remember {
        mutableStateOf(false)
    }

    val foundDevices = remember {
        mutableStateMapOf<String, Device>(
            "F9:7F:8A:CA:E8:81" to Device(
                "8080S 0020557",
                "F9:7F:8A:CA:E8:81"
            )
        )
    }

    val prefs = context.getPreferences(Context.MODE_PRIVATE)
    val lastUsed = prefs.getString("lastUsed", ",")
        ?.split(",", ignoreCase = true, limit = 2)
    val lastUsedId = lastUsed?.get(0)
    val lastUsedName = lastUsed?.get(1)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Heart rate monitor", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(modifier = Modifier.animateContentSize(),
                onClick = {
                    if (!scanning) {
                        val status = handler.scan(context) { _, s ->
                            val device = s.device
                            val hasPerms =
                                permissionCheckAndRequest(
                                    context,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                            if (!hasPerms) return@scan
                            Log.d(
                                "app",
                                "found device\nname:${device.name}\nalias:${device.alias}\naddress:${device.address}"
                            )
                            foundDevices[device.address] = Device(device.name, device.address)
                        }
                        scanning = status == BluetoothHandler.ScanStatus.STARTED
                    } else {
                        handler.stopScan()
                        scanning = false
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
        if (!lastUsedId.isNullOrEmpty() && !lastUsedName.isNullOrEmpty()) {
            Text(text = "Last used device")
            Spacer(modifier = Modifier.height(8.dp))
            DeviceCard {
                DeviceCard(device = Device(lastUsedName, lastUsedId), onClick = onSelect)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Other devices
        Text(text = "Available devices")
        Spacer(modifier = Modifier.height(8.dp))
        DeviceCard {
            Column {
                foundDevices.forEach {
                    DeviceCard(device = it.value, onClick = onSelect)
                }
            }
        }
    }

}