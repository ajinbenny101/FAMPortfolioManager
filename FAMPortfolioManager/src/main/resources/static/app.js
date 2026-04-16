const API_BASE = window.location.origin;

const fallbackData = {
  portfolios: [
    { id: 1, name: "Growth Portfolio" },
    { id: 2, name: "Income Portfolio" }
  ],
  assetsByPortfolio: {
    1: [
      { id: 1, ticker: "AAPL", companyName: "Apple Inc.", quantity: 35, purchasePrice: 120, currentPrice: 145.32, marketValue: 5086.2, profitLoss: 886.2 },
      { id: 2, ticker: "TSLA", companyName: "Tesla Inc.", quantity: 20, purchasePrice: 650, currentPrice: 720.15, marketValue: 14403, profitLoss: 1403 },
      { id: 3, ticker: "AMZN", companyName: "Amazon.com Inc.", quantity: 10, purchasePrice: 3100, currentPrice: 3340.5, marketValue: 33405, profitLoss: 2405 },
      { id: 4, ticker: "GOOGL", companyName: "Alphabet Inc.", quantity: 8, purchasePrice: 2500, currentPrice: 2750.75, marketValue: 22006, profitLoss: 2006 }
    ],
    2: [
      { id: 5, ticker: "MSFT", companyName: "Microsoft Corp.", quantity: 22, purchasePrice: 350, currentPrice: 410.25, marketValue: 9025.5, profitLoss: 1325.5 },
      { id: 6, ticker: "JNJ", companyName: "Johnson & Johnson", quantity: 40, purchasePrice: 152, currentPrice: 161.4, marketValue: 6456, profitLoss: 376 },
      { id: 7, ticker: "VZ", companyName: "Verizon", quantity: 70, purchasePrice: 39, currentPrice: 42.3, marketValue: 2961, profitLoss: 231 },
      { id: 8, ticker: "BND", companyName: "Vanguard Total Bond", quantity: 90, purchasePrice: 71, currentPrice: 72.8, marketValue: 6552, profitLoss: 162 }
    ]
  }
};

const selectEl = document.getElementById("portfolioSelect");
const holdingsBody = document.getElementById("holdingsBody");
const refreshButton = document.getElementById("refreshButton");
const statusMessageEl = document.getElementById("statusMessage");

const totalValueEl = document.getElementById("totalValue");
const totalReturnEl = document.getElementById("totalReturn");
const cashValueEl = document.getElementById("cashValue");
const stocksValueEl = document.getElementById("stocksValue");
const bondsValueEl = document.getElementById("bondsValue");
const cryptoValueEl = document.getElementById("cryptoValue");

let allocationChart;
let performanceChart;
let portfolios = [];

function formatCurrency(value) {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value);
}

async function apiGet(path) {
  const response = await fetch(`${API_BASE}${path}`);
  if (!response.ok) {
    let details = "";

    try {
      const errorBody = await response.json();
      details = errorBody.details || errorBody.message || "";
    } catch {
      details = "";
    }

    const suffix = details ? ` - ${details}` : "";
    throw new Error(`Request failed: ${response.status}${suffix}`);
  }
  return response.json();
}

function setStatus(message, isError = false) {
  statusMessageEl.textContent = message;
  statusMessageEl.style.color = isError ? "#c62828" : "";
}

function populatePortfolioDropdown(items) {
  selectEl.innerHTML = "";
  items.forEach((portfolio) => {
    const option = document.createElement("option");
    option.value = portfolio.id;
    option.textContent = portfolio.name;
    selectEl.appendChild(option);
  });
}

function calculateHoldingsValues(assets) {
  return assets.reduce(
    (acc, item) => {
      const marketValue = Number(item.marketValue ?? item.quantity * item.currentPrice);
      const cost = Number(item.quantity * item.purchasePrice);
      acc.totalMarketValue += marketValue;
      acc.totalCost += cost;
      acc.totalProfitLoss += Number(item.profitLoss ?? marketValue - cost);
      return acc;
    },
    { totalMarketValue: 0, totalCost: 0, totalProfitLoss: 0 }
  );
}

function renderHoldingsTable(assets) {
  holdingsBody.innerHTML = "";

  assets.forEach((item) => {
    const tr = document.createElement("tr");
    const marketValue = Number(item.marketValue ?? item.quantity * item.currentPrice);
    const pl = Number(item.profitLoss ?? marketValue - item.quantity * item.purchasePrice);

    tr.innerHTML = `
      <td>${item.ticker}</td>
      <td>${item.companyName}</td>
      <td>${item.quantity}</td>
      <td>${formatCurrency(item.currentPrice)}</td>
      <td>${formatCurrency(item.purchasePrice)}</td>
      <td>${formatCurrency(marketValue)}</td>
      <td>${pl >= 0 ? "+" : ""}${formatCurrency(pl)}</td>
    `;

    holdingsBody.appendChild(tr);
  });
}

function buildAllocationFromAssets(assets, totalValue) {
  if (!assets.length || totalValue <= 0) {
    return { labels: ["No Data"], values: [100] };
  }

  const sorted = [...assets].sort((a, b) => (b.marketValue ?? 0) - (a.marketValue ?? 0));
  const top = sorted.slice(0, 5);
  const rest = sorted.slice(5);

  const labels = top.map((a) => a.ticker);
  const values = top.map((a) => Number(((a.marketValue / totalValue) * 100).toFixed(2)));

  if (rest.length) {
    const otherValue = rest.reduce((sum, a) => sum + a.marketValue, 0);
    labels.push("Other");
    values.push(Number(((otherValue / totalValue) * 100).toFixed(2)));
  }

  return { labels, values };
}

function buildPerformanceSeries(totalCost, totalValue) {
  const labels = ["M-11", "M-10", "M-9", "M-8", "M-7", "M-6", "M-5", "M-4", "M-3", "M-2", "M-1", "Now"];
  const start = totalCost > 0 ? totalCost : totalValue * 0.9;
  const points = labels.map((_, idx) => {
    const t = idx / (labels.length - 1);
    const value = start + (totalValue - start) * t;
    return Number(value.toFixed(2));
  });
  return { labels, points };
}

function renderSummary(assets) {
  const { totalMarketValue, totalCost, totalProfitLoss } = calculateHoldingsValues(assets);
  const totalValue = totalMarketValue;
  const totalReturnPct = totalCost > 0 ? (totalProfitLoss / totalCost) * 100 : 0;

  totalValueEl.textContent = formatCurrency(totalValue);
  totalReturnEl.textContent = `${totalReturnPct.toFixed(2)}%`;

  cashValueEl.textContent = formatCurrency(0);
  stocksValueEl.textContent = formatCurrency(totalMarketValue);
  bondsValueEl.textContent = formatCurrency(0);
  cryptoValueEl.textContent = formatCurrency(0);

  return { totalCost, totalValue };
}

function renderAllocationChart(assets, totalValue) {
  const { labels, values } = buildAllocationFromAssets(assets, totalValue);

  if (allocationChart) {
    allocationChart.destroy();
  }

  allocationChart = new Chart(document.getElementById("allocationChart"), {
    type: "doughnut",
    data: {
      labels,
      datasets: [
        {
          data: values,
          backgroundColor: ["#2b78e4", "#0a8f4e", "#e84762", "#f0b429", "#6f42c1", "#20c997"],
          borderWidth: 0
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: "right" } },
      cutout: "55%"
    }
  });
}

function renderPerformanceChart(totalCost, totalValue) {
  const series = buildPerformanceSeries(totalCost, totalValue);

  if (performanceChart) {
    performanceChart.destroy();
  }

  performanceChart = new Chart(document.getElementById("performanceChart"), {
    type: "line",
    data: {
      labels: series.labels,
      datasets: [
        {
          label: "Portfolio Value",
          data: series.points,
          borderColor: "#2b78e4",
          pointRadius: 0,
          fill: true,
          backgroundColor: "rgba(43, 120, 228, 0.2)",
          tension: 0.2
        }
      ]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      scales: {
        y: {
          ticks: {
            callback: (value) => `$${(value / 1000).toFixed(0)}k`
          }
        }
      }
    }
  });
}

async function loadAssetsByPortfolioId(portfolioId) {
  try {
    const assets = await apiGet(`/api/assets?portfolioId=${portfolioId}`);
    setStatus(`Loaded ${assets.length} asset${assets.length === 1 ? "" : "s"}.`);
    return assets;
  } catch (error) {
    const selectedPortfolio = portfolios.find((portfolio) => String(portfolio.id) === String(portfolioId));
    if (selectedPortfolio?.assets?.length) {
      setStatus("Asset endpoint failed, using portfolio payload instead.", true);
      return selectedPortfolio.assets;
    }

    if (fallbackData.assetsByPortfolio[portfolioId]) {
      setStatus(`Backend asset fetch failed: ${error.message}. Showing demo data.`, true);
      return fallbackData.assetsByPortfolio[portfolioId] || [];
    }

    setStatus(`Backend asset fetch failed: ${error.message}.`, true);
    return fallbackData.assetsByPortfolio[portfolioId] || [];
  }
}

async function updateDashboard(portfolioId) {
  const assets = await loadAssetsByPortfolioId(portfolioId);
  renderHoldingsTable(assets);
  const summary = renderSummary(assets);
  renderAllocationChart(assets, summary.totalValue);
  renderPerformanceChart(summary.totalCost, summary.totalValue);
}

async function refreshPortfolios() {
  try {
    portfolios = await apiGet("/api/portfolios");
    populatePortfolioDropdown(portfolios);
    return true;
  } catch (error) {
    if (!portfolios.length) {
      portfolios = fallbackData.portfolios;
      populatePortfolioDropdown(portfolios);
    }
    setStatus(`Portfolio fetch failed: ${error.message}.`, true);
    return false;
  }
}

async function refreshDashboard() {
  const selectedId = selectEl.value || portfolios[0]?.id;
  if (!selectedId) {
    return;
  }

  await refreshPortfolios();

  const stillExists = portfolios.some((portfolio) => String(portfolio.id) === String(selectedId));
  const nextId = stillExists ? selectedId : portfolios[0]?.id;

  if (!nextId) {
    return;
  }

  selectEl.value = String(nextId);
  await updateDashboard(nextId);
}

async function init() {
  await refreshPortfolios();

  if (!portfolios.length) {
    setStatus("No portfolios found yet.");
    return;
  }

  await refreshDashboard();

  selectEl.addEventListener("change", async (event) => {
    await updateDashboard(event.target.value);
  });

  refreshButton.addEventListener("click", async () => {
    setStatus("Refreshing data...");
    await refreshDashboard();
  });

  window.addEventListener("focus", async () => {
    await refreshDashboard();
  });
}

init();
