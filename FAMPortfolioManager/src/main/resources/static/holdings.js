/* ============================================================
   holdings.js — Holdings page logic
   Depends on: app.js (for apiGet, formatCurrency, fallbackData)
   ============================================================ */

// ── State ──────────────────────────────────────────────────────────────────
let hPortfolios = [];           // all portfolios
let hAllAssets = [];            // all assets for active portfolio (raw)
let hFilteredAssets = [];       // after search/filter
let hActivePortfolioId = null;  // currently selected portfolio id
let hSelectedAssetIds = new Set();

// Extend fallback with datePurchased for demo purposes
(function patchFallbackDates() {
  const dates = {
    1: ["2021-03-15", "2020-11-02", "2019-07-22", "2022-01-10"],
    2: ["2020-05-18", "2018-09-30", "2021-12-01", "2019-04-14"]
  };
  Object.entries(fallbackData.assetsByPortfolio).forEach(([pid, assets]) => {
    assets.forEach((a, i) => {
      if (!a.datePurchased) {
        a.datePurchased = (dates[pid] && dates[pid][i]) || "2020-01-01";
      }
    });
  });
})();

// ── DOM refs ───────────────────────────────────────────────────────────────
const portfolioStripEl    = document.getElementById("portfolioStrip");
const holdingsBodyEl      = document.getElementById("holdingsBody");
const holdingsCountEl     = document.getElementById("holdingsCount");
const holdingsStatusEl    = document.getElementById("holdingsStatusMessage");
const searchInputEl       = document.getElementById("searchInput");
const sortSelectEl        = document.getElementById("sortSelect");
const plFilterEl          = document.getElementById("plFilter");
const minValueEl          = document.getElementById("minValueInput");
const clearFiltersEl      = document.getElementById("clearFiltersBtn");
const selectAllEl         = document.getElementById("selectAllCheckbox");
const selectionSummaryEl  = document.getElementById("selectionSummary");
const emptyStateEl        = document.getElementById("emptyState");

// ── Utilities ──────────────────────────────────────────────────────────────
function setHoldingsStatus(msg, isError = false) {
  holdingsStatusEl.textContent = msg;
  holdingsStatusEl.style.color = isError ? "#c62828" : "";
}

function computeAssetSummary(assets) {
  return assets.reduce((acc, a) => {
    const mv = Number(a.marketValue ?? a.quantity * a.currentPrice);
    const cost = Number(a.quantity * a.purchasePrice);
    acc.totalValue += mv;
    acc.totalCost  += cost;
    acc.totalPL    += Number(a.profitLoss ?? mv - cost);
    return acc;
  }, { totalValue: 0, totalCost: 0, totalPL: 0 });
}

function formatDate(dateStr) {
  if (!dateStr) return "—";
  const d = new Date(dateStr);
  return isNaN(d) ? dateStr : d.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });
}

// ── Portfolio strip ────────────────────────────────────────────────────────
async function loadAllPortfolioSummaries() {
  portfolioStripEl.innerHTML = "";

  for (const portfolio of hPortfolios) {
    let assets = [];
    try {
      assets = await apiGet(`/api/assets?portfolioId=${portfolio.id}`);
    } catch {
      assets = fallbackData.assetsByPortfolio[portfolio.id] || [];
    }

    const { totalValue, totalCost, totalPL } = computeAssetSummary(assets);
    const retPct = totalCost > 0 ? (totalPL / totalCost) * 100 : 0;
    const isActive = String(portfolio.id) === String(hActivePortfolioId);

    const card = document.createElement("div");
    card.className = `portfolio-card${isActive ? " active" : ""}`;
    card.dataset.portfolioId = portfolio.id;
    card.innerHTML = `
      <div class="portfolio-card__name">${portfolio.name}</div>
      <div class="portfolio-card__value">${formatCurrency(totalValue)}</div>
      <div class="portfolio-card__return ${totalPL >= 0 ? "positive" : "negative"}">
        ${totalPL >= 0 ? "▲" : "▼"} ${retPct.toFixed(2)}%
      </div>
    `;
    card.addEventListener("click", () => switchPortfolio(portfolio.id));
    portfolioStripEl.appendChild(card);
  }
}

function updateStripActiveState() {
  document.querySelectorAll(".portfolio-card").forEach(card => {
    card.classList.toggle("active", String(card.dataset.portfolioId) === String(hActivePortfolioId));
  });
}

// ── Holdings table ─────────────────────────────────────────────────────────
function applyFiltersAndRender() {
  const query   = searchInputEl.value.trim().toLowerCase();
  const sort    = sortSelectEl.value;
  const plFilt  = plFilterEl.value;
  const minVal  = parseFloat(minValueEl.value) || 0;

  let assets = [...hAllAssets];

  // search
  if (query) {
    assets = assets.filter(a =>
      a.ticker.toLowerCase().includes(query) ||
      a.companyName.toLowerCase().includes(query)
    );
  }

  // p/l filter
  if (plFilt === "positive") {
    assets = assets.filter(a => {
      const pl = Number(a.profitLoss ?? (a.quantity * a.currentPrice) - (a.quantity * a.purchasePrice));
      return pl >= 0;
    });
  } else if (plFilt === "negative") {
    assets = assets.filter(a => {
      const pl = Number(a.profitLoss ?? (a.quantity * a.currentPrice) - (a.quantity * a.purchasePrice));
      return pl < 0;
    });
  }

  // min market value
  if (minVal > 0) {
    assets = assets.filter(a => {
      const mv = Number(a.marketValue ?? a.quantity * a.currentPrice);
      return mv >= minVal;
    });
  }

  // sort
  assets.sort((a, b) => {
    switch (sort) {
      case "ticker":      return a.ticker.localeCompare(b.ticker);
      case "ticker-desc": return b.ticker.localeCompare(a.ticker);
      case "value-desc":  return (b.marketValue ?? 0) - (a.marketValue ?? 0);
      case "value-asc":   return (a.marketValue ?? 0) - (b.marketValue ?? 0);
      case "pl-desc": {
        const plA = Number(a.profitLoss ?? (a.quantity * a.currentPrice) - (a.quantity * a.purchasePrice));
        const plB = Number(b.profitLoss ?? (b.quantity * b.currentPrice) - (b.quantity * b.purchasePrice));
        return plB - plA;
      }
      case "pl-asc": {
        const plA = Number(a.profitLoss ?? (a.quantity * a.currentPrice) - (a.quantity * a.purchasePrice));
        const plB = Number(b.profitLoss ?? (b.quantity * b.currentPrice) - (b.quantity * b.purchasePrice));
        return plA - plB;
      }
      case "date-desc": return new Date(b.datePurchased || 0) - new Date(a.datePurchased || 0);
      case "date-asc":  return new Date(a.datePurchased || 0) - new Date(b.datePurchased || 0);
      default: return 0;
    }
  });

  hFilteredAssets = assets;
  renderHoldingsTableH(assets);
}

function renderHoldingsTableH(assets) {
  holdingsBodyEl.innerHTML = "";
  holdingsCountEl.textContent = assets.length;

  if (!assets.length) {
    emptyStateEl.style.display = "block";
    selectAllEl.checked = false;
    selectAllEl.indeterminate = false;
    updateSelectionSummary();
    return;
  }

  emptyStateEl.style.display = "none";

  assets.forEach(item => {
    const mv = Number(item.marketValue ?? item.quantity * item.currentPrice);
    const pl = Number(item.profitLoss ?? mv - item.quantity * item.purchasePrice);
    const isSelected = hSelectedAssetIds.has(item.id);

    const tr = document.createElement("tr");
    if (isSelected) tr.classList.add("row-selected");

    tr.innerHTML = `
      <td><input type="checkbox" data-asset-id="${item.id}" ${isSelected ? "checked" : ""} /></td>
      <td><strong>${item.ticker}</strong></td>
      <td>${item.companyName}</td>
      <td>${item.quantity.toLocaleString()}</td>
      <td>${formatCurrency(item.currentPrice)}</td>
      <td>${formatCurrency(item.purchasePrice)}</td>
      <td>${formatCurrency(mv)}</td>
      <td>${formatDate(item.datePurchased)}</td>
      <td class="${pl >= 0 ? "pl-positive" : "pl-negative"}">${pl >= 0 ? "+" : ""}${formatCurrency(pl)}</td>
    `;

    const checkbox = tr.querySelector("input[type='checkbox']");
    checkbox.addEventListener("change", () => {
      if (checkbox.checked) {
        hSelectedAssetIds.add(item.id);
        tr.classList.add("row-selected");
      } else {
        hSelectedAssetIds.delete(item.id);
        tr.classList.remove("row-selected");
      }
      updateSelectAllState();
      updateSelectionSummary();
    });

    holdingsBodyEl.appendChild(tr);
  });

  updateSelectAllState();
  updateSelectionSummary();
}

function updateSelectAllState() {
  const visibleIds = hFilteredAssets.map(a => a.id);
  const selectedVisible = visibleIds.filter(id => hSelectedAssetIds.has(id));
  if (selectedVisible.length === 0) {
    selectAllEl.checked = false;
    selectAllEl.indeterminate = false;
  } else if (selectedVisible.length === visibleIds.length) {
    selectAllEl.checked = true;
    selectAllEl.indeterminate = false;
  } else {
    selectAllEl.checked = false;
    selectAllEl.indeterminate = true;
  }
}

function updateSelectionSummary() {
  const count = hSelectedAssetIds.size;
  if (count === 0) {
    selectionSummaryEl.innerHTML = `<p class="selection-hint">Select rows to enable stock actions.</p>`;
  } else {
    const selectedAssets = hAllAssets.filter(a => hSelectedAssetIds.has(a.id));
    const totalMv = selectedAssets.reduce((sum, a) => sum + Number(a.marketValue ?? a.quantity * a.currentPrice), 0);
    selectionSummaryEl.innerHTML = `
      <p class="selection-info">
        <strong>${count}</strong> holding${count > 1 ? "s" : ""} selected<br/>
        Total value: <strong>${formatCurrency(totalMv)}</strong>
      </p>
    `;
  }
}

// ── Portfolio switching ────────────────────────────────────────────────────
async function switchPortfolio(portfolioId) {
  hActivePortfolioId = portfolioId;
  hSelectedAssetIds.clear();
  updateStripActiveState();
  setHoldingsStatus("Loading holdings…");

  try {
    const assets = await apiGet(`/api/assets?portfolioId=${portfolioId}`);
    hAllAssets = assets;
    setHoldingsStatus(`${assets.length} holding${assets.length !== 1 ? "s" : ""} loaded.`);
  } catch (err) {
    hAllAssets = fallbackData.assetsByPortfolio[portfolioId] || [];
    setHoldingsStatus(`Using demo data: ${err.message}`, true);
  }

  applyFiltersAndRender();
}

// ── Modal helpers ──────────────────────────────────────────────────────────
function openModal(id) {
  document.getElementById(id).classList.add("open");
}

function closeModal(id) {
  document.getElementById(id).classList.remove("open");
}

function clearError(id) {
  const el = document.getElementById(id);
  if (el) el.textContent = "";
}

function showError(id, msg) {
  const el = document.getElementById(id);
  if (el) el.textContent = msg;
}

function populatePortfolioDropdowns() {
  ["addPortfolioSelect", "updatePortfolioSelect", "removePortfolioSelect"].forEach(selId => {
    const sel = document.getElementById(selId);
    if (!sel) return;
    sel.innerHTML = hPortfolios.map(p => `<option value="${p.id}">${p.name}</option>`).join("");
  });
}

// ── CRUD handlers (wired to backend; fallback to local state for demo) ─────

// Add stock
document.getElementById("btnAddStock").addEventListener("click", () => {
  clearError("addStockError");
  document.getElementById("addTicker").value = "";
  document.getElementById("addCompanyName").value = "";
  document.getElementById("addQuantity").value = "";
  document.getElementById("addPurchasePrice").value = "";
  document.getElementById("addCurrentPrice").value = "";
  document.getElementById("addDatePurchased").value = new Date().toISOString().slice(0, 10);
  populatePortfolioDropdowns();
  // Pre-select active portfolio
  const sel = document.getElementById("addPortfolioSelect");
  if (hActivePortfolioId) sel.value = hActivePortfolioId;
  openModal("modalAddStock");
});

document.getElementById("btnConfirmAddStock").addEventListener("click", async () => {
  clearError("addStockError");
  const ticker     = document.getElementById("addTicker").value.trim().toUpperCase();
  const company    = document.getElementById("addCompanyName").value.trim();
  const quantity   = parseFloat(document.getElementById("addQuantity").value);
  const purchase   = parseFloat(document.getElementById("addPurchasePrice").value);
  const current    = parseFloat(document.getElementById("addCurrentPrice").value) || purchase;
  const date       = document.getElementById("addDatePurchased").value;
  const portfolioId = parseInt(document.getElementById("addPortfolioSelect").value, 10);

  if (!ticker || !company || isNaN(quantity) || isNaN(purchase) || !date) {
    showError("addStockError", "Please fill in all required fields.");
    return;
  }

  const payload = { ticker, companyName: company, quantity, purchasePrice: purchase, currentPrice: current, datePurchased: date, portfolioId };

  try {
    await apiPost("/api/assets", payload);
  } catch {
    // Fallback: add to local demo state
    const newId = Date.now();
    const newAsset = {
      id: newId,
      ...payload,
      marketValue: quantity * current,
      profitLoss: quantity * (current - purchase)
    };
    if (!fallbackData.assetsByPortfolio[portfolioId]) fallbackData.assetsByPortfolio[portfolioId] = [];
    fallbackData.assetsByPortfolio[portfolioId].push(newAsset);
  }

  closeModal("modalAddStock");
  if (String(portfolioId) === String(hActivePortfolioId)) {
    await switchPortfolio(hActivePortfolioId);
  }
  await loadAllPortfolioSummaries();
});

// Update stock
document.getElementById("btnUpdateStock").addEventListener("click", () => {
  if (hSelectedAssetIds.size === 0) {
    alert("Please select a holding to update.");
    return;
  }
  if (hSelectedAssetIds.size > 1) {
    alert("Please select only one holding to update.");
    return;
  }
  const [assetId] = [...hSelectedAssetIds];
  const asset = hAllAssets.find(a => a.id === assetId);
  if (!asset) return;

  clearError("updateStockError");
  document.getElementById("updateStockId").value = asset.id;
  document.getElementById("updateTicker").value = asset.ticker;
  document.getElementById("updateCompanyName").value = asset.companyName;
  document.getElementById("updateQuantity").value = asset.quantity;
  document.getElementById("updatePurchasePrice").value = asset.purchasePrice;
  document.getElementById("updateCurrentPrice").value = asset.currentPrice;
  document.getElementById("updateDatePurchased").value = asset.datePurchased || "";
  openModal("modalUpdateStock");
});

document.getElementById("btnConfirmUpdateStock").addEventListener("click", async () => {
  clearError("updateStockError");
  const id       = document.getElementById("updateStockId").value;
  const ticker   = document.getElementById("updateTicker").value.trim().toUpperCase();
  const company  = document.getElementById("updateCompanyName").value.trim();
  const quantity = parseFloat(document.getElementById("updateQuantity").value);
  const purchase = parseFloat(document.getElementById("updatePurchasePrice").value);
  const current  = parseFloat(document.getElementById("updateCurrentPrice").value);
  const date     = document.getElementById("updateDatePurchased").value;

  if (!ticker || !company || isNaN(quantity) || isNaN(purchase) || isNaN(current)) {
    showError("updateStockError", "Please fill in all required fields.");
    return;
  }

  const payload = { ticker, companyName: company, quantity, purchasePrice: purchase, currentPrice: current, datePurchased: date };

  try {
    await apiPut(`/api/assets/${id}`, payload);
  } catch {
    // Fallback: update local demo state
    const pool = fallbackData.assetsByPortfolio[hActivePortfolioId] || [];
    const idx = pool.findIndex(a => String(a.id) === String(id));
    if (idx >= 0) {
      pool[idx] = { ...pool[idx], ...payload, marketValue: quantity * current, profitLoss: quantity * (current - purchase) };
    }
  }

  closeModal("modalUpdateStock");
  await switchPortfolio(hActivePortfolioId);
  await loadAllPortfolioSummaries();
});

// Remove stock
document.getElementById("btnRemoveStock").addEventListener("click", () => {
  if (hSelectedAssetIds.size === 0) {
    alert("Please select at least one holding to remove.");
    return;
  }
  clearError("removeStockError");
  const list = document.getElementById("removeStockList");
  list.innerHTML = "";
  hAllAssets
    .filter(a => hSelectedAssetIds.has(a.id))
    .forEach(a => {
      const li = document.createElement("li");
      li.textContent = `${a.ticker} — ${a.companyName}`;
      list.appendChild(li);
    });
  openModal("modalRemoveStock");
});

document.getElementById("btnConfirmRemoveStock").addEventListener("click", async () => {
  clearError("removeStockError");
  const ids = [...hSelectedAssetIds];

  for (const id of ids) {
    try {
      await apiDelete(`/api/assets/${id}`);
    } catch {
      // Fallback: remove from local demo state
      const pool = fallbackData.assetsByPortfolio[hActivePortfolioId];
      if (pool) {
        const idx = pool.findIndex(a => a.id === id);
        if (idx >= 0) pool.splice(idx, 1);
      }
    }
  }

  hSelectedAssetIds.clear();
  closeModal("modalRemoveStock");
  await switchPortfolio(hActivePortfolioId);
  await loadAllPortfolioSummaries();
});

// Add portfolio
document.getElementById("btnAddPortfolio").addEventListener("click", () => {
  clearError("addPortfolioError");
  document.getElementById("addPortfolioName").value = "";
  openModal("modalAddPortfolio");
});

document.getElementById("btnConfirmAddPortfolio").addEventListener("click", async () => {
  clearError("addPortfolioError");
  const name = document.getElementById("addPortfolioName").value.trim();
  if (!name) { showError("addPortfolioError", "Portfolio name is required."); return; }

  try {
    const created = await apiPost("/api/portfolios", { name });
    hPortfolios.push(created);
    fallbackData.portfolios.push(created);
    fallbackData.assetsByPortfolio[created.id] = [];
  } catch {
    const newId = Date.now();
    const newPortfolio = { id: newId, name };
    hPortfolios.push(newPortfolio);
    fallbackData.portfolios.push(newPortfolio);
    fallbackData.assetsByPortfolio[newId] = [];
  }

  closeModal("modalAddPortfolio");
  populatePortfolioDropdowns();
  await loadAllPortfolioSummaries();
});

// Update portfolio
document.getElementById("btnUpdatePortfolio").addEventListener("click", () => {
  clearError("updatePortfolioError");
  populatePortfolioDropdowns();
  document.getElementById("updatePortfolioName").value = "";
  openModal("modalUpdatePortfolio");
});

document.getElementById("btnConfirmUpdatePortfolio").addEventListener("click", async () => {
  clearError("updatePortfolioError");
  const id   = parseInt(document.getElementById("updatePortfolioSelect").value, 10);
  const name = document.getElementById("updatePortfolioName").value.trim();
  if (!name) { showError("updatePortfolioError", "New name is required."); return; }

  try {
    await apiPut(`/api/portfolios/${id}`, { name });
  } catch { /* ignore */ }

  // Update local state regardless
  const p = hPortfolios.find(x => x.id === id);
  if (p) p.name = name;
  const fp = fallbackData.portfolios.find(x => x.id === id);
  if (fp) fp.name = name;

  closeModal("modalUpdatePortfolio");
  await loadAllPortfolioSummaries();
});

// Remove portfolio
document.getElementById("btnRemovePortfolio").addEventListener("click", () => {
  clearError("removePortfolioError");
  populatePortfolioDropdowns();
  openModal("modalRemovePortfolio");
});

document.getElementById("btnConfirmRemovePortfolio").addEventListener("click", async () => {
  clearError("removePortfolioError");
  const id = parseInt(document.getElementById("removePortfolioSelect").value, 10);

  try {
    await apiDelete(`/api/portfolios/${id}`);
  } catch { /* ignore */ }

  hPortfolios = hPortfolios.filter(p => p.id !== id);
  fallbackData.portfolios = fallbackData.portfolios.filter(p => p.id !== id);
  delete fallbackData.assetsByPortfolio[id];

  if (String(hActivePortfolioId) === String(id)) {
    hActivePortfolioId = hPortfolios[0]?.id ?? null;
  }

  closeModal("modalRemovePortfolio");
  await loadAllPortfolioSummaries();
  if (hActivePortfolioId) await switchPortfolio(hActivePortfolioId);
});

// ── Close modal handlers ───────────────────────────────────────────────────
document.querySelectorAll("[data-close]").forEach(btn => {
  btn.addEventListener("click", () => closeModal(btn.dataset.close));
});

document.querySelectorAll(".modal-overlay").forEach(overlay => {
  overlay.addEventListener("click", e => {
    if (e.target === overlay) closeModal(overlay.id);
  });
});

document.addEventListener("keydown", e => {
  if (e.key === "Escape") {
    document.querySelectorAll(".modal-overlay.open").forEach(m => closeModal(m.id));
  }
});

// ── Select all checkbox ────────────────────────────────────────────────────
selectAllEl.addEventListener("change", () => {
  if (selectAllEl.checked) {
    hFilteredAssets.forEach(a => hSelectedAssetIds.add(a.id));
  } else {
    hFilteredAssets.forEach(a => hSelectedAssetIds.delete(a.id));
  }
  renderHoldingsTableH(hFilteredAssets);
});

// ── Filter event listeners ─────────────────────────────────────────────────
searchInputEl.addEventListener("input", applyFiltersAndRender);
sortSelectEl.addEventListener("change", applyFiltersAndRender);
plFilterEl.addEventListener("change", applyFiltersAndRender);
minValueEl.addEventListener("input", applyFiltersAndRender);

clearFiltersEl.addEventListener("click", () => {
  searchInputEl.value = "";
  sortSelectEl.value = "ticker";
  plFilterEl.value = "all";
  minValueEl.value = "";
  applyFiltersAndRender();
});

// ── HTTP helpers (POST, PUT, DELETE) ──────────────────────────────────────
const API_BASE_H = window.location.origin;

async function apiPost(path, body) {
  const res = await fetch(`${API_BASE_H}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error(`POST ${path} failed: ${res.status}`);
  return res.json();
}

async function apiPut(path, body) {
  const res = await fetch(`${API_BASE_H}${path}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  if (!res.ok) throw new Error(`PUT ${path} failed: ${res.status}`);
  return res.json().catch(() => ({}));
}

async function apiDelete(path) {
  const res = await fetch(`${API_BASE_H}${path}`, { method: "DELETE" });
  if (!res.ok) throw new Error(`DELETE ${path} failed: ${res.status}`);
}

// ── Init ───────────────────────────────────────────────────────────────────
async function holdingsInit() {
  setHoldingsStatus("Loading portfolios…");

  try {
    hPortfolios = await apiGet("/api/portfolios");
  } catch {
    hPortfolios = [...fallbackData.portfolios];
    setHoldingsStatus("Using demo data — backend unavailable.", true);
  }

  if (!hPortfolios.length) {
    setHoldingsStatus("No portfolios found.");
    return;
  }

  hActivePortfolioId = hPortfolios[0].id;

  // Load portfolio strip (summary cards) — async, non-blocking
  loadAllPortfolioSummaries();

  // Load first portfolio's holdings
  await switchPortfolio(hActivePortfolioId);
}

holdingsInit();
