package com.solvek.gardenmonitor.bl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.URL

class AccuWeatherDataSource {
    suspend fun request(apiKey: String, location: Int) = withContext(Dispatchers.Default) {
        val result = URL("https://dataservice.accuweather.com/currentconditions/v1/$location?apikey=$apiKey")
            .readText()
        JSONArray(result)
            .getJSONObject(0)
            .getJSONObject("Temperature")
            .getJSONObject("Metric")
            .getDouble("Value")
            .toFloat()
    }
}