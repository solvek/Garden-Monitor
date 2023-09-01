package com.solvek.gardenmonitor.bl

data class Point(val sensorTemperature: Float, val realTemperature: Float, val time: Long = System.currentTimeMillis())