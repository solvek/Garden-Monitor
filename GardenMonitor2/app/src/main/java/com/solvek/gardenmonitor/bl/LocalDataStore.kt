package com.solvek.gardenmonitor.bl

import android.content.Context

class LocalDataStore(context: Context) {
    private val prefs = context.getSharedPreferences("GM", Context.MODE_PRIVATE)

    fun readB() = prefs.getFloat(PreferencesKeys.B, 0.0f)

    fun readD() = prefs.getFloat(PreferencesKeys.D, 0.0f)

    fun store(b: Float, d: Float){
        prefs.edit()
            .putFloat(PreferencesKeys.B, b)
            .putFloat(PreferencesKeys.D, d)
            .apply()
    }

    private object PreferencesKeys {
        const val B = "KOEF_B"
        const val D = "KOEF_D"
    }
}