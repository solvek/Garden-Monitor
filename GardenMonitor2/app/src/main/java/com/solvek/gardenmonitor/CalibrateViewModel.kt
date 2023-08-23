package com.solvek.gardenmonitor

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solvek.gardenmonitor.bl.CalibrateInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CalibrateViewModel(context: Context) : AndroidViewModel(context.applicationContext as Application){
    private val calibrateInteractor = CalibrateInteractor(context, viewModelScope){message ->
//        Log.d("ViewModel", "Log message: $message")
        logText += "\n$message"
        _logContent.value = logText
    }

    private val _isReady = MutableStateFlow(true)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _logContent= MutableStateFlow("Ready")
    val logContent: StateFlow<String> = _logContent.asStateFlow()

    private var logText = ""
    fun calibrate(){
//        _isReady.value = false
//        viewModelScope.launch {
//            delay()
//            _isReady.value = true
//        }

        if (!_isReady.value) return

        if (calibrateInteractor.isBluetoothDisabled){
            _logContent.value = "Please enable bluetooth adapter"
            return
        }

        _isReady.value = false
        Log.d("ViewModel", "New is ready value: ${isReady.value} (backed property ${_isReady.value})")

        viewModelScope.launch {
            logText = "Started calibration"
            _logContent.value = logText
            calibrateInteractor.calibrate()
            _isReady.value = true
        }
    }

//    private suspend fun delay(){
//        delay(3.seconds)
//    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
//                val savedStateHandle = createSavedStateHandle()
                val application = checkNotNull(this[APPLICATION_KEY])
                CalibrateViewModel(application)
            }
        }
    }
}