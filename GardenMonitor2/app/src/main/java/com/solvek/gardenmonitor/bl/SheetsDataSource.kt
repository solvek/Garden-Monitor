package com.solvek.gardenmonitor.bl

import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import com.solvek.gardenmonitor.Config
import com.solvek.gardenmonitor.bl.db.Point
import timber.log.Timber

class SheetsDataSource {
    lateinit var service: Sheets

    fun upload(point: Point){
        if (!this::service.isInitialized){
            throw Exception("Service is not initialized yet. Please wait")
        }

        val rows = listOf(
            listOf(point.time, point.sensorTemperature, point.realTemperature)
        )
        val body = ValueRange().setValues(rows)
        val range = "'${Config.SHEET_NAME}'!A2"
        val result = service.spreadsheets().values().append(Config.SHEET_ID, range, body)
            .setValueInputOption("RAW")
            .setInsertDataOption("INSERT_ROWS")
            .execute()
        Timber.tag(TAG).d("newRows=${result.updates.updatedRows}")
    }

    companion object {
        private const val TAG = "SheetsDataSource"
    }
}