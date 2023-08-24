package com.solvek.gardenmonitor.bl

import com.google.api.services.sheets.v4.Sheets
import com.solvek.gardenmonitor.bl.db.Point

class SheetsDataSource {
    lateinit var service: Sheets

    suspend fun upload(point: Point){
        if (!this::service.isInitialized){
            throw Exception("Service is not initialized yet. Please wait")
        }
    }
}