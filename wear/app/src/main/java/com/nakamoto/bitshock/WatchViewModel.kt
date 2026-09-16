package com.nakamoto.bitshock

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class WatchViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val engine = (application as BitshockApplication).engine

    val state = engine.state

    fun start() {
        engine.start()
    }

    fun refresh() = engine.refresh()

    fun simulateNewBlock() = engine.simulateNewBlock()
}
