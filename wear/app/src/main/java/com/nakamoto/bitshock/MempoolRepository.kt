package com.nakamoto.bitshock

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class LiveSnapshot(
    val priceUsd: Double,
    val blockHeight: Int,
    val blockTimestampSec: Long?,
    val mempoolCount: Int,
    val mempoolVsize: Long,
    val mempoolFeesSats: Long,
    val feeFast: Int,
    val feeHalfHour: Int,
    val feeHour: Int,
    val feeEconomy: Int,
)

class MempoolRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    fun fetchLive(): LiveSnapshot {
        val prices = JSONObject(get("https://mempool.space/api/v1/prices"))
        val height = get("https://mempool.space/api/blocks/tip/height").trim().toInt()
        val hash = get("https://mempool.space/api/blocks/tip/hash").trim().trim('"')
        val block = JSONObject(get("https://mempool.space/api/block/$hash"))
        val mempool = JSONObject(get("https://mempool.space/api/mempool"))
        val fees = JSONObject(get("https://mempool.space/api/v1/fees/recommended"))

        return LiveSnapshot(
            priceUsd = prices.optDouble("USD"),
            blockHeight = height,
            blockTimestampSec = if (block.has("timestamp")) block.getLong("timestamp") else null,
            mempoolCount = mempool.optInt("count"),
            mempoolVsize = mempool.optLong("vsize"),
            mempoolFeesSats = mempool.optLong("total_fee"),
            feeFast = fees.optInt("fastestFee"),
            feeHalfHour = fees.optInt("halfHourFee"),
            feeHour = fees.optInt("hourFee"),
            feeEconomy = fees.optInt("economyFee"),
        )
    }

    fun connectBlocks(onHeight: (Int, Long?) -> Unit): WebSocket {
        val request = Request.Builder()
            .url("wss://mempool.space/api/v1/ws")
            .build()
        return client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                webSocket.send("""{"action":"want","data":["blocks"]}""")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = JSONObject(text)
                    val tip = when {
                        msg.has("block") -> msg.getJSONObject("block")
                        msg.has("blocks") -> {
                            val arr = msg.getJSONArray("blocks")
                            if (arr.length() == 0) return
                            arr.getJSONObject(arr.length() - 1)
                        }
                        else -> return
                    }
                    val height = tip.optInt("height", -1)
                    if (height > 0) {
                        val ts = if (tip.has("timestamp")) tip.optLong("timestamp") else null
                        onHeight(height, ts)
                    }
                } catch (_: Exception) {
                    // ignore malformed frames
                }
            }
        })
    }

    private fun get(url: String): String {
        val request = Request.Builder().url(url).header("User-Agent", "NakamotoBitshock/0.1").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("HTTP ${response.code} for $url")
            }
            return response.body?.string() ?: throw IllegalStateException("Empty body for $url")
        }
    }
}
