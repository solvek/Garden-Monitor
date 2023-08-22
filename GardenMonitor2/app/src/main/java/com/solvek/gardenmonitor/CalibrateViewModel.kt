package com.solvek.gardenmonitor

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch

class CalibrateViewModel(context: Context) : AndroidViewModel(context.applicationContext as Application){
    private val calibrateInteractor = CalibrateInteractor(context)

    var isInProgress: Boolean by mutableStateOf(false)
        private set
    var logContent: String by mutableStateOf("")
        private set
    fun calibrate(){
        viewModelScope.launch {
            isInProgress = true
            logContent = ""

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