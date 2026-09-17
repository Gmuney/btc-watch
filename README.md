# Nakamoto Bitshock

Live Bitcoin dashboard for Wear OS — app and watch face with data from [mempool.space](https://mempool.space).

## Compatibility

| | |
|---|---|
| **Watch (recommended)** | **Samsung Galaxy Watch 4 or newer** (4, 5, 6, Ultra, etc.) |
| **Tested device** | Galaxy Watch 4 **44mm** (450×450 round) |
| **Wear OS on watch** | **Wear OS 3 or newer** (Android **API 30+** on the watch) |
| **Built with (SDK)** | **compileSdk / targetSdk 35** (Wear OS 5 SDK in Android Studio), **minSdk 30** |
| **Tooling** | Android Gradle Plugin **8.7.3**, Kotlin **2.0.21** |

Other **Wear OS 3+** watches may work via sideload but are **not tested**. Very old Tizen-only Samsung watches (pre–Watch 4) are **not** supported.

## Install on your watch

**[INSTALL.md](INSTALL.md)** — clone, Android Studio, pair watch, Run, set the face.

- **App:** swipe screens for price, block, mempool, fees; block celebration
- **Watch face:** same live data on the home screen — [wear/MAIN_VIEW.md](wear/MAIN_VIEW.md)

Package: `com.nakamoto.bitshock`

## Data

Public mempool.space APIs (no API key). The watch needs network access for live values.

## License

Private / all rights reserved unless you add a license file.
