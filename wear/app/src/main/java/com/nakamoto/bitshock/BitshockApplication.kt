package com.nakamoto.bitshock

import android.app.Application

class BitshockApplication : Application() {
    lateinit var engine: BitshockEngine
        private set

    override fun onCreate() {
        super.onCreate()
        engine = BitshockEngine(this)
        engine.start()
    }
}
