package com.solvek.gardenmonitor.bl

import android.util.Log
import com.juul.kable.Peripheral
import com.juul.kable.characteristicOf
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class GMDevice(private val peripheral: Peripheral
) : Peripheral by peripheral {
    val sensorTemperature = peripheral
        .observe(CH_RX)
        .map {
            val t = String(it)
            if (t.startsWith("#K")){
                try {
                    Integer.parseInt(t.substring(2, 6)) / 10.0
                }
                catch(e: NumberFormatException){
                    null
                }
            }
            else {
                null
            }
        }
        .filterNotNull()

    suspend fun writeTime(timestamp: Long){
        writeToDevice("#T${TIME_FORMAT.format(timestamp)}")
    }

    /**
     * paramB and paramK should be both between 0 and 255
     */
    suspend fun writeTemperatureCalibrationParameters(paramB: Int, paramK: Int){
        writeToDevice("#C${paramB.pad}${paramK.pad}")
    }

    suspend fun readSensorTemperature(): Double {
        var res: Double = -1.0

        val job = coroutineScope {
            launch {
                sensorTemperature.collect { st ->
                    Log.d("ST", "Sensor temperature: $st")
                    res = st
                    cancel()
                }
            }
        }

        job.join()
        return res
    }

    private suspend fun writeToDevice(t: String){
        val b: Byte = 0x00
        val tmp = t.toByteArray()
        val tx = ByteArray(tmp.size + 1)
        tx[0] = b
        for (i in 1 until tmp.size + 1) {
            tx[i] = tmp[i - 1]
        }
        Log.d("GMDevice", "Writing such data to device: $t")
        peripheral.write(CH_TX, tx)
    }

    private val Int.pad
        get() = this.toString().padStart(3, '0')

    companion object {
        @Suppress("SpellCheckingInspection")
        private val TIME_FORMAT = SimpleDateFormat("yyMMdduuHHmmss", Locale.US)

        private const val SERVICE = "713d0000-503e-4c75-ba94-3148f18d941e"
        private val CH_TX = characteristicOf(SERVICE, "713d0003-503e-4c75-ba94-3148f18d941e")
        private val CH_RX = characteristicOf(SERVICE, "713d0002-503e-4c75-ba94-3148f18d941e")
    }
}