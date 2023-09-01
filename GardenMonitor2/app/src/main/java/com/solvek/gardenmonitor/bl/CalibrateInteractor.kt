package com.solvek.gardenmonitor.bl

import android.bluetooth.BluetoothManager
import android.content.Context
import com.juul.kable.peripheral
import com.solvek.gardenmonitor.Config
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
    private val localDataStore = LocalDataStore(context)
    private val calibrator = TemperatureCalibrator(localDataStore.readB(), localDataStore.readD())
//    private val sheets = SheetsDataSource()
    private val dataset = FirestoreDataSource(context)

    private val errorHandler = CoroutineExceptionHandler { _, exception -> log(exception)}

    val isBluetoothDisabled: Boolean
        get() = !(context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter.isEnabled

    suspend fun calibrate() {
        scope.launch(errorHandler) {
            log("Connecting")
            peripheral.connect()

            log("Connected")

            val realTemperatureD = async {
                log("Reading temperature from web")
                accuWeatherDataSource.request(Config.AW_API_KEY, Config.AW_LOCATION_ID)
            }

            val sensorTemperature = gmDevice.readSensorTemperature()
            gmDevice.sensorTemperature.first()
    //        val sensorTemperature = 25.5
            log("Sensor temperature: $sensorTemperature")

            val realTemperature = realTemperatureD.await()
            log("Real temperature: $realTemperature")

//            val appendDataset = launch(Dispatchers.IO) {
//                log("Uploading record to google sheet")
//                sheets.upload(newPoint)
//            }
            val newPoint = Point(sensorTemperature, realTemperature)

            val appendDataset = launch(Dispatchers.IO) {
                log("Uploading record to dataset")
                dataset.upload(newPoint)
                log("Point added to dataset")
//                log("Point added to dataset: $ref")
            }

            log("Calibrating time")
            gmDevice.writeTime(System.currentTimeMillis())

            val calibrationResult = calibrator.calibrate(newPoint)

            if (calibrationResult == TemperatureCalibrator.Result.SUCCESS)
                with(calibrator) {
                    localDataStore.store(calibrator.b, calibrator.d)
                    log("Temperature calibration parameters: d=$d, b=$b, paramD=$paramD, paramB=$paramB")
                    gmDevice.writeTemperatureCalibrationParameters(paramB, paramD)
                }
            else {
               log("Temperature calibration failed: $calibrationResult")
            }

            log("Disconnecting")
            peripheral.disconnect()

            appendDataset.join()
            log("All done")
        }.join()
    }

//    fun setGoogleAccount(account: Account) {
//        val jsonFactory = GsonFactory.getDefaultInstance()
//        // GoogleNetHttpTransport.newTrustedTransport()
//        val httpTransport =  AndroidHttp.newCompatibleTransport()
//
//        val scopes = listOf(SheetsScopes.SPREADSHEETS)
//        val credential = GoogleAccountCredential.usingOAuth2(context, scopes)
//        credential.selectedAccount = account
//
//        val appName = context.getString(R.string.app_name)
//        sheets.service = Sheets.Builder(httpTransport, jsonFactory, credential)
//            .setApplicationName(appName)
//            .build()
//    }

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