package com.solvek.gardenmonitor.bl.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface CalibrationDao {
    @Query("SELECT * FROM Point")
    fun getAllPoint(): List<Point>

    @Query("DELETE FROM Point WHERE time < :time")
    fun cleanOldPoints(time: Long)

    @Insert
    fun appendPoint(point: Point)
}