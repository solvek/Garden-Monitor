package com.solvek.gardenmonitor.bl

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.juul.kable.peripheral
import com.solvek.gardenmonitor.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CalibrateInteractor(private val context: Context, scope: CoroutineScope) {
    private val _logMessage = MutableStateFlow("")
    val logMessage: StateFlow<String> = _logMessage

    private val peripheral = scope.peripheral(Config.ADDRESS)
    private val gmDevice = GMDevice(peripheral)

    val isBluetoothDisabled: Boolean
        get() = !(context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled

    suspend fun calibrate(){
        try {
            log("Connecting")
            peripheral.connect()

            log("Connected")

            log("Calibrating time")
            gmDevice.writeTime(System.currentTimeMillis())

            log("Disconnecting")
            peripheral.disconnect()
            log("All done")
        }
        catch (th: Throwable){
            log("Error: ${th.message}")
        }
    }

    private fun log(t: String){
        Log.d("GMProcess", t)
        _logMessage.tryEmit(t)
    }
}