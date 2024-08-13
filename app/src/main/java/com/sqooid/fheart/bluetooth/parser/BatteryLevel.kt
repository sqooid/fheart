package com.sqooid.fheart.bluetooth.parser

import com.sqooid.fheart.bluetooth.DataParser

class BatteryLevel(val percentage: Int) : DataParser<BatteryLevel> {
    override fun parseFromBytes(byteArray: ByteArray): BatteryLevel {
        return BatteryLevel(byteArray[0].toPositiveInt())
    }
}