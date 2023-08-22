package com.solvek.gardenmonitor

import com.juul.kable.Peripheral
import com.juul.kable.characteristicOf
import com.juul.kable.peripheral
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import java.text.SimpleDateFormat
import java.util.Locale

class CalibrateInteractor() {
    val logMessage = MutableSharedFlow<String>()

    private lateinit var peripheral: Peripheral

    suspend fun calibrate(scope: CoroutineScope){
//        peripheral.observe()
        log("Connecting")
        peripheral = scope.peripheral(Config.address)
        peripheral.connect()
        log("Connected")

        writeTime()

        log("Disconnecting")
        peripheral.disconnect()
        log("Finished")
    }

    private suspend fun writeTime(){
        log("Writing time")
        writeToDevice("#T${TIME_FORMAT.format(System.currentTimeMillis())}")
    }

    private suspend fun writeToDevice(t: String){
        log("Sending text: $t")
        val b: Byte = 0x00
        val tmp = t.toByteArray()
        val tx = ByteArray(tmp.size + 1)
        tx[0] = b
        for (i in 1 until tmp.size + 1) {
            tx[i] = tmp[i - 1]
        }
        peripheral.write(CH_TX, tx)
    }

    private fun log(t: String){
        logMessage.tryEmit(t)
    }

    companion object {
        @Suppress("SpellCheckingInspection")
        private val TIME_FORMAT = SimpleDateFormat("yyMMdduuHHmmss", Locale.US)

        private val SERVICE = "713d0000-503e-4c75-ba94-3148f18d941e"
        private val CH_TX = characteristicOf(SERVICE, "713d0003-503e-4c75-ba94-3148f18d941e")
    }
}