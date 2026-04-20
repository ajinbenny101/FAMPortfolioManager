/* ============================================================
   performance.js — Performance page logic
  Depends on: app.js (for apiGet, formatCurrency)

  Backend integration in this page:
  - GET /api/portfolios
  - GET /api/assets?portfolioId={id}

  Primary goal:
  Translate backend portfolio/asset snapshots into decision-friendly KPI
  cards and interactive performance trend charts.
   ============================================================ */

// ── Constants ─────────────────────────────────────────────────────────────
// Stable palette reused for chart datasets and custom legend chips.
const CHART_COLORS = [
  "#4E79A7", "#59A14F", "#F28E2B", "#B07AA1",
  "#E15759", "#76B7B2", "#EDC948", "#9C755F"
];

const MONTHS_SHORT = ["Jan", "Feb", "Mar", "Apr", "May", "Jun",
                      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

// ── State ─────────────────────────────────────────────────────────────────
// Runtime state for selected portfolio, loaded holdings and chart instance.
let pPortfolios = [];
let pActivePortfolioId = null;
let pActiveAssets = [];
let perfChart = null;
let hiddenDatasets = new Set();
let pAssetSeriesCache = new Map();

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

// Apply positive/negative semantic coloring to KPI values.
function setColor(el, value) {
  el.classList.remove("positive", "negative");
  el.classList.add(value >= 0 ? "positive" : "negative");
}

function now() {
  return new Date();
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

function filterSeriesByRange(points, range) {
  if (!Array.isArray(points) || points.length === 0) return [];
  if (range === "all") return points;

  const days = rangeToDays(range);
  const endDate = now();
  const startDate = new Date(endDate);
  startDate.setDate(startDate.getDate() - days);

  return points.filter(point => {
    const d = new Date(point.x);
    return !Number.isNaN(d.getTime()) && d >= startDate && d <= endDate;
  });
}

async function fetchAssetPerformanceSeries(assetId) {
  const cacheKey = String(assetId);
  if (pAssetSeriesCache.has(cacheKey)) {
    return pAssetSeriesCache.get(cacheKey);
  }

  const rawSeries = await apiGet(`/api/assets/${assetId}/performance?ts=${Date.now()}`, {
    cache: "no-store"
  });

  const mapped = Array.isArray(rawSeries)
    ? rawSeries
        .map(p => ({ x: new Date(p.date), y: Number(p.totalValue ?? 0) }))
        .filter(p => !Number.isNaN(p.x.getTime()))
    : [];

  pAssetSeriesCache.set(cacheKey, mapped);
  return mapped;
}

// ── Portfolio summary helpers ─────────────────────────────────────────────
function computeSummary(assets) {
  const totalValue   = assets.reduce((s, a) => s + a.marketValue, 0);
  const totalCost    = assets.reduce((s, a) => s + a.quantity * a.purchasePrice, 0);
  const totalPL      = totalValue - totalCost;
  const returnPct    = totalCost > 0 ? (totalPL / totalCost) * 100 : 0;
  return { totalValue, totalCost, totalPL, returnPct };
}

// Identify top and bottom performers by % return within the selected portfolio.
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
    // Pull live holdings so each strip card reflects current backend valuation.
    let assets = [];
    try {
      assets = await apiGet(`/api/assets?portfolioId=${portfolio.id}`);
    } catch {
      assets = [];
    }

    const { totalValue, totalCost, totalPL, returnPct } = computeSummary(assets);
    const isActive = String(portfolio.id) === String(pActivePortfolioId);

    // Build hover stock list HTML for quick composition insight.
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

    // Clamp return bar width so extreme values do not overflow card layout.
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

  // KPI cards are all derived from backend-provided asset snapshots.
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

  // "All stocks" option allows multi-series portfolio comparison.
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

// Legend chips toggle visibility without rebuilding dataset arrays.
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
async function renderChart(assets, range) {
  const ctx = document.getElementById("perfStockChart").getContext("2d");
  const days = rangeToDays(range);

  // Optional focus mode: one stock, otherwise compare all.
  const selectedVal = stockSelectEl.value;
  const targetAssets = selectedVal === "all"
    ? assets
    : assets.filter(a => String(a.id) === selectedVal);

  const seriesList = await Promise.all(targetAssets.map(async asset => {
    try {
      const fullSeries = await fetchAssetPerformanceSeries(asset.id);
      return filterSeriesByRange(fullSeries, range);
    } catch {
      return [];
    }
  }));

  const datasets = targetAssets.map((asset, i) => {
    const history = seriesList[i] || [];
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

  // Recreate chart when range/selection changes to keep config straightforward.
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

  // Fetch all holdings for selected portfolio from backend.
  let assets = [];
  try {
    assets = await apiGet(`/api/assets?portfolioId=${id}`);
  } catch {
    assets = [];
  }

  pActiveAssets = assets;
  pAssetSeriesCache = new Map();

  const portfolio = pPortfolios.find(p => String(p.id) === String(id));
  renderStatsPanel(portfolio, assets);
  populateStockDropdown(assets);
  
  // Update chart title to show active portfolio name
  const chartTitle = document.querySelector(".perf-chart-title");
  if (chartTitle && portfolio) {
    chartTitle.textContent = `Stock Performance Over Time for ${portfolio.name}`;
  }
  
  await renderChart(assets, rangeSelectEl.value);
}

// ── Event listeners ───────────────────────────────────────────────────────
stockSelectEl.addEventListener("change", async () => {
  await renderChart(pActiveAssets, rangeSelectEl.value);
});

rangeSelectEl.addEventListener("change", async () => {
  await renderChart(pActiveAssets, rangeSelectEl.value);
});

// ── Init ──────────────────────────────────────────────────────────────────
async function perfInit() {
  // Set last-updated timestamp
  lastUpdatedEl.textContent = now().toLocaleString("en-US", {
    month: "short", day: "numeric", year: "numeric",
    hour: "2-digit", minute: "2-digit"
  });

  // Initial portfolio fetch seeds strip + default selection.
  try {
    pPortfolios = await apiGet("/api/portfolios");
  } catch {
    pPortfolios = [];
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
