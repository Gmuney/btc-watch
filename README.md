# Nakamoto Bitshock

Live Bitcoin dashboard inspired by mempool.space — web demo plus a Wear OS app and watch face for Galaxy Watch (450×450).

## Web demo

- `index.html`, `app.js`, `styles.css` — browser UI (swipe faces, block pulse, face motion)
- `server.py` — local server; proxies `/live` and `/api/*` to mempool.space for CORS

```bash
python3 server.py
# open http://localhost:8080
```

## Wear OS (sideload)

**Start here:** [INSTALL.md](INSTALL.md) — step-by-step for Android Studio, watch pairing, Run, and setting the face.

- **App:** four swipe screens, live price / block / mempool / fees, block celebration
- **Watch face:** same live data on the home screen — see also [wear/MAIN_VIEW.md](wear/MAIN_VIEW.md)

Package: `com.nakamoto.bitshock`

## Data

Public APIs from [mempool.space](https://mempool.space) (no API key). Requires network on the watch for live values.

## License

Private / all rights reserved unless you add a license file.
