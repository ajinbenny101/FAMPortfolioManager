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
const overallViewBtn = document.getElementById("overallViewBtn");
const individualViewBtn = document.getElementById("individualViewBtn");
const chartTitleEl = document.getElementById("chartTitle");

const totalValueEl = document.getElementById("totalValue");
const totalReturnEl = document.getElementById("totalReturn");

const portfolioNameEl = document.getElementById("portfolioName");
const portfolioValueEl = document.getElementById("portfolioValue");
const portfolioProfitLossEl = document.getElementById("portfolioProfitLoss");
const portfolioReturnEl = document.getElementById("portfolioReturn");

let performanceChart;
let trendChart;
let portfolios = [];
let currentViewMode = "overall";

function formatCurrency(value) {
  return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(value);
}

function calculatePortfolioMetrics(assets) {
  const marketValue = assets.reduce((sum, asset) => sum + asset.marketValue, 0);
  const costBasis = assets.reduce((sum, asset) => sum + asset.quantity * asset.purchasePrice, 0);
  const profitLoss = marketValue - costBasis;
  const returnPercent = costBasis > 0 ? (profitLoss / costBasis) * 100 : 0;
  return { marketValue, profitLoss, returnPercent };
}

function renderOverallPerformanceChart() {
  const ctx = document.getElementById("performanceChart").getContext("2d");

  const chartData = portfolios.map(portfolio => {
    const assets = fallbackData.assetsByPortfolio[portfolio.id] || [];
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

function renderIndividualPerformanceChart(portfolioId) {
  const assets = fallbackData.assetsByPortfolio[portfolioId] || [];
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

function generatePerformanceData(baseValue) {
  const months = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];
  const data = [];
  let currentValue = baseValue * 0.85;
  
  for (let i = 0; i < months.length; i++) {
    const change = (Math.random() - 0.45) * baseValue * 0.02;
    currentValue = Math.max(currentValue + change, baseValue * 0.7);
    data.push(Math.round(currentValue));
  }
  
  return { labels: months, data };
}

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

function switchViewMode(mode) {
  currentViewMode = mode;

  if (mode === "overall") {
    overallViewBtn.classList.add("active");
    individualViewBtn.classList.remove("active");
    chartTitleEl.textContent = "Overall Performance";
    selectEl.style.display = "none";
    document.getElementById("portfolioSummary").style.display = "none";
    renderOverallPerformanceChart();

    const allAssets = Object.values(fallbackData.assetsByPortfolio).flat();
    const { marketValue, profitLoss, returnPercent } = calculatePortfolioMetrics(allAssets);
    totalValueEl.textContent = formatCurrency(marketValue);
    totalReturnEl.textContent = `${returnPercent.toFixed(2)}%`;

    const overallPerfData = generatePerformanceData(marketValue);
    renderPerformanceTrendChart([{ label: "Total Portfolio", labels: overallPerfData.labels, data: overallPerfData.data }]);
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
    const assets = fallbackData.assetsByPortfolio[selectedPortfolioId] || [];
    const { marketValue } = calculatePortfolioMetrics(assets);
    const perfData = generatePerformanceData(marketValue);
    renderPerformanceTrendChart([{ label: selectedPortfolio.name, labels: perfData.labels, data: perfData.data }]);
  }
}

function populatePortfolioDropdown() {
  portfolios.forEach(portfolio => {
    const option = document.createElement("option");
    option.value = portfolio.id;
    option.textContent = portfolio.name;
    selectEl.appendChild(option);
  });
  selectEl.value = portfolios[0].id;
}

function renderSelectedPortfolioSummary() {
  const selectedPortfolioId = parseInt(selectEl.value);
  const selectedPortfolio = portfolios.find(p => p.id === selectedPortfolioId);
  const assets = fallbackData.assetsByPortfolio[selectedPortfolioId] || [];
  const { marketValue, profitLoss, returnPercent } = calculatePortfolioMetrics(assets);

  portfolioNameEl.textContent = selectedPortfolio.name;
  portfolioValueEl.textContent = formatCurrency(marketValue);
  portfolioProfitLossEl.textContent = formatCurrency(profitLoss);
  portfolioReturnEl.textContent = `${returnPercent.toFixed(2)}%`;
}

function init() {
  portfolios = fallbackData.portfolios;
  populatePortfolioDropdown();
  switchViewMode("overall");

  overallViewBtn.addEventListener("click", () => switchViewMode("overall"));
  individualViewBtn.addEventListener("click", () => switchViewMode("individual"));
  selectEl.addEventListener("change", () => {
    const selectedPortfolioId = parseInt(selectEl.value);
    renderIndividualPerformanceChart(selectedPortfolioId);
    renderSelectedPortfolioSummary();
    
    const selectedPortfolio = portfolios.find(p => p.id === selectedPortfolioId);
    const assets = fallbackData.assetsByPortfolio[selectedPortfolioId] || [];
    const { marketValue } = calculatePortfolioMetrics(assets);
    const perfData = generatePerformanceData(marketValue);
    renderPerformanceTrendChart([{ label: selectedPortfolio.name, labels: perfData.labels, data: perfData.data }]);
  });
}

document.addEventListener("DOMContentLoaded", init);
