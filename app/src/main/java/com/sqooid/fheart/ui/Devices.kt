package com.sqooid.fheart.ui

import android.annotation.SuppressLint
import android.app.Activity
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sqooid.fheart.R
import com.sqooid.fheart.bluetooth.GattDevice
import com.sqooid.fheart.lib.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class DeviceState {
    IDLE,
    LOADING,
    LOADED,
}

@Composable
fun DeviceItem(
    device: GattDevice?,
    recordedName: String = "",
    state: DeviceState = DeviceState.IDLE,
    battery: Int = 0,
    onClick: (_: GattDevice) -> Unit
) {
    var name by remember {
        mutableStateOf(recordedName)
    }
    val coroutineScope = rememberCoroutineScope()
    fun loadName() {
        if (device != null) {
            coroutineScope.launch(context = Dispatchers.Main) {
                name = device.name
            }
        }
    }
    loadName()

    TextButton(
        onClick = { device?.let(onClick) },
        modifier = Modifier
            .clip(RoundedCornerShape(1.dp))
            .fillMaxWidth(),
        shape = RectangleShape
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
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
            if (state == DeviceState.LOADING) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp))
            } else if (state == DeviceState.LOADED) {
                if (battery > 0) {
                    Text(text = "$battery%")
                } else {
                    Text(text = "Connected")
                }
            }
        }
    }
}

@Composable
fun DeviceSection(content: @Composable () -> Unit) {
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
fun DeviceSelector(
    context: Activity,
    viewModel: MainViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Heart rate monitor", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(modifier = Modifier.animateContentSize(),
                onClick = {
                    viewModel.toggleScan(context)

                }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = if (viewModel.scanning) "Scanning" else "Start scan")
                    if (viewModel.scanning) {
                        Spacer(modifier = Modifier.width(4.dp))
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Last used device
        if (viewModel.lastDevice != null) {
            Text(text = "Last used device")
            Spacer(modifier = Modifier.height(8.dp))
            DeviceSection {
                DeviceItem(
                    device = viewModel.lastDevice,
                    recordedName = viewModel.lastDeviceDummy?.name ?: "",
                    state = if (viewModel.loadingDevice) DeviceState.LOADING else if (viewModel.hrValue > 0) DeviceState.LOADED else DeviceState.IDLE,
                    battery = viewModel.batteryValue,
                    onClick = { viewModel.selectDevice(context, it, viewModel.hrValue > 0) }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Other devices
        Text(text = "Available devices")
        Spacer(modifier = Modifier.height(8.dp))
        DeviceSection {
            LazyColumn {
                viewModel.foundDevices.forEach {
                    item {
                        DeviceItem(
                            device = it.value,
                            onClick = { viewModel.selectDevice(context, it) })
                    }
                }
            }
        }
    }

}