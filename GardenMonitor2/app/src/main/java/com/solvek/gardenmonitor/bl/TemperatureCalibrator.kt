package com.solvek.gardenmonitor.bl

import com.solvek.gardenmonitor.Config
import com.solvek.gardenmonitor.bl.db.Point
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.atanh
import kotlin.math.roundToInt
import kotlin.math.tanh

class TemperatureCalibrator {
    var paramB:Int = 0
        private set

    var paramK:Int = 0
        private set

    var x1:Double = 0.0
        private set
    var y1:Double = 0.0
        private set
    var x2:Double = 0.0
        private set
    var y2:Double = 0.0
        private set
    var k:Double = 0.0
        private set
    var b:Double = 0.0
        private set

    var timeToTrim: Long = 0
        private set
    fun calibrate(points: List<Point>, newPoint: Point): Result{
        val orderedPoints = points.plus(newPoint).sortedByDescending { it.time }

        timeToTrim = if (orderedPoints.size > Config.KEEP_POINT_IN_DB)
            orderedPoints[Config.KEEP_POINT_IN_DB-1].time
            else 0


        if (orderedPoints.size < 2) return Result.TOO_LITTLE_DATA

        x1 = orderedPoints[0].sensorTemperature
        y1 = orderedPoints[0].realTemperature

        val prev = orderedPoints.drop(1).firstOrNull {
            abs(it.sensorTemperature - x1) >= 3
        } ?: return Result.NO_DIFF_RECORD

        x2 = prev.sensorTemperature
        y2 = prev.realTemperature

        k = 1.0 //(y2-y1)/(x2-x1)
        b = y1 - k*x1

        try {
            val arg = (b - M1) / N1
            paramB = if (arg <= -1){
                0
            } else if (arg >= 1){
                255
            } else {
                (atanh(arg) / G1 + 128).roundToInt()
            }
            paramK = 128 //((atanh((log(k, 2.0) - M2) / N2)) / G2 + 128).roundToInt()
        }
        catch (th: Throwable){
            Timber.tag(TAG).e(th, "Couldn't calculate paramB and/or parmaK")
            return Result.PARAMS_CALC_FAILED
        }

        return Result.SUCCESS
    }

    enum class Result {
        SUCCESS,
        NO_DIFF_RECORD,
        TOO_LITTLE_DATA,
        PARAMS_CALC_FAILED
    }

    companion object {
        private const val TAG = "Calibrator"

        private const val G1 = 1.0/256
        private const val P1 = -10
        private const val Q1 = 10
        private val T1 = tanh(127*G1)
        private val N1 = (Q1-P1)/(T1 - tanh(-128*G1))
        private val M1 = Q1 - N1*T1

//        private const val G2 = 1.0/256
//        private const val P2 = -1
//        private const val Q2 = 1
//        private val T2 = tanh(127*G2)
//        private val N2 = (Q2-P2)/(T2 - tanh(-128*G2))
//        private val M2 = Q2 - N2*T2
    }
}