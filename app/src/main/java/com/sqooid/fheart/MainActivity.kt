package com.sqooid.fheart

import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sqooid.fheart.bluetooth.GattScanner
import com.sqooid.fheart.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val isPipSupported by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scanner = GattScanner()
        val activity = this
        setContent {
            MyApplicationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Button(onClick = { enterPictureInPictureMode(pipParams()) }) {
                            Text(text = "PiP")
                        }
                        DeviceSelector(context = activity, scanner) {

                        }
                    }
                }
            }
        }
    }
}

private fun pipParams(): PictureInPictureParams {
    return PictureInPictureParams.Builder().setAspectRatio(Rational(4, 3)).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setSeamlessResizeEnabled(false)
        }
    }.build()
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!", modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme {
        Greeting("Android")
    }
}