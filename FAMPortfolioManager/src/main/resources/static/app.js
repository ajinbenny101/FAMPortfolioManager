/* ============================================================
  app.js — Shared frontend API layer + dashboard page logic

  Backend integration covered here:
  - GET    /api/portfolios
  - POST   /api/portfolios
  - PUT    /api/portfolios/{id}
  - DELETE /api/portfolios/{id}
  - GET    /api/assets?portfolioId={id}
  - POST   /api/assets
  - PUT    /api/assets/{id}
  - DELETE /api/assets/{id}

  Why this file matters:
  1) It centralizes API calls and response normalization so all pages use
    the same object shape (id, marketValue, profitLoss, etc.).
  2) It powers dashboard-specific charts/KPIs for overall vs. individual
    portfolio analytics.
  ============================================================ */

// Resolve one or more backend bases.
// In local development, static files may be served from a different port,
// so we try same-origin first, then common backend ports.
const API_BASES = (() => {
  const host = window.location.hostname;
  const origin = window.location.origin;

  if (host === "localhost" || host === "127.0.0.1") {
    return [origin, `${window.location.protocol}//${host}:8080`, `${window.location.protocol}//${host}:8081`]
      .filter((v, i, arr) => arr.indexOf(v) === i);
  }

  return [origin];
})();

async function fetchWithApiFallback(path, options = {}) {
  let lastError;

  for (let i = 0; i < API_BASES.length; i++) {
    const base = API_BASES[i];
    try {
      const res = await fetch(`${base}${path}`, options);

      // When frontend is served by a non-backend dev server, /api/* commonly returns 404/405.
      // In that case, try the next known backend base (8080/8081) before failing.
      const shouldTryNextBase = (res.status === 404 || res.status === 405) && i < API_BASES.length - 1;
      if (shouldTryNextBase) {
        continue;
      }

      return res;
    } catch (err) {
      lastError = err;
    }
  }

  throw new Error(`Failed to fetch ${path}. Checked: ${API_BASES.join(", ")}. ${lastError?.message || ""}`.trim());
}

async function buildApiError(res, method, path) {
  let details = "";
  try {
    const text = await res.text();
    if (text) {
      try {
        const json = JSON.parse(text);
        details = json.message || json.details || text;
      } catch {
        details = text;
      }
    }
  } catch {
    details = "";
  }
  return new Error(`${method} ${path} failed: ${res.status}${details ? ` - ${details}` : ""}`);
}

// Backend DTOs expect ISO-8601 datetime values for purchase date fields.
// HTML date inputs provide YYYY-MM-DD, so append a midnight time component.
function toApiDateTime(dateValue) {
  if (!dateValue) return null;
  if (dateValue.includes("T")) return dateValue;
  return `${dateValue}T00:00:00`;
}

// Normalize asset payload shape from backend variants into one stable model
// consumed by all frontend pages.
function normalizeAsset(asset) {
  const quantity = Number(asset.quantity ?? 0);
  const purchasePrice = Number(asset.purchasePrice ?? 0);
  const currentPrice = Number(asset.currentPrice ?? purchasePrice);
  const marketValue = Number(asset.marketValue ?? (quantity * currentPrice));
  const profitLoss = Number(asset.profitLoss ?? (marketValue - (quantity * purchasePrice)));

  return {
    ...asset,
    id: Number(asset.id ?? asset.Id),
    purchaseDate: asset.purchaseDate ?? asset.datePurchased ?? null,
    datePurchased: asset.datePurchased ?? asset.purchaseDate ?? null,
    quantity,
    purchasePrice,
    currentPrice,
    marketValue,
    profitLoss
  };
}

// Normalize portfolio payloads and recursively normalize embedded assets.
function normalizePortfolio(portfolio) {
  return {
    ...portfolio,
    id: Number(portfolio.id ?? portfolio.Id),
    assets: Array.isArray(portfolio.assets) ? portfolio.assets.map(normalizeAsset) : []
  };
}

// Shared GET wrapper used by all pages.
// Adds normalization so page scripts can stay focused on rendering.
async function apiGet(path, options = {}) {
  const res = await fetchWithApiFallback(path, options);
  if (!res.ok) throw await buildApiError(res, "GET", path);
  const data = await res.json();

  if (path.includes("/performance")) {
    return data;
  }

  if (path.startsWith("/api/assets")) {
    return Array.isArray(data) ? data.map(normalizeAsset) : normalizeAsset(data);
  }
  if (path.startsWith("/api/portfolios")) {
    return Array.isArray(data) ? data.map(normalizePortfolio) : normalizePortfolio(data);
  }
  return data;
}

// Shared POST wrapper for create operations.
async function apiPost(path, body) {
  const res = await fetchWithApiFallback(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw await buildApiError(res, "POST", path);
  const data = await res.json();
  if (path.startsWith("/api/assets")) return normalizeAsset(data);
  if (path.startsWith("/api/portfolios")) return normalizePortfolio(data);
  return data;
}

// Shared PUT wrapper for update operations.
async function apiPut(path, body) {
  const res = await fetchWithApiFallback(path, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw await buildApiError(res, "PUT", path);
  const text = await res.text();
  if (!text) return {};
  const data = JSON.parse(text);
  if (path.startsWith("/api/assets")) return normalizeAsset(data);
  if (path.startsWith("/api/portfolios")) return normalizePortfolio(data);
  return data;
}

// Shared DELETE wrapper for remove operations.
async function apiDelete(path) {
  const res = await fetchWithApiFallback(path, { method: "DELETE" });
  if (!res.ok) throw await buildApiError(res, "DELETE", path);
}

const selectEl = document.getElementById("portfolioSelect");
const overallViewBtn = document.getElementById("overallViewBtn");
const individualViewBtn = document.getElementById("individualViewBtn");
const chartTitleEl = document.getElementById("chartTitle");

const totalValueEl = document.getElementById("totalValue");
const totalReturnEl = document.getElementById("totalReturn");

const portfolioNameEl = document.getElementById("portfolioName");
const portfolioValueEl = document.getElementById("portfolioValue");
const portfolioProfitLossEl = document.getElementById("portfolioProfitLoss");
const portfolioReturnEl = document.getElementById("portfolioReturn");

// Dashboard state: in-memory cache of portfolios and per-portfolio holdings.
let performanceChart;
let trendChart;
let portfolios = [];
let assetsByPortfolio = {};
let currentViewMode = "overall";

function formatCurrency(value) {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value);
}

function formatMonthLabel(isoDate) {
  const d = new Date(isoDate);
  if (Number.isNaN(d.getTime())) return isoDate;
  return d.toLocaleString("en-US", { month: "short", year: "2-digit" });
}

// Compute aggregate valuation metrics from an asset array.
// Used by KPI cards and chart summaries.
function calculatePortfolioMetrics(assets) {
  const marketValue = assets.reduce((sum, asset) => sum + asset.marketValue, 0);
  const costBasis = assets.reduce((sum, asset) => sum + asset.quantity * asset.purchasePrice, 0);
  const profitLoss = marketValue - costBasis;
  const returnPercent = costBasis > 0 ? (profitLoss / costBasis) * 100 : 0;
  return { marketValue, profitLoss, returnPercent };
}

// Overall mode chart: each slice represents one portfolio's total market value.
function renderOverallPerformanceChart() {
  const ctx = document.getElementById("performanceChart").getContext("2d");

  const chartData = portfolios.map(portfolio => {
    const assets = assetsByPortfolio[portfolio.id] || [];
    const { marketValue } = calculatePortfolioMetrics(assets);
    return marketValue;
  });

  if (performanceChart) {
    performanceChart.destroy();
  }

  performanceChart = new Chart(ctx, {
    type: "pie",
    data: {
      labels: portfolios.map(p => p.name),
      datasets: [{
        data: chartData,
        backgroundColor: ["#4CAF50", "#2196F3", "#FF9800", "#9C27B0"],
        borderWidth: 2,
        borderColor: "#fff"
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: "right"
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              const value = context.parsed;
              return `${context.label}: ${formatCurrency(value)}`;
            }
          }
        }
      }
    }
  });
}

// Individual mode chart: each slice represents one stock in the selected portfolio.
function renderIndividualPerformanceChart(portfolioId) {
  const assets = assetsByPortfolio[portfolioId] || [];
  const ctx = document.getElementById("performanceChart").getContext("2d");

  const chartData = assets.map(asset => asset.marketValue);
  const labels = assets.map(asset => asset.ticker);

  if (performanceChart) {
    performanceChart.destroy();
  }

  performanceChart = new Chart(ctx, {
    type: "pie",
    data: {
      labels: labels,
      datasets: [{
        data: chartData,
        backgroundColor: ["#FF6384", "#36A2EB", "#FFCE56", "#4BC0C0", "#9966FF", "#FF9F40"],
        borderWidth: 2,
        borderColor: "#fff"
      }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: "right"
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              const value = context.parsed;
              return `${context.label}: ${formatCurrency(value)}`;
            }
          }
        }
      }
    }
  });
}

// Build a lightweight month-by-month trend approximation from available
// asset data when full historical prices are not persisted in the backend.
function buildTrendFromAssets(assets) {
  if (!assets.length) {
    return { labels: ["Now"], data: [0] };
  }

  const now = new Date();
  const parsedDates = assets
    .map(a => new Date(a.datePurchased || a.purchaseDate || now))
    .filter(d => !Number.isNaN(d.getTime()));

  const start = new Date(Math.min(...parsedDates.map(d => d.getTime())));
  start.setDate(1);
  start.setHours(0, 0, 0, 0);

  const labels = [];
  const data = [];
  const cursor = new Date(start);

  while (cursor <= now) {
    const monthEnd = new Date(cursor.getFullYear(), cursor.getMonth() + 1, 0, 23, 59, 59, 999);

    const totalAtMonth = assets.reduce((sum, a) => {
      const purchaseDate = new Date(a.datePurchased || a.purchaseDate || now);
      if (Number.isNaN(purchaseDate.getTime()) || purchaseDate > monthEnd) {
        return sum;
      }

      const purchaseValue = Number(a.quantity) * Number(a.purchasePrice);
      const currentValue = Number(a.marketValue ?? (Number(a.quantity) * Number(a.currentPrice ?? a.purchasePrice)));
      const denominator = Math.max(now.getTime() - purchaseDate.getTime(), 1);
      const progress = Math.max(0, Math.min(1, (monthEnd.getTime() - purchaseDate.getTime()) / denominator));
      const estimatedValue = purchaseValue + (currentValue - purchaseValue) * progress;
      return sum + estimatedValue;
    }, 0);

    labels.push(cursor.toLocaleString("en-US", { month: "short", year: "2-digit" }));
    data.push(Math.round(totalAtMonth));

    cursor.setMonth(cursor.getMonth() + 1);
  }

  return { labels, data };
}

function mapPerformanceSeries(points) {
  if (!Array.isArray(points) || !points.length) {
    return { labels: ["Now"], data: [0] };
  }

  return {
    labels: points.map(p => formatMonthLabel(p.date)),
    data: points.map(p => Number(p.totalValue ?? 0))
  };
}

async function getOverallTrendData(allAssets) {
  try {
    const points = await apiGet(`/api/portfolios/performance/overall?ts=${Date.now()}`, {
      cache: "no-store"
    });
    return mapPerformanceSeries(points);
  } catch {
    return buildTrendFromAssets(allAssets);
  }
}

async function getPortfolioTrendData(portfolioId, assets) {
  try {
    const points = await apiGet(`/api/portfolios/${portfolioId}/performance?ts=${Date.now()}`, {
      cache: "no-store"
    });
    return mapPerformanceSeries(points);
  } catch {
    return buildTrendFromAssets(assets);
  }
}

// Render trend line chart (overall portfolio or single selected portfolio).
function renderPerformanceTrendChart(datasets) {
  const ctx = document.getElementById("trendChart").getContext("2d");

  if (trendChart) {
    trendChart.destroy();
  }

  const colors = ["#4CAF50", "#2196F3", "#FF9800", "#9C27B0"];
  
  const chartDatasets = datasets.map((dataset, index) => ({
    label: dataset.label,
    data: dataset.data,
    borderColor: colors[index % colors.length],
    backgroundColor: colors[index % colors.length].replace(")", ", 0.1)").replace("rgb", "rgba"),
    borderWidth: 2,
    fill: false,
    tension: 0.4,
    pointRadius: 3,
    pointBackgroundColor: colors[index % colors.length]
  }));

  trendChart = new Chart(ctx, {
    type: "line",
    data: {
      labels: datasets[0].labels,
      datasets: chartDatasets
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: {
        legend: {
          position: "top"
        },
        tooltip: {
          callbacks: {
            label: function(context) {
              return `${context.dataset.label}: ${formatCurrency(context.parsed.y)}`;
            }
          }
        }
      },
      scales: {
        y: {
          ticks: {
            callback: function(value) {
              return formatCurrency(value);
            }
          }
        }
      }
    }
  });
}

// Toggle between dashboard perspectives and refresh all dependent UI blocks:
// charts, totals, and selected portfolio summary.
async function switchViewMode(mode) {
  currentViewMode = mode;

  if (mode === "overall") {
    overallViewBtn.classList.add("active");
    individualViewBtn.classList.remove("active");
    chartTitleEl.textContent = "Portfolio Distribution";
    selectEl.style.display = "none";
    document.getElementById("portfolioSummary").style.display = "none";
    renderOverallPerformanceChart();

    const allAssets = Object.values(assetsByPortfolio).flat();
    const { marketValue, profitLoss, returnPercent } = calculatePortfolioMetrics(allAssets);
    totalValueEl.textContent = formatCurrency(marketValue);
    totalReturnEl.textContent = `${returnPercent.toFixed(2)}%`;

    const overallPerfData = await getOverallTrendData(allAssets);
    renderPerformanceTrendChart([{ label: "Summary Of Assets", labels: overallPerfData.labels, data: overallPerfData.data }]);
  } else {
    overallViewBtn.classList.remove("active");
    individualViewBtn.classList.add("active");
    chartTitleEl.textContent = "Individual Performance";
    selectEl.style.display = "inline-block";
    document.getElementById("portfolioSummary").style.display = "grid";
    
    const selectedPortfolioId = parseInt(selectEl.value);
    renderIndividualPerformanceChart(selectedPortfolioId);
    renderSelectedPortfolioSummary();

    const selectedPortfolio = portfolios.find(p => p.id === selectedPortfolioId);
    const assets = assetsByPortfolio[selectedPortfolioId] || [];
    const perfData = await getPortfolioTrendData(selectedPortfolioId, assets);
    renderPerformanceTrendChart([{ label: selectedPortfolio.name, labels: perfData.labels, data: perfData.data }]);
  }
}

// Populate portfolio selector used by individual dashboard mode.
function populatePortfolioDropdown() {
  portfolios.forEach(portfolio => {
    const option = document.createElement("option");
    option.value = portfolio.id;
    option.textContent = portfolio.name;
    selectEl.appendChild(option);
  });
  selectEl.value = portfolios[0].id;
}

// Render per-portfolio KPI summary for selected option in dropdown.
function renderSelectedPortfolioSummary() {
  const selectedPortfolioId = parseInt(selectEl.value);
  const selectedPortfolio = portfolios.find(p => p.id === selectedPortfolioId);
  const assets = assetsByPortfolio[selectedPortfolioId] || [];
  const { marketValue, profitLoss, returnPercent } = calculatePortfolioMetrics(assets);

  portfolioNameEl.textContent = selectedPortfolio?.name || "—";
  portfolioValueEl.textContent = formatCurrency(marketValue);
  portfolioProfitLossEl.textContent = formatCurrency(profitLoss);
  portfolioReturnEl.textContent = `${returnPercent.toFixed(2)}%`;
}

// Initial data load for dashboard screen:
// 1) portfolios
// 2) holdings for each portfolio
async function loadDashboardData() {
  try {
    portfolios = await apiGet("/api/portfolios");
  } catch {
    portfolios = [];
  }

  assetsByPortfolio = {};
  for (const portfolio of portfolios) {
    try {
      assetsByPortfolio[portfolio.id] = await apiGet(`/api/assets?portfolioId=${portfolio.id}`);
    } catch {
      assetsByPortfolio[portfolio.id] = [];
    }
  }
}

// Dashboard bootstrap.
// Runs after DOM is ready so all chart containers and controls are available.
async function init() {
  await loadDashboardData();
  if (!portfolios.length) return;

  populatePortfolioDropdown();
  await switchViewMode("overall");

  overallViewBtn.addEventListener("click", async () => switchViewMode("overall"));
  individualViewBtn.addEventListener("click", async () => switchViewMode("individual"));
  selectEl.addEventListener("change", async () => {
    const selectedPortfolioId = parseInt(selectEl.value);
    renderIndividualPerformanceChart(selectedPortfolioId);
    renderSelectedPortfolioSummary();
    
    const selectedPortfolio = portfolios.find(p => p.id === selectedPortfolioId);
    const assets = assetsByPortfolio[selectedPortfolioId] || [];
    const perfData = await getPortfolioTrendData(selectedPortfolioId, assets);
    renderPerformanceTrendChart([{ label: selectedPortfolio.name, labels: perfData.labels, data: perfData.data }]);
  });
}

document.addEventListener("DOMContentLoaded", init);
