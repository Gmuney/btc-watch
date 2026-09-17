package com.nakamoto.bitshock

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.WebSocket
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Process-wide live data + navigation state shared by the launcher app and the watch face.
 */
class BitshockEngine(
    @Suppress("unused") private val appContext: Context,
    private val repo: MempoolRepository = MempoolRepository(),
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(WatchUiState())
    val state: StateFlow<WatchUiState> = _state

    private val invalidators = CopyOnWriteArrayList<() -> Unit>()

    var currentPage: Int = 0
        private set

    private var lastHeight: Int? = null
    private var socket: WebSocket? = null
    private var pollJob: Job? = null
    private var started = false

    fun addInvalidator(invalidator: () -> Unit) {
        invalidators.add(invalidator)
    }

    fun removeInvalidator(invalidator: () -> Unit) {
        invalidators.remove(invalidator)
    }

    private fun notifyInvalidate() {
        invalidators.forEach { it.invoke() }
    }

    fun start() {
        if (started) return
        started = true
        refresh()
        listenForBlocks()
        pollJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(300_000) // Poll every 5 minutes to preserve battery life
                refreshQuiet()
            }
        }
    }

    fun refresh() {
        scope.launch(Dispatchers.IO) {
            _state.update { it.copy(loading = it.priceUsd == null, error = null) }
            notifyInvalidate()
            runCatching { repo.fetchLive() }
                .onSuccess { applySnapshot(it, announceNewBlock = true) }
                .onFailure { err ->
                    _state.update {
                        it.copy(loading = false, error = err.message ?: "Could not load live data")
                    }
                    notifyInvalidate()
                }
        }
    }

    fun simulateNewBlock() {
        val next = (lastHeight ?: 0) + 1
        lastHeight = next
        _state.update {
            it.copy(
                blockHeight = next,
                blockTimestampSec = System.currentTimeMillis() / 1000,
                celebratingHeight = next,
            )
        }
        notifyInvalidate()
        scope.launch {
            delay(1900)
            _state.update { current ->
                if (current.celebratingHeight == next) current.copy(celebratingHeight = null) else current
            }
            notifyInvalidate()
        }
    }

    fun nextPage() {
        currentPage = (currentPage + 1) % 4
        notifyInvalidate()
    }

    fun prevPage() {
        currentPage = (currentPage + 3) % 4
        notifyInvalidate()
    }

    private fun refreshQuiet() {
        runCatching { repo.fetchLive() }
            .onSuccess { applySnapshot(it, announceNewBlock = true) }
    }

    private fun listenForBlocks() {
        socket = repo.connectBlocks { height, timestamp ->
            scope.launch { onHeight(height, timestamp) }
        }
    }

    private fun applySnapshot(snap: LiveSnapshot, announceNewBlock: Boolean) {
        if (announceNewBlock) {
            onHeight(snap.blockHeight, snap.blockTimestampSec)
        } else {
            lastHeight = snap.blockHeight
        }
        _state.update {
            it.copy(
                loading = false,
                error = null,
                priceUsd = snap.priceUsd,
                blockHeight = snap.blockHeight,
                blockTimestampSec = snap.blockTimestampSec,
                mempoolCount = snap.mempoolCount,
                mempoolVsize = snap.mempoolVsize,
                mempoolFeesSats = snap.mempoolFeesSats,
                feeFast = snap.feeFast,
                feeHalfHour = snap.feeHalfHour,
                feeHour = snap.feeHour,
                feeEconomy = snap.feeEconomy,
                lastUpdatedMs = System.currentTimeMillis(),
            )
        }
        notifyInvalidate()
    }

    private fun onHeight(height: Int, timestamp: Long?) {
        val prev = lastHeight
        lastHeight = height
        val isNew = prev != null && height > prev
        _state.update {
            it.copy(
                blockHeight = height,
                blockTimestampSec = timestamp ?: it.blockTimestampSec,
                celebratingHeight = if (isNew) height else it.celebratingHeight,
            )
        }
        notifyInvalidate()
        if (isNew) {
            scope.launch {
                delay(1900)
                _state.update { current ->
                    if (current.celebratingHeight == height) current.copy(celebratingHeight = null) else current
                }
                notifyInvalidate()
            }
        }
    }
}
