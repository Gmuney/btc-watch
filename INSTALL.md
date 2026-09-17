# Install Nakamoto Bitshock on your watch

Sideload guide for the **Wear OS app + live watch face**. No Play Store required.

**Repo:** clone or download ZIP from GitHub, then follow steps below.

---

## Compatibility

- **Recommended:** **Samsung Galaxy Watch 4 or newer** (Galaxy Watch 4, 5, 6, Ultra, …).
- **Tested on:** Galaxy Watch 4 **44mm** (450×450 round display).
- **Wear OS version:** watch must run **Wear OS 3+** (**Android API 30+**). Galaxy Watch 4 shipped with Wear OS 3; newer models run Wear OS 4 or 5.
- **This project is built with:** **compileSdk / targetSdk 35** (install **Android API 35** platform in SDK Manager if Gradle asks), **minSdk 30**.

Other Wear OS 3+ devices may work; pre–Watch 4 Samsung (Tizen) watches cannot run this app.

---

## What you need

- A compatible watch (see **Compatibility** above)
- A **Windows or Mac PC** with [Android Studio](https://developer.android.com/studio)
- **Wi‑Fi** on watch and PC (same network for wireless install)
- Watch can reach the internet (live data from [mempool.space](https://mempool.space))

---

## 1. Get the project

**Option A — Git**

```bash
git clone https://github.com/Gmuney/btc-watch.git
cd btc-watch
```

**Option B — ZIP**

Download the repo as ZIP from GitHub, unzip to a folder like `btc-watch`.

Do **not** copy individual files into a blank project — open this repo as-is.

---

## 2. Open in Android Studio

1. Install Android Studio (Standard setup is fine).
2. **File → Open…**
3. Select the **`wear`** folder inside `btc-watch`  
   Example: `btc-watch/wear`
4. Click **Trust Project** if asked.
5. Wait for **Gradle Sync** to finish (first time may take 10–20 minutes).

If sync fails, use **Tools → SDK Manager** and install what the error suggests (often **Android API 35**).

Ignore optional banners like **AGP Upgrade Assistant** or **Gradle Daemon toolchain** — not required to run.

---

## 3. Enable developer mode on the watch

1. **Settings → About watch → Software**
2. Tap **Software version** (or **Build number**) **7 times** → developer mode on.
3. **Settings → Developer options**
4. Turn on **ADB debugging**
5. Turn on **Wireless debugging**
6. Open **Wireless debugging → Pair new device** and note **IP, port, and pairing code**

---

## 4. Connect the watch to Android Studio

1. In Android Studio: device menu → **Pair Devices Using Wi‑Fi** (or Device Manager → **+**).
2. Enter pairing details from the watch.
3. If the watch still does not appear, use **Terminal** in Studio:

   ```bash
   adb pair WATCH_IP:PAIRING_PORT
   adb connect WATCH_IP:DEBUG_PORT
   ```

   Use the **debug** IP/port shown on the watch’s Wireless debugging screen after pairing.

---

## 5. Install on the watch

1. Run configuration: **`app`**
2. Device: your **watch** (not the phone)
3. Click **Run** ▶
4. On the watch, accept **install** / **allow debugging**

When it succeeds, **Nakamoto Bitshock** appears in the watch app list.

---

## 6. Set the live dashboard as your watch face (optional)

1. **Long-press** the current watch face
2. **Browse faces** → choose **Nakamoto Bitshock**

**Controls on the face**

- **Tap left / right** — change page (price, block, mempool, fees)
- **Double-tap** — refresh live data

You can also open the **app** from the drawer for the full Compose UI (swipe pages, long-press test block in the app).

---

## Troubleshooting

| Problem | Try |
|--------|-----|
| No wireless debugging menu | Enable developer mode first (step 3). |
| Watch not in device list | Same Wi‑Fi; re-pair; `adb connect …` |
| Gradle sync errors | SDK Manager → install missing SDK; **File → Sync Project** |
| No live price | Watch needs Wi‑Fi/internet; double-tap face to refresh |
| Face not in picker | Reboot watch once after install |

More detail: [wear/MAIN_VIEW.md](wear/MAIN_VIEW.md)

---

## Notes

- **Sideload / dev build** — not distributed via Google Play in this repo.
- **Battery:** live network on the always-visible face uses more power than a static clock; background refresh is every 5 minutes (blocks still update via WebSocket when possible).
- **License:** see [README.md](README.md) — no public license file yet; ask the repo owner before redistributing.
