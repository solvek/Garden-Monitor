package com.solvek.gardenmonitor

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solvek.gardenmonitor.bl.CalibrateInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CalibrateViewModel(context: Context) : AndroidViewModel(context.applicationContext as Application){
    private val calibrateInteractor = CalibrateInteractor(context, viewModelScope){message ->
//        Log.d("ViewModel", "Log message: $message")
        logText += "\n$message"
        _logContent.value = logText
    }

    private val _isInProgress = MutableStateFlow(false)
    val isInProgress: StateFlow<Boolean> = _isInProgress
    private val _logContent= MutableStateFlow("Ready")
    val logContent: StateFlow<String> = _logContent

    private var logText = ""
    fun calibrate(){
        if (_isInProgress.value) return

        if (calibrateInteractor.isBluetoothDisabled){
            _logContent.value = "Please enable bluetooth adapter"
            return
        }

        viewModelScope.launch {
            _isInProgress.value = true
            logText = "Started calibration"
            _logContent.value = logText
            calibrateInteractor.calibrate()
            _isInProgress.value = false
        }
    }

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