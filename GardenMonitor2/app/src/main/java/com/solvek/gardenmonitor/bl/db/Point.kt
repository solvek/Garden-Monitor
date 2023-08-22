package com.solvek.gardenmonitor.bl.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Point(
    @PrimaryKey val time: Long = System.currentTimeMillis(),
    val sensorTemperature: Double,
    val realTemperature: Double
)