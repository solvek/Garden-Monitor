package com.redbear.chat

import java.util.HashMap

/**
 * This class includes a small subset of standard GATT attributes for
 * demonstration purposes.
 */
object RBLGattAttributes {
    private val attributes = HashMap<String, String>()
    var CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    var BLE_SHIELD_TX = "713d0003-503e-4c75-ba94-3148f18d941e"
    var BLE_SHIELD_RX = "713d0002-503e-4c75-ba94-3148f18d941e"
    var BLE_SHIELD_SERVICE = "713d0000-503e-4c75-ba94-3148f18d941e"

    init {
        // RBL Services.
        attributes["713d0000-503e-4c75-ba94-3148f18d941e"] = "BLE Shield Service"
        // RBL Characteristics.
        attributes[BLE_SHIELD_TX] = "BLE Shield TX"
        attributes[BLE_SHIELD_RX] = "BLE Shield RX"
    }

    fun lookup(uuid: String, defaultName: String): String {
        val name = attributes[uuid]
        return name ?: defaultName
    }
}