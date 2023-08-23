package com.solvek.gardenmonitor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun CalibrateScreen(model: CalibrateViewModel) {
    val isReady by model.isReady.collectAsState()
    val logContent by model.logContent.collectAsState()

    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Text("Is ready: $isReady")
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = model::calibrate,
            enabled = isReady
        ) {
            Text(text = stringResource(R.string.calibrate))
        }
        Text(
            text = logContent,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        )
    }
}

//@Preview(showBackground = true)
//@Composable
//fun CalibrateScreenPreview() {
//    GardenMonitorTheme {
//        CalibrateScreen(null, "some log")
//    }
//}