package com.sqooid.fheart

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.PictureInPictureModeChangedInfo
import androidx.core.util.Consumer
import com.sqooid.fheart.lib.MainViewModel
import com.sqooid.fheart.ui.DeviceSelector
import com.sqooid.fheart.ui.HeartRateDisplay
import com.sqooid.fheart.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val isPipSupported by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: MainViewModel by viewModels()
        viewModel.init(this)
        val activity = this

        if (!isPipSupported) {
            Toast.makeText(
                this,
                "This device does not support picture in picture mode",
                Toast.LENGTH_LONG
            ).show()
        }

        setContent {
            val inPipMode = rememberIsInPipMode(activity = this)
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    if (inPipMode) {
                        HeartRateDisplay(rate = viewModel.hrValue, true) {
                            viewModel.displayLayout = it
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
                                HeartRateDisplay(rate = viewModel.hrValue, false) {
                                    viewModel.displayLayout = it
                                }
                                Button(modifier = Modifier, onClick = {
                                    viewModel.startPip(activity)
                                }) {
                                    Text(
                                        text = "Start floating\nwindow",
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            DeviceSelector(
                                context = activity,
                                viewModel
                            )
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
    var pipMode by remember { mutableStateOf(activity.isInPictureInPictureMode) }
    DisposableEffect(activity) {
        val observer = Consumer<PictureInPictureModeChangedInfo> { info ->
            pipMode = info.isInPictureInPictureMode
        }
        activity.addOnPictureInPictureModeChangedListener(observer)
        onDispose { activity.removeOnPictureInPictureModeChangedListener(observer) }
    }
    return pipMode
}

