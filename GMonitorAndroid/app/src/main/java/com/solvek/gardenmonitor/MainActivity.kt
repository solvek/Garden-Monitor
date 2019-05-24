package com.solvek.gardenmonitor

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.redbear.chat.RBLService
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {
    private var mBluetoothLeService: RBLService? = null
    val mDeviceAddress = "F6:30:3E:A2:AF:0B"

    private val map = HashMap<UUID, BluetoothGattCharacteristic>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonConnect.setOnClickListener{mBluetoothLeService?.let {s ->  connect(s)}}
        buttonDisconnect.setOnClickListener{disconnect()}
        buttonSend.setOnClickListener{send()}

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
                RBLService.ACTION_DATA_AVAILABLE -> displayData(intent.getByteArrayExtra(RBLService.EXTRA_DATA))
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

    private fun displayData(byteArray: ByteArray?) {
        if (byteArray == null) return
        val data = String(byteArray)
        tvResponses.append(data)
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
    }

    private fun send(){
        val text = etSend.text.toString()

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

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE)

        return intentFilter
    }
}
