package com.solvek.gardenmonitor

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.redbear.chat.RBLService
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.toast
import org.json.JSONArray
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {
    private var mBluetoothLeService: RBLService? = null
    private val mDeviceAddress = "F6:30:3E:A2:AF:0B"

    private val map = HashMap<UUID, BluetoothGattCharacteristic>()
    private val db = FirebaseFirestore.getInstance()
    private var temperatureSensor: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonConnect.setOnClickListener{mBluetoothLeService?.let {s ->  connect(s)}}
        buttonDisconnect.setOnClickListener{disconnect()}
        buttonSend.setOnClickListener{send()}
        buttonTime.setOnClickListener{setClock()}
        buttonRestart.setOnClickListener{restart()}
        buttonSendTemperature.setOnClickListener{sendTemperature()}
        sbBrightness.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {sb -> setBrightness(sb.progress)}
            }
        })

        val gattServiceIntent = Intent(this, RBLService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
    }

    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, binder: IBinder) {
            connectState(allowConnect = true, allowDisconnect = false)
            mBluetoothLeService = (binder as RBLService.LocalBinder).service
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
            connectState(allowConnect = false, allowDisconnect = false)
            Toast
                .makeText(this@MainActivity, R.string.service_disconnected, Toast.LENGTH_LONG)
                .show()
        }
    }

    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            when (intent.action) {
                RBLService.ACTION_GATT_DISCONNECTED -> {
                    connectState(allowConnect = true, allowDisconnect = false)
                }
                RBLService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    connectState(allowConnect = false, allowDisconnect = true)
                    mBluetoothLeService?.let {
                        s -> getGattService(s.supportedGattService)
                    }
                }
                RBLService.ACTION_DATA_AVAILABLE -> handleBleInput(intent.getByteArrayExtra(RBLService.EXTRA_DATA))
            }
        }
    }

    private fun getGattService(gattService: BluetoothGattService?) {
        if (gattService == null) return

        val characteristic = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_TX)
        map[characteristic.uuid] = characteristic

        val characteristicRx = gattService.getCharacteristic(RBLService.UUID_BLE_SHIELD_RX)
        mBluetoothLeService?.setCharacteristicNotification(characteristicRx,true)
        mBluetoothLeService?.readCharacteristic(characteristicRx)
    }

    @SuppressLint("SetTextI18n")
    private fun handleBleInput(byteArray: ByteArray?) {
        if (byteArray == null) return
        val data = String(byteArray)
        if (data.startsWith("#K")){
            try {
                temperatureSensor = Integer.parseInt(data.substring(2, 6)) / 10.0
                tvTemperature.text = "Temperature: $temperatureSensor"
                buttonSendTemperature.isEnabled = true
            }
            catch(e: NumberFormatException){
                Log.e(TAG, "Failed to parse temperature: $data")
            }
        }
        else {
            tvResponses.append(data)
        }
    }

    private fun connect(service: RBLService){
        connectState(allowConnect = false, allowDisconnect = false)
        if (!service.initialize()) {
            Log.e(TAG, "Unable to initialize Bluetooth")
            finish()
        }
        // Automatically connects to the device upon successful start-up
        // initialization.
        service.connect(mDeviceAddress)
    }

    private fun disconnect(){
        mBluetoothLeService!!.disconnect()
    }

    private fun connectState(allowConnect: Boolean, allowDisconnect: Boolean){
        buttonConnect.isEnabled = allowConnect
        buttonDisconnect.isEnabled = allowDisconnect
        buttonTime.isEnabled = allowDisconnect
        buttonSend.isEnabled = allowDisconnect
        buttonRestart.isEnabled = allowDisconnect
        sbBrightness.isEnabled = allowDisconnect
    }

    private fun setBrightness(b: Int){
        send("#B${String.format("%03d", b)}")
    }

    @SuppressLint("SimpleDateFormat")
    private fun setClock() {
        send("#T${SimpleDateFormat("yyMMdduuHHmmss").format(Date())}")
    }

    private fun restart() {
        send("#R")
    }

    private fun send(){
        val text = etSend.text.toString()

        send(text)
    }

    private fun send(text: String){
        Log.d(TAG, "Sending to device via BLE: $text")
        val b: Byte = 0x00
        val tmp = text.toByteArray()
        val tx = ByteArray(tmp.size + 1)
        tx[0] = b
        for (i in 1 until tmp.size + 1) {
            tx[i] = tmp[i - 1]
        }

        val characteristic = map[RBLService.UUID_BLE_SHIELD_TX]
        characteristic?.let {ch ->
            ch.value = tx
            mBluetoothLeService?.writeCharacteristic(ch)
        }
    }

    private fun sendTemperature(){
        if (null == temperatureSensor) {toast(R.string.need_temperature)}

        doAsync{
            temperatureSensor?.let {temperatureSensor ->
                val result = URL("http://dataservice.accuweather.com/currentconditions/v1/" +
                        "${Constants.AW_LOCATION_ID}" +
                        "?apikey=${Constants.AW_API_KEY}")
                    .readText()
                val temperatureAw = JSONArray(result)
                    .getJSONObject(0)
                    .getJSONObject("Temperature")
                    .getJSONObject("Metric")
                    .getDouble("Value")

                val record = HashMap<String, Any>()
                record["temperature_sensor"] = temperatureSensor
                record["temperature_aw"] = temperatureAw
                record["time"] = Timestamp(Date())
                db.collection("temperature")
                    .add(record)
                    .addOnSuccessListener {
                        toast(R.string.temperature_added_to_server)
                    }
                    .addOnFailureListener{e->
                        Log.e(TAG, "Failed to add temperature record", e)
                        toast(R.string.temperature_server_failed)
                    }
            }
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE)

        return intentFilter
    }
}
