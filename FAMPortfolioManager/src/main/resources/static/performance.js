/* ============================================================
   performance.js — Performance page logic
   Depends on: app.js (for fallbackData, formatCurrency)
   ============================================================ */

// ── Constants ─────────────────────────────────────────────────────────────
const CHART_COLORS = [
  "#2b78e4", "#0a8f4e", "#e67e22", "#9b59b6",
  "#e74c3c", "#1abc9c", "#f39c12", "#2980b9"
];

const MONTHS_SHORT = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

// ── State ─────────────────────────────────────────────────────────────────
let pPortfolios = [];
let pActivePortfolioId = null;
let pActiveAssets = [];
let perfChart = null;
let hiddenDatasets = new Set();

// ── DOM refs ──────────────────────────────────────────────────────────────
const stripEl         = document.getElementById("perfPortfolioStrip");
const selectedNameEl  = document.getElementById("perfSelectedName");
const stockCountEl    = document.getElementById("perfStockCount");
const kpiTotalVal     = document.getElementById("kpiTotalValue");
const kpiCostBasis    = document.getElementById("kpiCostBasis");
const kpiProfitLoss   = document.getElementById("kpiProfitLoss");
const kpiReturn       = document.getElementById("kpiReturn");
const kpiBest         = document.getElementById("kpiBest");
const kpiWorst        = document.getElementById("kpiWorst");
const stockSelectEl   = document.getElementById("perfStockSelect");
const rangeSelectEl   = document.getElementById("perfRangeSelect");
const legendEl        = document.getElementById("perfChartLegend");
const lastUpdatedEl   = document.getElementById("lastUpdated");

// ── Helpers ───────────────────────────────────────────────────────────────
function fmt(v) {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(v);
}

function fmtPct(v) {
  return (v >= 0 ? "+" : "") + v.toFixed(2) + "%";
}

function setColor(el, value) {
  el.classList.remove("positive", "negative");
  el.classList.add(value >= 0 ? "positive" : "negative");
}

function now() {
  return new Date();
}

// ── Generate simulated history ─────────────────────────────────────────
/**
 * Build a plausible price series for an asset.
 * Starts from purchasePrice on datePurchased, ends near currentPrice today.
 * Uses a seeded random walk so it stays stable within a session.
 */
function generateHistory(asset, rangeDays) {
  const endDate   = now();
  const startDate = new Date(endDate);
  startDate.setDate(startDate.getDate() - rangeDays);

  // How many data points?
  const totalDays = Math.min(rangeDays, 730);
  const step      = Math.max(1, Math.floor(totalDays / 80)); // ~80 points max

  const purchaseDate = asset.datePurchased
    ? new Date(asset.datePurchased)
    : new Date(endDate.getFullYear() - 2, 0, 1);

  const points = [];
  let d = new Date(startDate);

  // Simple seeded pseudo-random based on ticker
  let seed = asset.ticker.split("").reduce((a, c) => a + c.charCodeAt(0), 0);
  function rand() {
    seed = (seed * 1664525 + 1013904223) & 0xffffffff;
    return (seed >>> 0) / 4294967296;
  }

  // Linear progress from purchasePrice → currentPrice, with noise
  const effectiveStart = d < purchaseDate ? asset.purchasePrice : asset.purchasePrice;
  const effectiveEnd   = asset.currentPrice;
  const range          = effectiveEnd - effectiveStart;

  let currentVal = effectiveStart;
  let idx = 0;

  while (d <= endDate) {
    if (d >= purchaseDate) {
      // Linear drift + noise
      const totalSteps = Math.ceil(totalDays / step);
      const progress   = Math.min(idx / Math.max(totalSteps - 1, 1), 1);
      const drift      = effectiveStart + range * progress;
      const noise      = (rand() - 0.5) * asset.purchasePrice * 0.04;
      currentVal       = Math.max(drift + noise, 0.01);
      points.push({ x: new Date(d), y: parseFloat(currentVal.toFixed(2)) });
    }

    d = new Date(d);
    d.setDate(d.getDate() + step);
    idx++;
  }

  // Always ensure last point is exactly currentPrice
  if (points.length > 0) {
    points[points.length - 1] = { x: new Date(endDate), y: asset.currentPrice };
  } else {
    points.push({ x: new Date(endDate), y: asset.currentPrice });
  }

  return points;
}

function rangeToDays(range) {
  switch (range) {
    case "3m":  return 90;
    case "6m":  return 180;
    case "1y":  return 365;
    case "2y":  return 730;
    case "all": return 1825;
    default:    return 365;
  }
}

// ── Portfolio summary helpers ─────────────────────────────────────────────
function computeSummary(assets) {
  const totalValue   = assets.reduce((s, a) => s + a.marketValue, 0);
  const totalCost    = assets.reduce((s, a) => s + a.quantity * a.purchasePrice, 0);
  const totalPL      = totalValue - totalCost;
  const returnPct    = totalCost > 0 ? (totalPL / totalCost) * 100 : 0;
  return { totalValue, totalCost, totalPL, returnPct };
}

function getBestWorst(assets) {
  if (!assets.length) return { best: null, worst: null };
  const sorted = [...assets].sort((a, b) => {
    const pla = ((a.marketValue - a.quantity * a.purchasePrice) / (a.quantity * a.purchasePrice)) * 100;
    const plb = ((b.marketValue - b.quantity * b.purchasePrice) / (b.quantity * b.purchasePrice)) * 100;
    return plb - pla;
  });
  return { best: sorted[0], worst: sorted[sorted.length - 1] };
}

// ── Render portfolio strip ────────────────────────────────────────────────
async function renderPortfolioStrip() {
  stripEl.innerHTML = "";

  for (const portfolio of pPortfolios) {
    let assets = [];
    try {
      assets = await apiGet(`/api/assets?portfolioId=${portfolio.id}`);
    } catch {
      assets = fallbackData.assetsByPortfolio[portfolio.id] || [];
    }

    const { totalValue, totalCost, totalPL, returnPct } = computeSummary(assets);
    const isActive = String(portfolio.id) === String(pActivePortfolioId);

    // Build hover stock list HTML
    const stockRows = assets.map(a => {
      const pl    = a.marketValue - a.quantity * a.purchasePrice;
      const plPct = a.quantity * a.purchasePrice > 0
        ? (pl / (a.quantity * a.purchasePrice)) * 100 : 0;
      const sign  = pl >= 0 ? "+" : "";
      return `
        <div class="stock-hover-list__row">
          <span class="stock-hover-list__ticker">${a.ticker}</span>
          <span class="stock-hover-list__company">${a.companyName}</span>
          <span class="stock-hover-list__pl ${pl >= 0 ? "positive" : "negative"}">
            ${sign}${plPct.toFixed(1)}%
          </span>
        </div>`;
    }).join("");

    // Clamp bar width
    const barWidth = Math.min(Math.abs(returnPct) * 2.5, 100);

    const card = document.createElement("div");
    card.className = `perf-portfolio-card${isActive ? " active" : ""}`;
    card.dataset.portfolioId = portfolio.id;
    card.setAttribute("tabindex", "0");
    card.setAttribute("role", "button");
    card.setAttribute("aria-label", `Select ${portfolio.name} portfolio`);
    card.innerHTML = `
      <div class="ppc__label">${portfolio.name}</div>
      <div class="ppc__value">${fmt(totalValue)}</div>
      <div class="ppc__meta-row">
        <span class="ppc__pl ${totalPL >= 0 ? "positive" : "negative"}">
          ${totalPL >= 0 ? "▲" : "▼"} ${fmt(Math.abs(totalPL))} (${fmtPct(returnPct)})
        </span>
        <span class="ppc__stocks">${assets.length} stock${assets.length !== 1 ? "s" : ""}</span>
      </div>
      <div class="ppc__return-bar">
        <div class="ppc__return-bar-fill ${totalPL >= 0 ? "positive" : "negative"}"
             style="width:${barWidth}%"></div>
      </div>

      <!-- Hover stock list -->
      <div class="stock-hover-list" role="tooltip">
        <div class="stock-hover-list__title">Holdings in ${portfolio.name}</div>
        ${stockRows || '<div class="stock-hover-list__row" style="color:var(--muted);font-style:italic">No holdings</div>'}
      </div>
    `;

    card.addEventListener("click", () => selectPortfolio(portfolio.id));
    card.addEventListener("keydown", e => {
      if (e.key === "Enter" || e.key === " ") {
        e.preventDefault();
        selectPortfolio(portfolio.id);
      }
    });

    stripEl.appendChild(card);
  }
}

function updateStripActive() {
  document.querySelectorAll(".perf-portfolio-card").forEach(c => {
    c.classList.toggle("active", String(c.dataset.portfolioId) === String(pActivePortfolioId));
  });
}

// ── KPI stats panel ───────────────────────────────────────────────────────
function renderStatsPanel(portfolio, assets) {
  const { totalValue, totalCost, totalPL, returnPct } = computeSummary(assets);
  const { best, worst } = getBestWorst(assets);

  selectedNameEl.textContent = portfolio.name;
  stockCountEl.textContent   = `${assets.length} stock${assets.length !== 1 ? "s" : ""}`;

  kpiTotalVal.textContent  = fmt(totalValue);
  kpiCostBasis.textContent = fmt(totalCost);

  kpiProfitLoss.textContent = (totalPL >= 0 ? "+" : "") + fmt(totalPL);
  setColor(kpiProfitLoss, totalPL);

  kpiReturn.textContent = fmtPct(returnPct);
  setColor(kpiReturn, returnPct);

  if (best) {
    const bPl = ((best.marketValue - best.quantity * best.purchasePrice) / (best.quantity * best.purchasePrice)) * 100;
    kpiBest.textContent = `${best.ticker} (${fmtPct(bPl)})`;
    setColor(kpiBest, bPl);
  } else {
    kpiBest.textContent = "—";
  }

  if (worst) {
    const wPl = ((worst.marketValue - worst.quantity * worst.purchasePrice) / (worst.quantity * worst.purchasePrice)) * 100;
    kpiWorst.textContent = `${worst.ticker} (${fmtPct(wPl)})`;
    setColor(kpiWorst, wPl);
  } else {
    kpiWorst.textContent = "—";
  }
}

// ── Stock dropdown ────────────────────────────────────────────────────────
function populateStockDropdown(assets) {
  stockSelectEl.innerHTML = "";

  // "All stocks" option
  const allOpt = document.createElement("option");
  allOpt.value = "all";
  allOpt.textContent = "All Stocks";
  stockSelectEl.appendChild(allOpt);

  assets.forEach(asset => {
    const opt = document.createElement("option");
    opt.value = asset.id;
    opt.textContent = `${asset.ticker} — ${asset.companyName}`;
    stockSelectEl.appendChild(opt);
  });

  stockSelectEl.value = "all";
}

// ── Chart legend ──────────────────────────────────────────────────────────
function renderLegend(datasets) {
  legendEl.innerHTML = "";
  datasets.forEach((ds, i) => {
    const chip = document.createElement("span");
    chip.className = "legend-chip";
    chip.dataset.index = i;
    chip.innerHTML = `
      <span class="legend-dot" style="background:${ds.borderColor}"></span>
      ${ds.label}
    `;
    chip.addEventListener("click", () => toggleDataset(i, chip));
    legendEl.appendChild(chip);
  });
}

function toggleDataset(index, chip) {
  if (!perfChart) return;
  const meta = perfChart.getDatasetMeta(index);
  if (hiddenDatasets.has(index)) {
    hiddenDatasets.delete(index);
    meta.hidden = false;
    chip.classList.remove("hidden");
  } else {
    hiddenDatasets.add(index);
    meta.hidden = true;
    chip.classList.add("hidden");
  }
  perfChart.update();
}

// ── Chart rendering ───────────────────────────────────────────────────────
function renderChart(assets, range) {
  const ctx = document.getElementById("perfStockChart").getContext("2d");
  const days = rangeToDays(range);

  const selectedVal = stockSelectEl.value;
  const targetAssets = selectedVal === "all"
    ? assets
    : assets.filter(a => String(a.id) === selectedVal);

  const datasets = targetAssets.map((asset, i) => {
    const history = generateHistory(asset, days);
    const color   = CHART_COLORS[i % CHART_COLORS.length];
    return {
      label: asset.ticker,
      data: history,
      borderColor: color,
      backgroundColor: hexToRgba(color, 0.06),
      borderWidth: 2,
      fill: targetAssets.length === 1,
      tension: 0.35,
      pointRadius: 0,
      pointHoverRadius: 5,
      pointHoverBackgroundColor: color,
      pointHoverBorderColor: "#fff",
      pointHoverBorderWidth: 2
    };
  });

  if (perfChart) {
    perfChart.destroy();
    hiddenDatasets.clear();
  }

  perfChart = new Chart(ctx, {
    type: "line",
    data: { datasets },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      interaction: {
        mode: "index",
        intersect: false
      },
      plugins: {
        legend: { display: false },
        tooltip: {
          backgroundColor: "#fff",
          borderColor: "#dde3ef",
          borderWidth: 1,
          titleColor: "#283349",
          bodyColor: "#5f6f8f",
          padding: 10,
          callbacks: {
            title: items => {
              if (!items.length) return "";
              const d = new Date(items[0].parsed.x);
              return d.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });
            },
            label: ctx => ` ${ctx.dataset.label}: ${fmt(ctx.parsed.y)}`
          }
        }
      },
      scales: {
        x: {
          type: "time",
          time: {
            unit: days <= 90 ? "week" : days <= 365 ? "month" : "quarter",
            displayFormats: {
              week:    "MMM d",
              month:   "MMM yyyy",
              quarter: "QQQ yyyy"
            }
          },
          grid: { color: "#f0f2f7" },
          ticks: { color: "#5f6f8f", font: { size: 11 } }
        },
        y: {
          grid: { color: "#f0f2f7" },
          ticks: {
            color: "#5f6f8f",
            font: { size: 11 },
            callback: v => fmt(v)
          }
        }
      }
    }
  });

  renderLegend(datasets);
}

function hexToRgba(hex, alpha) {
  const r = parseInt(hex.slice(1, 3), 16);
  const g = parseInt(hex.slice(3, 5), 16);
  const b = parseInt(hex.slice(5, 7), 16);
  return `rgba(${r},${g},${b},${alpha})`;
}

// ── Select portfolio ──────────────────────────────────────────────────────
async function selectPortfolio(id) {
  pActivePortfolioId = id;
  updateStripActive();

  let assets = [];
  try {
    assets = await apiGet(`/api/assets?portfolioId=${id}`);
  } catch {
    assets = fallbackData.assetsByPortfolio[id] || [];
  }

  pActiveAssets = assets;

  const portfolio = pPortfolios.find(p => String(p.id) === String(id));
  renderStatsPanel(portfolio, assets);
  populateStockDropdown(assets);
  renderChart(assets, rangeSelectEl.value);
}

// ── Event listeners ───────────────────────────────────────────────────────
stockSelectEl.addEventListener("change", () => {
  renderChart(pActiveAssets, rangeSelectEl.value);
});

rangeSelectEl.addEventListener("change", () => {
  renderChart(pActiveAssets, rangeSelectEl.value);
});

// ── Init ──────────────────────────────────────────────────────────────────
async function perfInit() {
  // Set last-updated timestamp
  lastUpdatedEl.textContent = now().toLocaleString("en-US", {
    month: "short", day: "numeric", year: "numeric",
    hour: "2-digit", minute: "2-digit"
  });

  // Load portfolios
  try {
    pPortfolios = await apiGet("/api/portfolios");
  } catch {
    pPortfolios = [...fallbackData.portfolios];
  }

  if (!pPortfolios.length) {
    stripEl.innerHTML = '<p class="perf-loading">No portfolios found.</p>';
    return;
  }

  pActivePortfolioId = pPortfolios[0].id;

  // Render strip
  await renderPortfolioStrip();

  // Load first portfolio
  await selectPortfolio(pActivePortfolioId);
}

document.addEventListener("DOMContentLoaded", perfInit);
