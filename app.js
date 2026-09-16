const API_BASE = '/api';
const LIVE_ENDPOINT = '/live';

const SCREEN_NAMES = ['Price', 'Block', 'Mempool', 'Fees'];
const TOTAL_SCREENS = SCREEN_NAMES.length;

const ANIM_DURATION_MS = 1900;
let refreshSeq = 0;

const state = {
  currentScreen: 0,
  currency: 'USD',
  refreshInterval: 30,
  loading: false,
  lastPrice: null,
  lastBlockHeight: null,
  blockAnimEnabled: true,
  blockAnimStyle: 'combo',
  animatingBlock: false,
  faceMotion: 'off',
  faceImageUrl: null,
  refreshTimer: null,
  blockWs: null,
  touchStartX: 0,
  touchStartY: 0,
};

const els = {
  screens: document.getElementById('screens'),
  pageIndicator: document.getElementById('page-indicator'),
  screenName: document.getElementById('screen-name'),
  statusTime: document.getElementById('status-time'),
  statusDot: document.getElementById('status-dot'),
  lastUpdated: document.getElementById('last-updated'),
  errorBanner: document.getElementById('error-banner'),
  errorText: document.getElementById('error-text'),
  loadingOverlay: document.getElementById('loading-overlay'),
  btcPrice: document.getElementById('btc-price'),
  btcChange: document.getElementById('btc-change'),
  blockHeight: document.getElementById('block-height'),
  blockTime: document.getElementById('block-time'),
  mempoolCount: document.getElementById('mempool-count'),
  mempoolSize: document.getElementById('mempool-size'),
  mempoolFees: document.getElementById('mempool-fees'),
  feeFast: document.getElementById('fee-fast'),
  feeHalf: document.getElementById('fee-half'),
  feeHour: document.getElementById('fee-hour'),
  feeEcon: document.getElementById('fee-econ'),
  watchBezel: document.getElementById('watch-bezel'),
  watchScreen: document.getElementById('watch-screen'),
  blockFx: document.getElementById('block-fx'),
  blockParticles: document.getElementById('block-particles'),
  toastHeight: document.getElementById('toast-height'),
  faceBg: document.getElementById('face-bg'),
  faceBgImage: document.getElementById('face-bg-image'),
};

function parseHexColor(hex) {
  const raw = String(hex || '').replace('#', '').trim();
  const full = raw.length === 3
    ? raw.split('').map((c) => c + c).join('')
    : raw;
  if (!/^[0-9a-fA-F]{6}$/.test(full)) return null;
  return {
    r: parseInt(full.slice(0, 2), 16),
    g: parseInt(full.slice(2, 4), 16),
    b: parseInt(full.slice(4, 6), 16),
    hex: `#${full.toLowerCase()}`,
  };
}

function setFaceAccent(hex) {
  const color = parseHexColor(hex);
  if (!color || !els.faceBg) return;
  els.faceBg.style.setProperty('--face-rgb', `${color.r}, ${color.g}, ${color.b}`);
  els.faceBg.style.setProperty('--face-accent', color.hex);
  const picker = document.getElementById('face-color-picker');
  if (picker && picker.value !== color.hex) picker.value = color.hex;
}

function mixDimRgb(r, g, b, amount = 0.55) {
  // Blend toward mid-gray so secondary captions stay readable
  const blend = (c) => Math.round(c * amount + 142 * (1 - amount));
  return `${blend(r)}, ${blend(g)}, ${blend(b)}`;
}

function setPrimaryColor(hex) {
  const color = parseHexColor(hex);
  if (!color || !els.watchScreen) return;
  const rgb = `${color.r}, ${color.g}, ${color.b}`;
  els.watchScreen.style.setProperty('--text', color.hex);
  els.watchScreen.style.setProperty('--text-rgb', rgb);
  els.watchScreen.style.setProperty('--text-dim', `rgb(${mixDimRgb(color.r, color.g, color.b)})`);
  const picker = document.getElementById('primary-color-picker');
  if (picker && picker.value !== color.hex) picker.value = color.hex;
}

function setSecondaryColor(hex) {
  const color = parseHexColor(hex);
  if (!color || !els.watchScreen) return;
  const rgb = `${color.r}, ${color.g}, ${color.b}`;
  els.watchScreen.style.setProperty('--accent', color.hex);
  els.watchScreen.style.setProperty('--accent-rgb', rgb);
  els.watchScreen.style.setProperty('--ui-time', color.hex);
  // Bezel flash / glow sits outside .watch-screen
  if (els.watchBezel) {
    els.watchBezel.style.setProperty('--accent', color.hex);
    els.watchBezel.style.setProperty('--accent-rgb', rgb);
  }
  const picker = document.getElementById('ui-color-picker');
  if (picker && picker.value !== color.hex) picker.value = color.hex;
}

function formatPrice(value, currency) {
  const symbols = { USD: '$', EUR: '€', GBP: '£' };
  const sym = symbols[currency] || '$';
  if (value >= 100000) {
    return `${sym}${(value / 1000).toFixed(1)}k`;
  }
  return `${sym}${value.toLocaleString(undefined, { maximumFractionDigits: 0 })}`;
}

function formatNumber(n) {
  return n.toLocaleString();
}

function formatVsize(vbytes) {
  if (vbytes >= 1_000_000) return `${(vbytes / 1_000_000).toFixed(2)} MB`;
  if (vbytes >= 1000) return `${(vbytes / 1000).toFixed(1)} kB`;
  return `${vbytes} vB`;
}

function formatBtc(sats) {
  const btc = sats / 1e8;
  if (btc >= 1) return `${btc.toFixed(2)} BTC`;
  if (btc >= 0.001) return `${btc.toFixed(4)} BTC`;
  return `${btc.toFixed(6)} BTC`;
}

function timeAgo(isoOrTs) {
  const ts = typeof isoOrTs === 'number' ? isoOrTs * 1000 : new Date(isoOrTs).getTime();
  const diff = Date.now() - ts;
  const mins = Math.floor(diff / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins}m ago`;
  const hrs = Math.floor(mins / 60);
  return `${hrs}h ${mins % 60}m ago`;
}

function updateClock() {
  const now = new Date();
  els.statusTime.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function goToScreen(index) {
  state.currentScreen = ((index % TOTAL_SCREENS) + TOTAL_SCREENS) % TOTAL_SCREENS;
  els.screens.style.transform = `translateX(-${state.currentScreen * 100}%)`;

  const dots = els.pageIndicator.querySelectorAll('.dot');
  dots.forEach((dot, i) => dot.classList.toggle('active', i === state.currentScreen));
  els.screenName.textContent = SCREEN_NAMES[state.currentScreen];
}

function showError(msg) {
  els.errorText.textContent = msg;
  els.errorBanner.classList.remove('hidden');
  els.statusDot.className = 'status-dot error';
}

function hideError() {
  els.errorBanner.classList.add('hidden');
}

function setLoading(on) {
  state.loading = on;
  els.loadingOverlay.classList.toggle('hidden', !on);
}

async function fetchJson(url) {
  const res = await fetch(url, { cache: 'no-store' });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  return res.json();
}

async function fetchLiveData() {
  let snap;
  try {
    snap = await fetchJson(LIVE_ENDPOINT);
  } catch (_) {
    // Fallback if the combined proxy is down but /api still works
    const [prices, blockHeight, mempool, fees, blockHash] = await Promise.all([
      fetchJson(`${API_BASE}/v1/prices`),
      fetchJson(`${API_BASE}/blocks/tip/height`),
      fetchJson(`${API_BASE}/mempool`),
      fetchJson(`${API_BASE}/v1/fees/recommended`),
      fetchJson(`${API_BASE}/blocks/tip/hash`),
    ]);
    const block = await fetchJson(`${API_BASE}/block/${blockHash}`);
    snap = { prices, blockHeight, mempool, fees, block };
  }

  if (snap && snap.error) throw new Error(snap.detail || 'Live snapshot error');

  const prices = snap.prices || {};
  const price = prices[state.currency] ?? prices.USD;
  if (price == null) throw new Error('Live price missing');

  return {
    price,
    prevPrice: state.lastPrice,
    blockHeight: snap.blockHeight,
    blockTimestamp: snap.block && snap.block.timestamp,
    mempool: snap.mempool,
    fees: snap.fees,
  };
}

function spawnParticles(count = 14) {
  els.blockParticles.innerHTML = '';
  for (let i = 0; i < count; i++) {
    const spark = document.createElement('span');
    spark.className = 'spark';
    const angle = (Math.PI * 2 * i) / count + Math.random() * 0.4;
    const dist = 48 + Math.random() * 70;
    spark.style.setProperty('--dx', `${Math.cos(angle) * dist}px`);
    spark.style.setProperty('--dy', `${Math.sin(angle) * dist}px`);
    spark.style.animationDelay = `${Math.random() * 0.12}s`;
    els.blockParticles.appendChild(spark);
  }
}

function playBlockAnimation(height) {
  if (!state.blockAnimEnabled || state.animatingBlock) return;
  state.animatingBlock = true;

  const style = state.blockAnimStyle;
  const fx = els.blockFx;
  const toastEl = els.toastHeight;
  const heightEl = els.blockHeight;

  toastEl.textContent = formatNumber(height);

  // Overlay sits above every face — stay on the current screen
  fx.className = 'block-fx active show-toast';
  if (style === 'combo') {
    fx.classList.add('anim-combo');
  } else if (style === 'ripple') {
    fx.classList.add('anim-ripple');
  } else if (style === 'particles') {
    fx.classList.add('anim-particles');
  }

  if (style === 'combo' || style === 'particles') {
    spawnParticles(style === 'combo' ? 16 : 18);
  } else {
    els.blockParticles.innerHTML = '';
  }

  if (style === 'combo' || style === 'flash') {
    els.watchScreen.classList.add('flash-glow');
    els.watchBezel.classList.add('flash-glow');
  }

  // Tick the overlay height so it's visible on Price / Mempool / Fees too
  if (style === 'combo' || style === 'tick') {
    toastEl.classList.remove('tick-pop');
    heightEl.classList.remove('tick-pop');
    void toastEl.offsetWidth;
    toastEl.classList.add('tick-pop');
    heightEl.classList.add('tick-pop');
  }

  // Stand-in for Wear OS haptic
  if (navigator.vibrate) {
    try { navigator.vibrate(40); } catch (_) { /* ignore */ }
  }

  window.setTimeout(() => {
    fx.className = 'block-fx';
    els.watchScreen.classList.remove('flash-glow');
    els.watchBezel.classList.remove('flash-glow');
    toastEl.classList.remove('tick-pop');
    heightEl.classList.remove('tick-pop');
    els.blockParticles.innerHTML = '';
    state.animatingBlock = false;
  }, ANIM_DURATION_MS);
}

function onNewBlock(height, timestamp) {
  const prev = state.lastBlockHeight;
  state.lastBlockHeight = height;
  els.blockHeight.textContent = formatNumber(height);
  if (timestamp != null) {
    els.blockTime.textContent = `Mined ${timeAgo(timestamp)}`;
  } else {
    els.blockTime.textContent = 'Mined just now';
  }

  // First observation is baseline — don't celebrate boot as a new block
  if (prev != null && height > prev) {
    playBlockAnimation(height);
  }
}

function simulateNewBlock() {
  const base = state.lastBlockHeight ?? 872100;
  const next = base + 1;
  onNewBlock(next, Math.floor(Date.now() / 1000));
}

function connectBlockSocket() {
  if (state.blockWs) return;

  try {
    const ws = new WebSocket('wss://mempool.space/api/v1/ws');
    state.blockWs = ws;

    ws.addEventListener('open', () => {
      ws.send(JSON.stringify({ action: 'want', data: ['blocks'] }));
    });

    ws.addEventListener('message', (event) => {
      try {
        const msg = JSON.parse(event.data);
        const blocks = msg.blocks || (msg.block ? [msg.block] : null);
        if (!blocks || !blocks.length) return;
        const tip = blocks[blocks.length - 1];
        const height = tip.height ?? tip;
        if (typeof height === 'number') {
          onNewBlock(height, tip.timestamp);
        }
      } catch (_) {
        /* ignore malformed frames */
      }
    });

    ws.addEventListener('close', () => {
      state.blockWs = null;
      window.setTimeout(connectBlockSocket, 8000);
    });

    ws.addEventListener('error', () => {
      try { ws.close(); } catch (_) { /* ignore */ }
    });
  } catch (err) {
    console.warn('Block websocket unavailable:', err);
  }
}

function renderData(data) {
  const { price, prevPrice, blockHeight, blockTimestamp, mempool, fees } = data;

  els.btcPrice.textContent = formatPrice(price, state.currency);

  if (prevPrice != null) {
    const change = price - prevPrice;
    const pct = ((change / prevPrice) * 100).toFixed(2);
    const sign = change >= 0 ? '+' : '';
    els.btcChange.textContent = `${sign}${pct}% since last refresh`;
    els.btcChange.className = 'price-change ' + (change > 0 ? 'up' : change < 0 ? 'down' : 'neutral');
  } else {
    els.btcChange.textContent = 'Live from mempool.space';
    els.btcChange.className = 'price-change neutral';
  }

  state.lastPrice = price;

  onNewBlock(blockHeight, blockTimestamp);

  els.mempoolCount.textContent = formatNumber(mempool.count);
  els.mempoolSize.textContent = formatVsize(mempool.vsize);
  els.mempoolFees.textContent = formatBtc(mempool.total_fee);

  els.feeFast.textContent = fees.fastestFee;
  els.feeHalf.textContent = fees.halfHourFee;
  els.feeHour.textContent = fees.hourFee;
  els.feeEcon.textContent = fees.economyFee;

  const now = new Date();
  els.lastUpdated.textContent = `Updated ${now.toLocaleTimeString()}`;
  els.statusDot.className = 'status-dot live';
}

async function refreshData() {
  const seq = ++refreshSeq;
  setLoading(true);
  hideError();

  try {
    const live = await fetchLiveData();
    if (seq !== refreshSeq) return;
    renderData(live);
  } catch (err) {
    if (seq !== refreshSeq) return;
    console.error('Fetch error:', err);
    showError('Could not load live Bitcoin price — tap Refresh now');
    if (state.lastPrice == null) {
      els.btcPrice.textContent = '—';
    }
  } finally {
    if (seq === refreshSeq) setLoading(false);
  }
}

function setupRefreshTimer() {
  if (state.refreshTimer) clearInterval(state.refreshTimer);
  state.refreshTimer = setInterval(refreshData, state.refreshInterval * 1000);
}

function setupSwipe() {
  const screen = els.watchScreen;
  let swiping = false;

  screen.addEventListener('touchstart', (e) => {
    state.touchStartX = e.touches[0].clientX;
    state.touchStartY = e.touches[0].clientY;
    swiping = false;
  }, { passive: true });

  screen.addEventListener('touchmove', (e) => {
    const dx = e.touches[0].clientX - state.touchStartX;
    const dy = e.touches[0].clientY - state.touchStartY;
    if (Math.abs(dx) > Math.abs(dy) && Math.abs(dx) > 10) {
      swiping = true;
    }
  }, { passive: true });

  screen.addEventListener('touchend', (e) => {
    if (!swiping) return;
    const dx = e.changedTouches[0].clientX - state.touchStartX;
    if (Math.abs(dx) > 40) {
      goToScreen(state.currentScreen + (dx < 0 ? 1 : -1));
    }
  }, { passive: true });

  let mouseDown = false;
  let mouseStartX = 0;

  screen.addEventListener('mousedown', (e) => {
    mouseDown = true;
    mouseStartX = e.clientX;
  });

  screen.addEventListener('mouseup', (e) => {
    if (!mouseDown) return;
    mouseDown = false;
    const dx = e.clientX - mouseStartX;
    if (Math.abs(dx) > 40) {
      goToScreen(state.currentScreen + (dx < 0 ? 1 : -1));
    }
  });

  screen.addEventListener('mouseleave', () => { mouseDown = false; });
}

function init() {
  document.getElementById('watch-size').addEventListener('change', (e) => {
    els.watchBezel.className = 'watch-bezel size-' + e.target.value;
  });

  document.getElementById('currency').addEventListener('change', (e) => {
    state.currency = e.target.value;
    state.lastPrice = null;
    refreshData();
  });

  document.getElementById('refresh-interval').addEventListener('change', (e) => {
    state.refreshInterval = parseInt(e.target.value, 10);
    setupRefreshTimer();
  });

  document.getElementById('block-anim-enabled').addEventListener('change', (e) => {
    state.blockAnimEnabled = e.target.value === 'on';
  });

  document.getElementById('block-anim').addEventListener('change', (e) => {
    state.blockAnimStyle = e.target.value;
  });

  document.getElementById('face-motion').addEventListener('change', (e) => {
    state.faceMotion = e.target.value;
    els.faceBg.dataset.motion = state.faceMotion;
  });

  const faceColorSelect = document.getElementById('face-color');
  const faceColorPicker = document.getElementById('face-color-picker');
  const primaryColorSelect = document.getElementById('primary-color');
  const primaryColorPicker = document.getElementById('primary-color-picker');
  const secondaryColorSelect = document.getElementById('ui-color');
  const secondaryColorPicker = document.getElementById('ui-color-picker');

  faceColorSelect.addEventListener('change', (e) => {
    if (e.target.value === 'custom') {
      setFaceAccent(faceColorPicker.value);
      return;
    }
    setFaceAccent(e.target.value);
  });

  faceColorPicker.addEventListener('input', (e) => {
    faceColorSelect.value = 'custom';
    setFaceAccent(e.target.value);
  });

  primaryColorSelect.addEventListener('change', (e) => {
    if (e.target.value === 'custom') {
      setPrimaryColor(primaryColorPicker.value);
      return;
    }
    setPrimaryColor(e.target.value);
  });

  primaryColorPicker.addEventListener('input', (e) => {
    primaryColorSelect.value = 'custom';
    setPrimaryColor(e.target.value);
  });

  secondaryColorSelect.addEventListener('change', (e) => {
    if (e.target.value === 'custom') {
      setSecondaryColor(secondaryColorPicker.value);
      return;
    }
    setSecondaryColor(e.target.value);
  });

  secondaryColorPicker.addEventListener('input', (e) => {
    secondaryColorSelect.value = 'custom';
    setSecondaryColor(e.target.value);
  });

  setFaceAccent(
    faceColorSelect.value === 'custom' ? faceColorPicker.value : faceColorSelect.value
  );
  setPrimaryColor(
    primaryColorSelect.value === 'custom' ? primaryColorPicker.value : primaryColorSelect.value
  );
  setSecondaryColor(
    secondaryColorSelect.value === 'custom' ? secondaryColorPicker.value : secondaryColorSelect.value
  );

  document.getElementById('face-scrim').addEventListener('change', (e) => {
    els.faceBg.dataset.scrim = e.target.value;
  });

  document.getElementById('face-image').addEventListener('change', (e) => {
    const file = e.target.files && e.target.files[0];
    if (!file) return;
    if (state.faceImageUrl) URL.revokeObjectURL(state.faceImageUrl);
    state.faceImageUrl = URL.createObjectURL(file);
    els.faceBgImage.style.backgroundImage = `url("${state.faceImageUrl}")`;
    els.faceBg.classList.add('has-image');
  });

  document.getElementById('btn-clear-bg').addEventListener('click', () => {
    if (state.faceImageUrl) {
      URL.revokeObjectURL(state.faceImageUrl);
      state.faceImageUrl = null;
    }
    els.faceBgImage.style.backgroundImage = '';
    els.faceBg.classList.remove('has-image');
    document.getElementById('face-image').value = '';
  });

  document.getElementById('btn-refresh').addEventListener('click', refreshData);
  document.getElementById('crown-btn').addEventListener('click', refreshData);
  document.getElementById('btn-simulate-block').addEventListener('click', simulateNewBlock);

  document.getElementById('nav-prev').addEventListener('click', () => goToScreen(state.currentScreen - 1));
  document.getElementById('nav-next').addEventListener('click', () => goToScreen(state.currentScreen + 1));

  document.addEventListener('keydown', (e) => {
    if (e.key === 'ArrowLeft') goToScreen(state.currentScreen - 1);
    if (e.key === 'ArrowRight') goToScreen(state.currentScreen + 1);
    if (e.key === 'r' || e.key === 'R') refreshData();
    if (e.key === 'b' || e.key === 'B') simulateNewBlock();
  });

  setupSwipe();
  updateClock();
  setInterval(updateClock, 10000);

  els.watchBezel.classList.add('size-44');
  goToScreen(0);
  refreshData();
  setupRefreshTimer();
  connectBlockSocket();
}

init();
