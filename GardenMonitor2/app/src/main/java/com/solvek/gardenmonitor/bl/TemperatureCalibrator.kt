package com.solvek.gardenmonitor.bl

import kotlin.math.atanh
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.tanh

class TemperatureCalibrator(b: Float, d: Float) {
    var paramB:Int = 0
        private set

    var paramD:Int = 0
        private set

    private var _b: Float
    val b: Float
        get() = _b
    private var _d: Float
    val d: Float
        get() = _d

    init {
        _b = b
        _d = d
    }

    fun calibrate(p: Point): Result{
        val x = p.sensorTemperature
        val y = p.realTemperature

        if (x == 20.0f){
            _d = y-20.0f
        }
        else {
            _d = (_b * x*(x-20)+_d*(x-20).pow(2)+20*x*(y-x))/(2*(x*x-20*x+200))
            _b = ((_d+20)*x-20*y)/(x-20)
        }

        paramB = _b.pack
        paramD = _d.pack

        return Result.SUCCESS
    }

    enum class Result {
        SUCCESS
    }

    companion object {
//        private const val TAG = "Calibrator"

        private const val M = 50.0
        private val G = -M/atanh(-128.0/129)

        private val Float.pack
            get() = (128+129*tanh(this/G)).roundToInt()
    }
}