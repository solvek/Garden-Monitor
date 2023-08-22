package com.solvek.gardenmonitor.bl

import com.juul.kable.Peripheral
import com.juul.kable.characteristicOf
import java.text.SimpleDateFormat
import java.util.Locale

class GMDevice(private val peripheral: Peripheral
) : Peripheral by peripheral {
//    val sensorTemperature: Flow<Float>

    suspend fun writeTime(timestamp: Long){
        writeToDevice("#T${TIME_FORMAT.format(timestamp)}")
    }

//    fun writeTemperatureCalibrationParameters(paramB: Byte, paramK: Byte){
//
//    }

    private suspend fun writeToDevice(t: String){
        val b: Byte = 0x00
        val tmp = t.toByteArray()
        val tx = ByteArray(tmp.size + 1)
        tx[0] = b
        for (i in 1 until tmp.size + 1) {
            tx[i] = tmp[i - 1]
        }
        peripheral.write(CH_TX, tx)
    }

    companion object {
        @Suppress("SpellCheckingInspection")
        private val TIME_FORMAT = SimpleDateFormat("yyMMdduuHHmmss", Locale.US)

        private const val SERVICE = "713d0000-503e-4c75-ba94-3148f18d941e"
        private val CH_TX = characteristicOf(SERVICE, "713d0003-503e-4c75-ba94-3148f18d941e")
    }
}