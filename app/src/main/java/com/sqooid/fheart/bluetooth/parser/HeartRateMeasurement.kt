package com.sqooid.fheart.bluetooth.parser

import com.sqooid.fheart.bluetooth.DataParser

class HeartRateMeasurement(val measurement: Int): DataParser<HeartRateMeasurement> {
    override fun parseFromBytes(byteArray: ByteArray): HeartRateMeasurement {
        val isU8 = (byteArray[0].toInt() and 1) == 0
        val hr = if (isU8) {
            byteArray[1].toInt()
        } else {
            byteArray[1].toInt() shl 8 or byteArray[2].toInt()
        }
        return HeartRateMeasurement(hr)
    }
}