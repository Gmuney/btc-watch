package com.nakamoto.bitshock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text

private val Screen = Color(0xFF0B0B0F)
private val TextMain = Color(0xFFF5F5F7)
private val TextDim = Color(0xFF8E8E93)
private val Accent = Color(0xFFF7931A)

private val screens = listOf("Price", "Block", "Mempool", "Fees")

@Composable
fun BitshockWatch(
    state: WatchUiState,
    onRefresh: () -> Unit,
    onSimulateBlock: () -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { screens.size })
    val celebrating = state.celebratingHeight != null
    val flash by animateFloatAsState(
        targetValue = if (celebrating) 1f else 0f,
        animationSpec = tween(if (celebrating) 180 else 700),
        label = "flash",
    )
    val tick by animateFloatAsState(
        targetValue = if (celebrating) 1.12f else 1f,
        animationSpec = tween(350),
        label = "tick",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Screen)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onRefresh() },
                    onLongPress = { onSimulateBlock() },
                )
            },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                0 -> PricePage(state)
                1 -> BlockPage(state, tick)
                2 -> MempoolPage(state)
                3 -> FeesPage(state)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            screens.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 7.dp else 5.dp)
                        .background(
                            if (pagerState.currentPage == index) Accent else TextDim,
                            CircleShape,
                        ),
                )
            }
        }

        if (flash > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Accent.copy(alpha = 0.22f * flash)),
            )
        }

        if (state.celebratingHeight != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .scale(tick)
                    .background(Color(0xE0140C04), CircleShape)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("NEW BLOCK", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    BitshockFormatting.formatCount(state.celebratingHeight),
                    color = TextMain,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun PricePage(state: WatchUiState) {
    MetricScreen(
        label = "BTC / USD",
        value = state.priceUsd?.let { BitshockFormatting.formatPrice(it) }
            ?: BitshockFormatting.loadingOrDash(state),
        sub = when {
            state.error != null -> state.error
            state.priceUsd != null -> "Live from mempool.space"
            else -> "Swipe for more"
        },
    )
}

@Composable
private fun BlockPage(state: WatchUiState, tick: Float) {
    MetricScreen(
        label = "Block Height",
        value = state.blockHeight?.let { BitshockFormatting.formatCount(it) }
            ?: BitshockFormatting.loadingOrDash(state),
        sub = state.blockTimestampSec?.let { "Mined ${BitshockFormatting.timeAgo(it)}" } ?: "Waiting for tip",
        valueScale = tick,
    )
}

@Composable
private fun MempoolPage(state: WatchUiState) {
    MetricScreen(
        label = "Mempool",
        value = state.mempoolCount?.let { BitshockFormatting.formatCount(it) }
            ?: BitshockFormatting.loadingOrDash(state),
        sub = "pending txs",
        extra = listOf(
            "Size" to (state.mempoolVsize?.let { BitshockFormatting.formatVsize(it) } ?: "—"),
            "Total fees" to (state.mempoolFeesSats?.let { BitshockFormatting.formatBtc(it) } ?: "—"),
        ),
    )
}

@Composable
private fun FeesPage(state: WatchUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Fee Rates", color = Accent, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        FeeRow("Fast", state.feeFast)
        FeeRow("30 min", state.feeHalfHour)
        FeeRow("1 hour", state.feeHour)
        FeeRow("Economy", state.feeEconomy)
    }
}

@Composable
private fun FeeRow(label: String, value: Int?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Accent, fontSize = 12.sp)
        Text(
            value?.let { "$it sat/vB" } ?: "—",
            color = TextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun MetricScreen(
    label: String,
    value: String,
    sub: String,
    extra: List<Pair<String, String>> = emptyList(),
    valueScale: Float = 1f,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(label, color = Accent, fontSize = 12.sp, textAlign = TextAlign.Center)
        Text(
            value,
            color = TextMain,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.scale(valueScale),
        )
        Text(sub, color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center)
        extra.forEach { (k, v) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 10.dp, end = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(k, color = TextDim, fontSize = 11.sp)
                Text(v, color = TextMain, fontSize = 11.sp)
            }
        }
    }
}

