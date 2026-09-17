package com.nakamoto.bitshock

data class WatchUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val priceUsd: Double? = null,
    val blockHeight: Int? = null,
    val blockTimestampSec: Long? = null,
    val celebratingHeight: Int? = null,
    val mempoolCount: Int? = null,
    val mempoolVsize: Long? = null,
    val mempoolFeesSats: Long? = null,
    val feeFast: Int? = null,
    val feeHalfHour: Int? = null,
    val feeHour: Int? = null,
    val feeEconomy: Int? = null,
    val lastUpdatedMs: Long? = null,
)
