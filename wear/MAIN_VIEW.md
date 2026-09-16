# Nakamoto Bitshock — live dashboard as your main watch view

Watch Face Studio cannot run your mempool API or swipe dashboard. The **`wear/`** project now ships **the same live data** as a **selectable watch face** (code-based face, sideloaded with Android Studio).

You can close Watch Face Studio for this path.

## What you get

- **Home screen face** with live BTC price, block, mempool, fees (same four views as the demo)
- **Orange orbit dots**, block **NEW BLOCK** flash, 30s refresh + block WebSocket
- **Tap left / right** on the face to change page (like swiping in the app)
- **Double-tap** to refresh now
- **Launcher app** still installed — full Compose UI, double-tap refresh, long-press simulate block (for testing)

## Install (Galaxy Watch 4, wireless debugging)

1. Copy the `wear/` folder to your PC.
2. Open **`wear/`** in Android Studio (install Wear OS SDK if prompted).
3. On the watch: **Settings → About → Software** tap build number → **Developer options** → **ADB debugging** + **Wireless debugging** → pair/connect (note IP and port).
4. Terminal: `adb connect WATCH_IP:PORT`
5. **Run** the `app` configuration on the watch.

## Set as main view

1. Long-press the current watch face → **Browse faces** (or swipe to face picker).
2. Choose **Nakamoto Bitshock** (same name as the app).
3. Confirm — this is now what you see when you wake the watch.

If the face does not appear in the list, reboot the watch once after install.

## Battery (honest note)

Live network + animation on the **always-visible face** uses more battery than a static clock. Ambient / always-on shows **time + price only** with no orbit animation. For all-day wear, consider using the face during the day and a minimal Samsung face at night until we add a phone companion for background alerts.

## Watch Face Studio

Use WFS only if you want a **second**, static decorative face. It cannot replace this dashboard for live data.

## Technical note

This uses the AndroidX **WatchFaceService** API (legacy but still sideloadable on Galaxy Watch 4). Google’s newer **Watch Face Format** (XML) does not support this kind of live API dashboard; that’s why the implementation lives in Kotlin in this repo.
