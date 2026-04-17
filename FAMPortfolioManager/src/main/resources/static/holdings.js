/* ============================================================
   holdings.js — Holdings page logic
  Depends on: app.js (for apiGet/apiPost/apiPut/apiDelete, formatCurrency)

  Backend integration in this page:
  - GET    /api/portfolios
  - GET    /api/assets?portfolioId={id}
  - POST   /api/assets
  - PUT    /api/assets/{id}
  - DELETE /api/assets/{id}
  - POST   /api/portfolios
  - PUT    /api/portfolios/{id}
  - DELETE /api/portfolios/{id}

  Primary goal:
  Keep holdings management (browse/filter/select + CRUD) synchronized with
  backend data while maintaining immediate UI feedback.
   ============================================================ */

// ── State ──────────────────────────────────────────────────────────────────
let hPortfolios = [];           // all portfolios
let hAllAssets = [];            // all assets for active portfolio (raw)
let hFilteredAssets = [];       // after search/filter
let hActivePortfolioId = null;  // currently selected portfolio id
let hSelectedAssetIds = new Set();

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

// Summarize a set of assets for card/header style displays.
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

// Render purchase date safely for table display.
function formatDate(dateStr) {
  if (!dateStr) return "—";
  const d = new Date(dateStr);
  return isNaN(d) ? dateStr : d.toLocaleDateString("en-US", { year: "numeric", month: "short", day: "numeric" });
}

// ── Portfolio strip ────────────────────────────────────────────────────────
async function loadAllPortfolioSummaries() {
  portfolioStripEl.innerHTML = "";

  for (const portfolio of hPortfolios) {
    // Pull holdings per portfolio so each card reflects live backend totals.
    let assets = [];
    try {
      assets = await apiGet(`/api/assets?portfolioId=${portfolio.id}`);
    } catch {
      assets = [];
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

  // Text search across ticker/company (client-side; no backend roundtrip).
  if (query) {
    assets = assets.filter(a =>
      a.ticker.toLowerCase().includes(query) ||
      a.companyName.toLowerCase().includes(query)
    );
  }

  // Profit/loss polarity filter.
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

  // Minimum market value threshold.
  if (minVal > 0) {
    assets = assets.filter(a => {
      const mv = Number(a.marketValue ?? a.quantity * a.currentPrice);
      return mv >= minVal;
    });
  }

  // Client-side sort for currently loaded holdings.
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

  // Empty-state handling when filters hide all rows or portfolio has no assets.
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

    // Row-level selection drives bulk operations (delete) and single-update flow.
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

// Selection summary helps user validate action scope before update/delete.
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
    // Core holdings fetch endpoint for the active portfolio.
    const assets = await apiGet(`/api/assets?portfolioId=${portfolioId}`);
    hAllAssets = assets;
    setHoldingsStatus(`${assets.length} holding${assets.length !== 1 ? "s" : ""} loaded.`);
  } catch (err) {
    hAllAssets = [];
    setHoldingsStatus(`Failed to load holdings: ${err.message}`, true);
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

// ── CRUD handlers (backend-backed workflows) ───────────────────────────────

// Add stock
document.getElementById("btnAddStock").addEventListener("click", () => {
  clearError("addStockError");
  document.getElementById("addTicker").value = "";
  document.getElementById("addCompanyName").value = "";
  document.getElementById("addQuantity").value = "";
  document.getElementById("addPurchasePrice").value = "";
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
  const date       = document.getElementById("addDatePurchased").value;
  const portfolioId = parseInt(document.getElementById("addPortfolioSelect").value, 10);

  if (!ticker || !company || isNaN(quantity) || isNaN(purchase) || !date || Number.isNaN(portfolioId)) {
    showError("addStockError", "Please fill in all required fields.");
    return;
  }

  // Matches backend AssetRequestDto shape.
  const payload = {
    ticker,
    companyName: company,
    quantity,
    purchasePrice: purchase,
    purchaseDate: toApiDateTime(date),
    portfolioId
  };

  try {
    await apiPost("/api/assets", payload);
  } catch (err) {
    showError("addStockError", `Could not add stock: ${err.message}`);
    return;
  }

  closeModal("modalAddStock");
  // Refresh current table when we add into the currently visible portfolio.
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
  document.getElementById("updateDatePurchased").value = (asset.datePurchased || "").toString().slice(0, 10);
  openModal("modalUpdateStock");
});

document.getElementById("btnConfirmUpdateStock").addEventListener("click", async () => {
  clearError("updateStockError");
  const id       = document.getElementById("updateStockId").value;
  const ticker   = document.getElementById("updateTicker").value.trim().toUpperCase();
  const company  = document.getElementById("updateCompanyName").value.trim();
  const quantity = parseFloat(document.getElementById("updateQuantity").value);
  const purchase = parseFloat(document.getElementById("updatePurchasePrice").value);
  const date     = document.getElementById("updateDatePurchased").value;

  if (!ticker || !company || isNaN(quantity) || isNaN(purchase)) {
    showError("updateStockError", "Please fill in all required fields.");
    return;
  }

  // PUT contract also carries portfolioId so backend can validate ownership.
  const payload = {
    ticker,
    companyName: company,
    quantity,
    purchasePrice: purchase,
    purchaseDate: toApiDateTime(date),
    portfolioId: Number(hActivePortfolioId)
  };

  try {
    await apiPut(`/api/assets/${id}`, payload);
  } catch (err) {
    showError("updateStockError", `Could not update stock: ${err.message}`);
    return;
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

  // Execute backend deletes one by one so we can stop at first failure.
  for (const id of ids) {
    try {
      await apiDelete(`/api/assets/${id}`);
    } catch (err) {
      showError("removeStockError", `Could not remove stock: ${err.message}`);
      return;
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
    // Description is optional in current UI; keep payload explicit.
    const created = await apiPost("/api/portfolios", { name, description: "" });
    hPortfolios.push(created);
  } catch (err) {
    showError("addPortfolioError", `Could not create portfolio: ${err.message}`);
    return;
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

  // Preserve existing description if UI only edits the portfolio name.
  const existing = hPortfolios.find(x => x.id === id);
  const description = existing?.description ?? "";

  try {
    await apiPut(`/api/portfolios/${id}`, { name, description });
  } catch (err) {
    showError("updatePortfolioError", `Could not update portfolio: ${err.message}`);
    return;
  }

  // Update local state after successful backend update
  const p = hPortfolios.find(x => x.id === id);
  if (p) p.name = name;

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
  } catch (err) {
    showError("removePortfolioError", `Could not remove portfolio: ${err.message}`);
    return;
  }

  hPortfolios = hPortfolios.filter(p => p.id !== id);

  // If the active portfolio was removed, gracefully fallback to first remaining.
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
// Bulk toggle only affects currently visible (filtered) holdings.
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

// ── Init ───────────────────────────────────────────────────────────────────
async function holdingsInit() {
  setHoldingsStatus("Loading portfolios…");

  try {
    hPortfolios = await apiGet("/api/portfolios");
  } catch (err) {
    hPortfolios = [];
    setHoldingsStatus(`Failed to load portfolios: ${err.message}`, true);
  }

  if (!hPortfolios.length) {
    setHoldingsStatus("No portfolios found.");
    return;
  }

  hActivePortfolioId = hPortfolios[0].id;

  // Load portfolio strip (summary cards) — async, non-blocking.
  // We intentionally do not await to keep first holdings table responsive.
  loadAllPortfolioSummaries();

  // Load first portfolio's holdings
  await switchPortfolio(hActivePortfolioId);
}

holdingsInit();
