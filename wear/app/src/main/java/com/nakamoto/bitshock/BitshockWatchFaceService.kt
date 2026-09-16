package com.nakamoto.bitshock

import android.view.SurfaceHolder
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.TapType
import androidx.wear.watchface.WatchFace
import androidx.wear.watchface.WatchFaceService
import androidx.wear.watchface.WatchFaceType
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository

class BitshockWatchFaceService : WatchFaceService() {

    private val engine: BitshockEngine
        get() = (application as BitshockApplication).engine

    override fun createComplicationSlotsManager(
        currentUserStyleRepository: CurrentUserStyleRepository,
    ): ComplicationSlotsManager {
        return ComplicationSlotsManager(emptyList(), currentUserStyleRepository)
    }

    override suspend fun createWatchFace(
        surfaceHolder: SurfaceHolder,
        watchState: WatchState,
        complicationSlotsManager: ComplicationSlotsManager,
        currentUserStyleRepository: CurrentUserStyleRepository,
    ): WatchFace {
        engine.start()

        val renderer = BitshockWatchFaceRenderer(
            engine = engine,
            surfaceHolder = surfaceHolder,
            watchState = watchState,
            complicationSlotsManager = complicationSlotsManager,
            currentUserStyleRepository = currentUserStyleRepository,
        )

        var lastTapMs = 0L

        return WatchFace(WatchFaceType.DIGITAL, renderer).setTapListener { tapType, tapEvent, _ ->
            if (tapType != TapType.UP) return@setTapListener
            val now = System.currentTimeMillis()
            if (now - lastTapMs < 350) {
                engine.refresh()
                lastTapMs = 0L
                return@setTapListener
            }
            lastTapMs = now
            val mid = renderer.lastBounds.width() / 2f
            if (tapEvent.xPos < mid) engine.prevPage() else engine.nextPage()
        }
    }
}
