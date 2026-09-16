#!/usr/bin/env python3
"""Serve the demo and proxy live mempool.space data (avoids browser CORS / mixed-origin failures)."""

from __future__ import annotations

import json
import threading
import time
import urllib.error
import urllib.request
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent
UPSTREAM = "https://mempool.space/api"
CACHE_TTL_SEC = 8
FETCH_TIMEOUT_SEC = 12

_cache = {"at": 0.0, "payload": None, "error": None}
_lock = threading.Lock()


def _http_json(url: str):
    req = urllib.request.Request(
        url,
        headers={
            "User-Agent": "NakamotoBitshock/1.0 (+https://mempool.space)",
            "Accept": "application/json, text/plain;q=0.9",
        },
    )
    with urllib.request.urlopen(req, timeout=FETCH_TIMEOUT_SEC) as resp:
        raw = resp.read()
    text = raw.decode("utf-8").strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        # tip/hash is a bare hex string, not JSON
        return text.strip('"')


def fetch_spot_prices(fallback: dict) -> dict:
    """Prefer Coinbase/CoinGecko spot so the watch matches the public BTC price."""
    prices = dict(fallback or {})
    got_spot = False
    for fiat, url in (
        ("USD", "https://api.coinbase.com/v2/prices/BTC-USD/spot"),
        ("EUR", "https://api.coinbase.com/v2/prices/BTC-EUR/spot"),
        ("GBP", "https://api.coinbase.com/v2/prices/BTC-GBP/spot"),
    ):
        try:
            payload = _http_json(url)
            amount = float(payload["data"]["amount"])
            prices[fiat] = round(amount)
            got_spot = True
        except Exception:
            continue
    if not got_spot:
        try:
            cg = _http_json(
                "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd,eur,gbp"
            )
            btc = cg.get("bitcoin") or {}
            mapping = {"usd": "USD", "eur": "EUR", "gbp": "GBP"}
            for key, fiat in mapping.items():
                if btc.get(key) is not None:
                    prices[fiat] = round(float(btc[key]))
                    got_spot = True
        except Exception:
            pass
    prices["source"] = "spot" if got_spot else "mempool.space"
    return prices


def fetch_live_snapshot() -> dict:
    prices, block_height, mempool, fees, block_hash = (
        _http_json(f"{UPSTREAM}/v1/prices"),
        _http_json(f"{UPSTREAM}/blocks/tip/height"),
        _http_json(f"{UPSTREAM}/mempool"),
        _http_json(f"{UPSTREAM}/v1/fees/recommended"),
        _http_json(f"{UPSTREAM}/blocks/tip/hash"),
    )
    block = _http_json(f"{UPSTREAM}/block/{block_hash}")
    prices = fetch_spot_prices(prices)
    return {
        "prices": prices,
        "blockHeight": block_height,
        "blockHash": block_hash,
        "block": block,
        "mempool": mempool,
        "fees": fees,
        "fetchedAt": int(time.time()),
        "source": prices.get("source", "mempool.space"),
    }


def get_live_snapshot() -> dict:
    now = time.time()
    with _lock:
        if _cache["payload"] and (now - _cache["at"]) < CACHE_TTL_SEC:
            return _cache["payload"]

    snapshot = fetch_live_snapshot()
    with _lock:
        _cache["at"] = time.time()
        _cache["payload"] = snapshot
        _cache["error"] = None
    return snapshot


class DemoHandler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(ROOT), **kwargs)

    def log_message(self, fmt, *args):
        print(f"[{self.log_date_time_string()}] {fmt % args}")

    def _send_json(self, status: int, payload: dict | list | int | str):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        path = self.path.split("?", 1)[0]
        if path in ("/live", "/api/live"):
            try:
                self._send_json(200, get_live_snapshot())
            except Exception as err:
                self._send_json(502, {"error": "upstream_failed", "detail": str(err)})
            return

        if path.startswith("/api/"):
            upstream = f"{UPSTREAM}/{path[len('/api/'):]}"
            try:
                self._send_json(200, _http_json(upstream))
            except urllib.error.HTTPError as err:
                self._send_json(err.code, {"error": "upstream_http", "detail": err.reason})
            except Exception as err:
                self._send_json(502, {"error": "upstream_failed", "detail": str(err)})
            return

        super().do_GET()


def main():
    server = ThreadingHTTPServer(("0.0.0.0", 8080), DemoHandler)
    print("Nakamoto Bitshock demo + live proxy on http://0.0.0.0:8080/")
    server.serve_forever()


if __name__ == "__main__":
    main()
