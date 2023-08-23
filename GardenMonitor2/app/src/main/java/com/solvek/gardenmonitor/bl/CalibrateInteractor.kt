package com.solvek.gardenmonitor.bl

import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.juul.kable.peripheral
import com.solvek.gardenmonitor.Config
import com.solvek.gardenmonitor.bl.db.AppDatabase
import com.solvek.gardenmonitor.bl.db.Point
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CalibrateInteractor(private val context: Context, private val scope: CoroutineScope, private val logger: (String) -> Unit) {
    private val peripheral = scope.peripheral(Config.ADDRESS)

    private val gmDevice = GMDevice(peripheral)
    private val accuWeatherDataSource = AccuWeatherDataSource()
    private val dbRepository = AppDatabase.create(context).getCalibrationDao()
    private val calibrator = TemperatureCalibrator()

    private val errorHandler = CoroutineExceptionHandler { _, exception -> log(exception)}

    val isBluetoothDisabled: Boolean
        get() = !(context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled

    suspend fun calibrate() = scope.launch(errorHandler){
        log("Connecting")
        peripheral.connect()

        log("Connected")

        log("Reading temperature from web")
        val realTemperatureD = async { accuWeatherDataSource.request(Config.AW_API_KEY, Config.AW_LOCATION_ID) }
        val currentPointsD = async {dbRepository.getAllPoints()}

        log("Calibrating time")
        gmDevice.writeTime(System.currentTimeMillis())

        val sensorTemperature = gmDevice.readSensorTemperature()
        gmDevice.sensorTemperature.first()
//        val sensorTemperature = 25.5
        log("Sensor temperature: $sensorTemperature")

        val realTemperature = realTemperatureD.await()
        log("Real temperature: $realTemperature")

        val newPoint = Point(
            sensorTemperature = sensorTemperature,
            realTemperature = realTemperature
        )

        calibrator.calibrate(currentPointsD.await(), newPoint)

        val updateDbJob = launch {
            dbRepository.cleanOldPoints(calibrator.timeToTrim)
            dbRepository.appendPoint(newPoint)
            log("Local db updated")
        }

        if (calibrator.success)
            with(calibrator) {
                log("Temperature calibration parameters: x1=$x1, y1=$y1, x2=$x2, y2=$y2, k=$k, b=$b, paramK=$paramK, paramB=$paramB")
                gmDevice.writeTemperatureCalibrationParameters(paramB, paramK)
            }
        else {
           log("Not found points to calibrate temperature yet")
        }

        log("Disconnecting")
        peripheral.disconnect()

        updateDbJob.join()
        log("All done")
    }

    private fun log(th: Throwable){
        Log.e("GMProcess", "Calibrating error", th)
        log("Error: $th")
    }

    private fun log(t: String){
        Log.d("Log", t)
        logger(t)
    }
}