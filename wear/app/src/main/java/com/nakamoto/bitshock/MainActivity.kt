package com.nakamoto.bitshock

import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.getSystemService
import androidx.wear.compose.material.MaterialTheme

class MainActivity : ComponentActivity() {
    private val viewModel: WatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.start()
        val vibrator = getSystemService<Vibrator>()

        setContent {
            val state by viewModel.state.collectAsState()
            LaunchedEffect(state.celebratingHeight) {
                if (state.celebratingHeight == null) return@LaunchedEffect
                vibrator?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
            }
            MaterialTheme {
                BitshockWatch(
                    state = state,
                    onRefresh = viewModel::refresh,
                    onSimulateBlock = viewModel::simulateNewBlock,
                )
            }
        }
    }
}
