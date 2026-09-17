# Nakamoto Bitshock — live dashboard as your main watch view

Code-based **watch face** + **launcher app** with live mempool data (sideload via Android Studio). See [../INSTALL.md](../INSTALL.md) for full steps.

## Compatibility

- **Samsung Galaxy Watch 4 or newer** (recommended; tested on Watch 4 44mm).
- **Wear OS 3+** on the watch (API 30+). Built with **target/compile SDK 35**.

## What you get

- **Home screen face** with live BTC price, block, mempool, fees (four pages)
- **Orange orbit dots**, **NEW BLOCK** flash, **5-minute** background refresh + block WebSocket
- **Tap left / right** on the face to change page
- **Double-tap** to refresh now
- **Launcher app** — full Compose UI, swipe pages, long-press simulate block (testing)

## Install

Follow [INSTALL.md](../INSTALL.md) (wireless debugging, Run **`app`**, set face in picker).

## Set as main view

1. Long-press the current watch face → **Browse faces**
2. Choose **Nakamoto Bitshock**
3. Wake the watch to see the live dashboard face

If the face does not appear, reboot the watch once after install.

## Battery

Live network + animation on the always-visible face uses more battery than a static clock. Ambient / always-on shows **time + price** with no orbit animation.

## Technical note

Uses AndroidX **WatchFaceService** (legacy API, still sideloadable on Galaxy Watch 4+). Google’s **Watch Face Format** (XML) does not support this kind of live API dashboard.
