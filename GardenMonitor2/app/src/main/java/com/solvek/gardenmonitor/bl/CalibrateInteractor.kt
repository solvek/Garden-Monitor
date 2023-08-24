package com.solvek.gardenmonitor.bl

import android.accounts.Account
import android.bluetooth.BluetoothManager
import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.juul.kable.peripheral
import com.solvek.gardenmonitor.Config
import com.solvek.gardenmonitor.R
import com.solvek.gardenmonitor.bl.db.AppDatabase
import com.solvek.gardenmonitor.bl.db.Point
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

class CalibrateInteractor(private val context: Context, private val scope: CoroutineScope, private val logger: (String) -> Unit) {
    private val peripheral = scope.peripheral(Config.ADDRESS)

    private val gmDevice = GMDevice(peripheral)
    private val accuWeatherDataSource = AccuWeatherDataSource()
    private val dbRepository = AppDatabase.create(context).getCalibrationDao()
    private val calibrator = TemperatureCalibrator()
    private val sheets = SheetsDataSource()

    private val errorHandler = CoroutineExceptionHandler { _, exception -> log(exception)}

    val isBluetoothDisabled: Boolean
        get() = !(context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled

    suspend fun calibrate() {
        scope.launch(errorHandler) {
            log("Connecting")
            peripheral.connect()

            log("Connected")

            log("Reading temperature from web")
            val realTemperatureD = async { accuWeatherDataSource.request(Config.AW_API_KEY, Config.AW_LOCATION_ID) }
            val currentPointsD = async {dbRepository.getAllPoints()}

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

//            val appendDataset = launch(Dispatchers.IO) {
//                log("Uploading record to google sheet")
//                sheets.upload(newPoint)
//            }

            val updateDbJob = launch {
                dbRepository.cleanOldPoints(calibrator.timeToTrim)
                dbRepository.appendPoint(newPoint)
                log("Local db updated")
            }

            log("Calibrating time")
            gmDevice.writeTime(System.currentTimeMillis())

            val calibrationResult = calibrator.calibrate(currentPointsD.await(), newPoint)

            if (calibrationResult == TemperatureCalibrator.Result.SUCCESS)
                with(calibrator) {
                    log("Temperature calibration parameters: x1=$x1, y1=$y1, x2=$x2, y2=$y2, k=$k, b=$b, paramK=$paramK, paramB=$paramB")
                    gmDevice.writeTemperatureCalibrationParameters(paramB, paramK)
                }
            else {
               log("Temperature calibration failed: $calibrationResult")
            }

            log("Disconnecting")
            peripheral.disconnect()

            updateDbJob.join()
//            appendDataset.join()
            log("All done")
        }.join()
    }

    fun setGoogleAccount(account: Account) {
        val jsonFactory = GsonFactory.getDefaultInstance()
        // GoogleNetHttpTransport.newTrustedTransport()
        val httpTransport =  AndroidHttp.newCompatibleTransport()

        val scopes = listOf(SheetsScopes.SPREADSHEETS)
        val credential = GoogleAccountCredential.usingOAuth2(context, scopes)
        credential.selectedAccount = account

        val appName = context.getString(R.string.app_name)
        sheets.service = Sheets.Builder(httpTransport, jsonFactory, credential)
            .setApplicationName(appName)
            .build()
    }

    private fun log(th: Throwable){
        Timber.tag(TAG).e(th, "Calibrating error")
        log("Error: $th")
    }

    private fun log(t: String){
        Timber.tag("Log").d(t)
        logger(t)
    }

    companion object {
        private const val TAG = "CalibrateInteractor"
    }
}