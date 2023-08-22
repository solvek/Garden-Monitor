package com.solvek.gardenmonitor.bl.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM Point")
    suspend fun getAllPoints(): List<Point>

    @Query("DELETE FROM Point WHERE time < :time")
    suspend fun cleanOldPoints(time: Long)

    @Insert
    suspend fun appendPoint(point: Point)
}