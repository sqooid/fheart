package com.sqooid.fheart

import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.graphics.toRect
import androidx.core.util.Consumer
import com.sqooid.fheart.bluetooth.GattListener
import com.sqooid.fheart.bluetooth.GattScanner
import com.sqooid.fheart.ui.theme.MyApplicationTheme
import com.sqooid.fheart.ui.theme.Typography

class MainActivity : ComponentActivity() {

    private val isPipSupported by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scanner = GattScanner()
        val activity = this
        var displayLayout: LayoutCoordinates? = null
        val pipBuilder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(4, 3))
            .setAutoEnterEnabled(true)


        val prefs = getPreferences(Context.MODE_PRIVATE)
        val (lastAdd, lastName) = prefs.getString("lastUsed", ",")
            ?.split(",", ignoreCase = true, limit = 2) ?: listOf("", "")

        setContent {
            var lastDevice by remember {
                mutableStateOf<LastDevice?>(
                    if (lastAdd.isNotBlank()) {
                        LastDevice(lastAdd, lastName)
                    } else null
                )
            }
//            var hrListener by remember {
//                mutableStateOf<GattListener<?>>(null)
//            }
            val inPipMode = rememberIsInPipMode(activity = this)
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    if (inPipMode) {
                        HeartRateDisplay(rate = 60, inPipMode) {
                            displayLayout = it
                        }

                    } else {
                        Column(modifier = Modifier.run {
                            padding(8.dp)
                        }) {
                            Text(
                                text = "Floating Heart rate monitor",
                                style = MaterialTheme.typography.displaySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp)
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HeartRateDisplay(rate = 60, inPipMode) {
                                    displayLayout = it
                                }
                                Button(modifier = Modifier, onClick = {
                                    displayLayout?.let {
                                        Log.d("app", displayLayout.toString())
                                        val pipParams = pipBuilder.setSourceRectHint(
                                            it.boundsInWindow().toAndroidRectF().toRect()
                                        )
                                            .build()
                                        enterPictureInPictureMode(pipParams)
                                    }
                                }) {
                                    Text(
                                        text = "Start floating\nwindow",
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            DeviceSelector(context = activity, scanner, lastDevice) { device ->
                                val addr = device.address
                                val name = device.name
                                val lastString = "${addr},${name}"
                                prefs.edit().putString("lastUsed", lastString).apply()
                                lastDevice = LastDevice(addr, name)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        Log.d("app", "pip: $isInPictureInPictureMode")
    }
}

@Composable
fun rememberIsInPipMode(activity: ComponentActivity): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
        DisposableEffect(activity) {
            val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
                pipMode = info.isInPictureInPictureMode
            }
            activity.addOnPictureInPictureModeChangedListener(observer)
            onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
        }
        return pipMode
    } else {
        return false
    }
}

private fun pipParams(rect: Rect): PictureInPictureParams {
    return PictureInPictureParams.Builder().setAspectRatio(Rational(4, 3)).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setSeamlessResizeEnabled(true)
        }
        setSourceRectHint(rect)
    }.build()
}
