package com.solvek.gardenmonitor.bl

data class Point(val sensorTemperature: Double, val realTemperature: Double, val time: Long = System.currentTimeMillis())
