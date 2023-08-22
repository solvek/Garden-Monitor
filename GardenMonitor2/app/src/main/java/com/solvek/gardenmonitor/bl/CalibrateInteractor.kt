package com.solvek.gardenmonitor.bl

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.juul.kable.peripheral
import com.solvek.gardenmonitor.Config
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CalibrateInteractor(private val context: Context, private val scope: CoroutineScope) {
    private val _logMessage = MutableStateFlow("")
    val logMessage: StateFlow<String> = _logMessage

    private val peripheral = scope.peripheral(Config.ADDRESS)
    private val gmDevice = GMDevice(peripheral)
    private val accuWeatherDataSource = AccuWeatherDataSource()

    private val errorHandler = CoroutineExceptionHandler { _, exception -> log(exception)}

    val isBluetoothDisabled: Boolean
        get() = !(context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled

    suspend fun calibrate() = scope.launch(errorHandler){
        log("Connecting")
        peripheral.connect()

        log("Connected")

        log("Reading temperature from web")
        val realTemperatureD = async { accuWeatherDataSource.request(Config.AW_API_KEY, Config.AW_LOCATION_ID) }

        log("Calibrating time")
        gmDevice.writeTime(System.currentTimeMillis())

        val sensorTemperature = gmDevice.sensorTemperature.first()
        log("Sensor temperature: $sensorTemperature")

        val realTemperature = realTemperatureD.await()
        log("Real temperature: $realTemperature")

        log("Disconnecting")
        peripheral.disconnect()
        log("All done")
    }

    private fun log(th: Throwable){
        Log.e("GMProcess", "Calibrating error", th)
        log("Error: $th")
    }

    private fun log(t: String){
        _logMessage.tryEmit(t)
    }
}