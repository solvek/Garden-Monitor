package com.solvek.gardenmonitor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.solvek.gardenmonitor.ui.theme.GardenMonitorTheme

@Composable
fun CalibrateScreen(onCalibrate:(()->Unit)?, logContent: String) {
    Column(Modifier.fillMaxWidth()) {
        Button(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = onCalibrate ?: {},
            enabled = onCalibrate != null
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

@Preview(showBackground = true)
@Composable
fun CalibrateScreenPreview() {
    GardenMonitorTheme {
        CalibrateScreen(null, "some log")
    }
}