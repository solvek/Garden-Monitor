package com.solvek.gardenmonitor.bl

import com.juul.kable.Peripheral
import com.juul.kable.characteristicOf
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.min

class GMDevice(private val peripheral: Peripheral
) : Peripheral by peripheral {
    val sensorTemperature = peripheral
        .observe(CH_RX)
        .map { bytes ->
            val t = String(bytes).dropLastWhile { !it.isDigit() }
            if (t.startsWith("#K")){
                try {
                    Integer.parseInt(t.substring(2, min(6, t.length))) / 10.0f
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
        if (paramB < 0 || paramB >255){
            Timber.tag(TAG).e("paramB should be a byte. Its value is $paramB")
            return
        }
        if (paramK < 0 || paramK >255){
            Timber.tag(TAG).e("paramK should be a byte. Its value is $paramK")
            return
        }
        writeToDevice("#C${paramB.pad}${paramK.pad}")
    }

//    suspend fun readSensorTemperature(): Float {
//        var res: Float = -1.0f
//
//        val job = coroutineScope {
//            launch {
//                sensorTemperature.collect { st ->
//                    Timber.tag(TAG).d("Sensor temperature: $st")
//                    res = st
//                    cancel()
//                }
//            }
//        }
//
//        job.join()
//        return res
//    }

    private suspend fun writeToDevice(t: String){
        val b: Byte = 0x00
        val tmp = t.toByteArray()
        val tx = ByteArray(tmp.size + 1)
        tx[0] = b
        for (i in 1 until tmp.size + 1) {
            tx[i] = tmp[i - 1]
        }
        Timber.tag(TAG).d("Writing such data to device: $t")
        peripheral.write(CH_TX, tx)
    }

    private val Int.pad
        get() = this.toString().padStart(3, '0')

    companion object {
        private const val TAG = "GMDevice"

        @Suppress("SpellCheckingInspection")
        private val TIME_FORMAT = SimpleDateFormat("yyMMdduuHHmmss", Locale.US)

        private const val SERVICE = "713d0000-503e-4c75-ba94-3148f18d941e"
        private val CH_TX = characteristicOf(SERVICE, "713d0003-503e-4c75-ba94-3148f18d941e")
        private val CH_RX = characteristicOf(SERVICE, "713d0002-503e-4c75-ba94-3148f18d941e")
   }
}