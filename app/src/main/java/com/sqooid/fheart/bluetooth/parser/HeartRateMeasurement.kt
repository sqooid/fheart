package com.sqooid.fheart.bluetooth.parser

import com.sqooid.fheart.bluetooth.DataParser

fun Byte.toPositiveInt() = toInt() and 0xFF

class HeartRateMeasurement(val measurement: Int) : DataParser<HeartRateMeasurement> {
    override fun parseFromBytes(byteArray: ByteArray): HeartRateMeasurement {
        val isU8 = (byteArray[0].toInt() and 1) == 0
        val hr = if (isU8) {
            byteArray[1].toPositiveInt()
        } else {
            byteArray[1].toPositiveInt() shl 8 or byteArray[2].toPositiveInt()
        }.toInt()
        return HeartRateMeasurement(hr)
    }
}