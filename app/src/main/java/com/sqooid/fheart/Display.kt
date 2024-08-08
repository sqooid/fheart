package com.sqooid.fheart

import android.graphics.Rect
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRect
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toRect

@Composable
fun HeartRateDisplay(rate: Int, pip: Boolean, rectCallback: (_: LayoutCoordinates) -> Unit) {
    Surface(
        tonalElevation = if (!pip) 16.dp else {
            0.dp
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .onGloballyPositioned {
                if (!pip) rectCallback(it)
            }
    ) {
        Text(
            text = rate.toString(),
            style = MaterialTheme.typography.displayLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.run {
                if (pip) {
                    wrapContentHeight(Alignment.CenterVertically)
                } else {
                    padding(48.dp)
                }
            },
        )
    }
}