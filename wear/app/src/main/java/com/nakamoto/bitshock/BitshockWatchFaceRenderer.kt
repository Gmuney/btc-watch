package com.nakamoto.bitshock

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.ComplicationSlotsManager
import androidx.wear.watchface.DrawMode
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class BitshockWatchFaceRenderer(
    private val engine: BitshockEngine,
    surfaceHolder: SurfaceHolder,
    watchState: WatchState,
    @Suppress("unused") complicationSlotsManager: ComplicationSlotsManager,
    currentUserStyleRepository: CurrentUserStyleRepository,
) : Renderer.CanvasRenderer(
    surfaceHolder = surfaceHolder,
    currentUserStyleRepository = currentUserStyleRepository,
    watchState = watchState,
    canvasType = CanvasType.HARDWARE,
    interactiveDrawModeUpdateDelayMillis = 16L,
) {
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B0B0F.toInt() }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF7931A.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val mainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF5F5F7.toInt()
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8E8E93.toInt()
        textAlign = Paint.Align.CENTER
    }
    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x38F7931A }
    private val dotActive = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF7931A.toInt() }
    private val dotIdle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF8E8E93.toInt() }
    private val orbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB3F7931A.toInt() }

    private val invalidator = { invalidate() }

    /** Updated each frame; used for left/right page taps. */
    var lastBounds: Rect = Rect()

    init {
        engine.addInvalidator(invalidator)
    }

    override fun onDestroy() {
        engine.removeInvalidator(invalidator)
        super.onDestroy()
    }

    override fun renderHighlightLayer(canvas: Canvas, bounds: Rect, zonedDateTime: ZonedDateTime) {
        // No highlight layer rendering needed
    }

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
    ) {
        lastBounds.set(bounds)
        val cx = bounds.exactCenterX()
        val cy = bounds.exactCenterY()
        val scale = min(bounds.width(), bounds.height()) / 450f

        canvas.drawRect(bounds, bgPaint)

        val ambient = renderParameters.drawMode == DrawMode.AMBIENT
        if (!ambient) {
            drawOrbit(canvas, cx, cy, bounds.width() * 0.42f)
        }

        val state = engine.state.value
        val timeText = "%02d:%02d".format(zonedDateTime.hour, zonedDateTime.minute)
        accentPaint.textSize = 28f * scale
        canvas.drawText(timeText, cx, bounds.top + 52f * scale, accentPaint)

        if (ambient) {
            drawMetric(
                canvas, cx, cy + 10f * scale, scale,
                label = "BTC / USD",
                value = state.priceUsd?.let { BitshockFormatting.formatPrice(it) }
                    ?: BitshockFormatting.loadingOrDash(state),
                sub = if (state.error != null) "Tap to refresh" else "Live",
            )
        } else {
            when (engine.currentPage) {
                0 -> drawMetric(
                    canvas, cx, cy, scale,
                    label = "BTC / USD",
                    value = state.priceUsd?.let { BitshockFormatting.formatPrice(it) }
                        ?: BitshockFormatting.loadingOrDash(state),
                    sub = when {
                        state.error != null -> requireNotNull(state.error)
                        state.priceUsd != null -> "Live · swipe sides"
                        else -> "Loading…"
                    },
                )
                1 -> drawMetric(
                    canvas, cx, cy, scale,
                    label = "Block Height",
                    value = state.blockHeight?.let { BitshockFormatting.formatCount(it) }
                        ?: BitshockFormatting.loadingOrDash(state),
                    sub = state.blockTimestampSec?.let { "Mined ${BitshockFormatting.timeAgo(it)}" }
                        ?: "Waiting for tip",
                )
                2 -> drawMetric(
                    canvas, cx, cy, scale,
                    label = "Mempool",
                    value = state.mempoolCount?.let { BitshockFormatting.formatCount(it) }
                        ?: BitshockFormatting.loadingOrDash(state),
                    sub = "pending txs",
                    extra = listOf(
                        "Size" to (state.mempoolVsize?.let { BitshockFormatting.formatVsize(it) } ?: "—"),
                        "Fees" to (state.mempoolFeesSats?.let { BitshockFormatting.formatBtc(it) } ?: "—"),
                    ),
                )
                else -> drawFees(canvas, cx, cy, scale, state)
            }
            drawPageDots(canvas, cx, bounds.bottom - 28f * scale, scale, engine.currentPage)
        }

        val celebrating = state.celebratingHeight
        if (celebrating != null && !ambient) {
            canvas.drawRect(bounds, flashPaint)
            drawCelebration(canvas, cx, cy, scale, celebrating)
        }

        if (!ambient) {
            invalidate()
        }
    }

    private fun drawOrbit(canvas: Canvas, cx: Float, cy: Float, radius: Float) {
        val t = System.currentTimeMillis() / 1000.0
        val dotR = 5f
        repeat(4) { i ->
            val angle = t * 0.9 + i * (Math.PI / 2)
            val x = cx + (radius * cos(angle)).toFloat()
            val y = cy + (radius * sin(angle)).toFloat()
            canvas.drawCircle(x, y, dotR, orbitPaint)
        }
    }

    private fun drawMetric(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        scale: Float,
        label: String,
        value: String,
        sub: String,
        extra: List<Pair<String, String>> = emptyList(),
    ) {
        accentPaint.textSize = 22f * scale
        canvas.drawText(label, cx, cy - 36f * scale, accentPaint)

        mainPaint.textSize = when {
            value.length > 12 -> 34f * scale
            value.length > 8 -> 40f * scale
            else -> 46f * scale
        }
        canvas.drawText(value, cx, cy + 8f * scale, mainPaint)

        dimPaint.textSize = 18f * scale
        canvas.drawText(sub.take(42), cx, cy + 38f * scale, dimPaint)

        var y = cy + 62f * scale
        dimPaint.textAlign = Paint.Align.LEFT
        mainPaint.textAlign = Paint.Align.RIGHT
        dimPaint.textSize = 16f * scale
        mainPaint.textSize = 16f * scale
        extra.forEach { (k, v) ->
            canvas.drawText(k, cx - 70f * scale, y, dimPaint)
            canvas.drawText(v, cx + 70f * scale, y, mainPaint)
            y += 22f * scale
        }
        dimPaint.textAlign = Paint.Align.CENTER
        mainPaint.textAlign = Paint.Align.CENTER
    }

    private fun drawFees(canvas: Canvas, cx: Float, cy: Float, scale: Float, state: WatchUiState) {
        accentPaint.textSize = 22f * scale
        canvas.drawText("Fee Rates", cx, cy - 70f * scale, accentPaint)
        val rows = listOf(
            "Fast" to state.feeFast,
            "30 min" to state.feeHalfHour,
            "1 hour" to state.feeHour,
            "Economy" to state.feeEconomy,
        )
        var y = cy - 38f * scale
        dimPaint.textAlign = Paint.Align.LEFT
        mainPaint.textAlign = Paint.Align.RIGHT
        dimPaint.textSize = 18f * scale
        mainPaint.textSize = 18f * scale
        rows.forEach { (label, sat) ->
            accentPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(label, cx - 78f * scale, y, accentPaint)
            mainPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(sat?.let { "$it sat/vB" } ?: "—", cx + 78f * scale, y, mainPaint)
            y += 26f * scale
        }
        dimPaint.textAlign = Paint.Align.CENTER
        mainPaint.textAlign = Paint.Align.CENTER
        accentPaint.textAlign = Paint.Align.CENTER
    }

    private fun drawPageDots(canvas: Canvas, cx: Float, y: Float, scale: Float, page: Int) {
        val spacing = 14f * scale
        repeat(4) { i ->
            val paint = if (i == page) dotActive else dotIdle
            val r = if (i == page) 5f * scale else 3.5f * scale
            canvas.drawCircle(cx - 1.5f * spacing + i * spacing, y, r, paint)
        }
    }

    private fun drawCelebration(canvas: Canvas, cx: Float, cy: Float, scale: Float, height: Int) {
        val rect = RectF(
            cx - 92f * scale,
            cy - 36f * scale,
            cx + 92f * scale,
            cy + 36f * scale,
        )
        val badge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xE0140C04.toInt() }
        canvas.drawRoundRect(rect, 24f * scale, 24f * scale, badge)
        accentPaint.textSize = 16f * scale
        canvas.drawText("NEW BLOCK", cx, cy - 8f * scale, accentPaint)
        mainPaint.textSize = 28f * scale
        canvas.drawText(BitshockFormatting.formatCount(height), cx, cy + 22f * scale, mainPaint)
    }
}
