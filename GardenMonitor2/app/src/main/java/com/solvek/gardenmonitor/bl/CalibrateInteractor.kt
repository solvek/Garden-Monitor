package com.solvek.gardenmonitor.bl

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.juul.kable.peripheral
import com.solvek.gardenmonitor.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

class CalibrateInteractor(private val context: Context, scope: CoroutineScope) {
    var logMessage = MutableStateFlow("")
     private set

    private val peripheral = scope.peripheral(Config.ADDRESS)
    private val gmDevice = GMDevice(peripheral)

    val isBluetoothDisabled: Boolean
        get() = !(context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled

    suspend fun calibrate(){
        try {
            log("Connecting")
            peripheral.connect()

            log("Connected")

            gmDevice.writeTime(System.currentTimeMillis())

            log("Disconnecting")
            peripheral.disconnect()
            log("Finished")
        }
        catch (th: Throwable){
            log("Error: ${th.message}")
        }
    }

    private fun log(t: String){
        Log.d("GMProcess", t)
        logMessage.tryEmit(t)
    }
}