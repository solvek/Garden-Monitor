package com.solvek.gardenmonitor

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.solvek.gardenmonitor.ui.theme.GardenMonitorTheme

class MainActivity : ComponentActivity() {
    private val viewModel: CalibrateViewModel by viewModels { CalibrateViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContent {
            GardenMonitorTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CalibrateScreen(viewModel::calibrate, viewModel.isInProgress, viewModel.logContent)
                }
            }
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }

        val permission = android.Manifest.permission.BLUETOOTH_CONNECT

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED){
            return
        }

        requestMultiplePermissions.launch(arrayOf(permission))
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }
        }
}