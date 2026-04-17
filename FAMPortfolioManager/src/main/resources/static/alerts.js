/* ============================================================
   alerts.js — Consolidated Portfolio Alert System
   
   HOW IT WORKS
   ─────────────────────────────────────────────────────────────
   After app.js has loaded all portfolio + asset data, this file:

     1. Runs a set of plain if/else RULES against that data.
     2. Builds an array of alert objects { type, label, message }.
     3. Displays them in the #alertBanner carousel at the top of
        the dashboard.

   ALERT TYPES
   ─────────────────────────────────────────────────────────────
   This system generates:
     • NEGATIVE WARNINGS (danger/warning): Portfolio losses, 
       underperforming stocks, concentration risk, low diversification
     • ONE TOP PERFORMER ALERT (success): The single best-performing
       individual stock across all portfolios (if above threshold)
     
   No neutral/positive messages are generated (all-clear, portfolio gains, etc.)

   Alert Types:
     "warning"  → amber   (caution / worth watching)
     "danger"   → red     (significant loss / action needed)
     "success"  → green   (best performer / positive signal)
   ============================================================ */

// ── Thresholds — edit these to tune alert sensitivity ─────────────────────
const ALERT_THRESHOLDS = {
  majorLossPct:          -15,   // % return that triggers a "major loss" danger alert
  moderateLossPct:        -5,   // % return considered a moderate loss
  singleStockDominance:   50,   // % of portfolio in one stock → concentration warning
  stockLossHighlight:    -10,   // individual stock return to flag as a loser
  stockGainHighlight:     20,   // individual stock return to highlight as top performer
};

// ── Alert object factory ───────────────────────────────────────────────────
function createAlert(type, label, message) {
  return { type, label, message };
}

// ── Core rule engine ───────────────────────────────────────────────────────
/**
 * Evaluates the current portfolio state and returns an array of alerts.
 * Only generates:
 *   1. Negative warnings (losses, concentration risk, low diversification)
 *   2. One top performer alert (individual stock with highest gain)
 *
 * @param {Array}  portfolios          - normalized portfolio objects
 * @param {Object} assetsByPortfolio   - map of portfolioId → asset[]
 * @returns {Array} alerts
 */
function evaluateAlerts(portfolios, assetsByPortfolio) {
  const alerts = [];

  // Guard: nothing to analyse
  if (!portfolios || portfolios.length === 0) {
    return alerts; // silently return empty
  }

  // ── Track top performer across all assets ───────────────────────────────
  let topPerformer = null;
  let topPerformerReturn = -Infinity;
  let topPerformerPortfolio = null;

  // ── Aggregate across ALL portfolios ──────────────────────────────────────
  const allAssets = Object.values(assetsByPortfolio).flat();
  const totalMarketValue = allAssets.reduce((s, a) => s + a.marketValue, 0);
  const totalCostBasis   = allAssets.reduce((s, a) => s + a.quantity * a.purchasePrice, 0);
  const totalProfitLoss  = totalMarketValue - totalCostBasis;
  const overallReturn    = totalCostBasis > 0
    ? (totalProfitLoss / totalCostBasis) * 100
    : 0;

  // ── RULE 1: Overall portfolio major loss ────────────────────────────────
  if (overallReturn <= ALERT_THRESHOLDS.majorLossPct) {
    alerts.push(createAlert(
      "danger",
      "Significant Loss",
      `Your total portfolio is down ${overallReturn.toFixed(1)}%. ` +
      `Review underperforming holdings and consider rebalancing.`
    ));

  // ── RULE 2: Overall portfolio moderate loss ─────────────────────────────
  } else if (overallReturn < ALERT_THRESHOLDS.moderateLossPct) {
    alerts.push(createAlert(
      "warning",
      "Portfolio Down",
      `Your overall return is ${overallReturn.toFixed(1)}%. ` +
      `Some positions are underperforming — monitor closely.`
    ));
  }

  // ── Per-portfolio rules ───────────────────────────────────────────────────
  for (const portfolio of portfolios) {
    const assets = assetsByPortfolio[portfolio.id] || [];
    if (assets.length === 0) continue;

    const portValue    = assets.reduce((s, a) => s + a.marketValue, 0);
    const portCost     = assets.reduce((s, a) => s + a.quantity * a.purchasePrice, 0);
    const portPL       = portValue - portCost;
    const portReturn   = portCost > 0 ? (portPL / portCost) * 100 : 0;

    // ── RULE 3: Individual portfolio major loss ──────────────────────────────
    if (portReturn <= ALERT_THRESHOLDS.majorLossPct) {
      alerts.push(createAlert(
        "danger",
        `${portfolio.name} — Loss Alert`,
        `${portfolio.name} is down ${portReturn.toFixed(1)}% ` +
        `(${formatAlertCurrency(portPL)}). Immediate review recommended.`
      ));

    // ── RULE 4: Individual portfolio moderate loss ───────────────────────────
    } else if (portReturn < ALERT_THRESHOLDS.moderateLossPct) {
      alerts.push(createAlert(
        "warning",
        `${portfolio.name} — Underperforming`,
        `${portfolio.name} is down ${portReturn.toFixed(1)}%. ` +
        `Check for drag from individual holdings.`
      ));
    }

    // ── RULE 5: Concentration risk — single stock dominates portfolio ────────
    if (portValue > 0) {
      for (const asset of assets) {
        const weight = (asset.marketValue / portValue) * 100;
        if (weight >= ALERT_THRESHOLDS.singleStockDominance) {
          alerts.push(createAlert(
            "warning",
            "Concentration Risk",
            `${asset.ticker} makes up ${weight.toFixed(0)}% of ${portfolio.name}. ` +
            `High concentration in one stock increases volatility risk.`
          ));
          break; // one concentration warning per portfolio is enough
        }
      }
    }

    // ── RULE 6: Low diversification ────────────────────────────────────────
    if (assets.length <= 2) {
      alerts.push(createAlert(
        "warning",
        "Low Diversification",
        `${portfolio.name} holds only ${assets.length} stock${assets.length === 1 ? "" : "s"}. ` +
        `Consider broadening to reduce single-stock exposure.`
      ));
    }

    // ── Per-asset rules: Track top performer & losses ──────────────────────
    for (const asset of assets) {
      const assetCost   = asset.quantity * asset.purchasePrice;
      const assetReturn = assetCost > 0
        ? ((asset.marketValue - assetCost) / assetCost) * 100
        : 0;

      // ── Track top performer (highest individual stock gain) ──────────────
      if (assetReturn > topPerformerReturn) {
        topPerformer = asset;
        topPerformerReturn = assetReturn;
        topPerformerPortfolio = portfolio;
      }

      // ── RULE 7: Struggling individual stock (only show losses) ────────────
      if (assetReturn <= ALERT_THRESHOLDS.stockLossHighlight) {
        alerts.push(createAlert(
          "danger",
          `${asset.ticker} — Loss Warning`,
          `${asset.ticker} (${asset.companyName}) is down ${assetReturn.toFixed(1)}% ` +
          `in ${portfolio.name} (${formatAlertCurrency(asset.profitLoss)}). ` +
          `Review your position.`
        ));
      }
    }
  }

  // ── RULE 8: Add top performer alert if above threshold ───────────────────
  if (topPerformer && topPerformerReturn >= ALERT_THRESHOLDS.stockGainHighlight) {
    alerts.push(createAlert(
      "success",
      `${topPerformer.ticker} — Top Performer`,
      `${topPerformer.ticker} (${topPerformer.companyName}) is your best performer, ` +
      `up ${topPerformerReturn.toFixed(1)}% in ${topPerformerPortfolio.name}, ` +
      `adding ${formatAlertCurrency(topPerformer.profitLoss)} to your portfolio.`
    ));
  }

  return alerts;
}

// ── Currency formatter (standalone so alerts.js has no hard dependency) ───
function formatAlertCurrency(value) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 0
  }).format(value);
}

// ── Banner controller ──────────────────────────────────────────────────────
const AlertBanner = (() => {
  const banner      = document.getElementById("alertBanner");
  const iconEl      = document.getElementById("alertIcon");
  const labelEl     = document.getElementById("alertLabel");
  const messageEl   = document.getElementById("alertMessage");
  const dotsEl      = document.getElementById("alertDots");
  const counterEl   = document.getElementById("alertCounter");
  const prevBtn     = document.getElementById("alertPrev");
  const nextBtn     = document.getElementById("alertNext");
  const dismissBtn  = document.getElementById("alertDismiss");

  let _alerts = [];
  let _current = 0;
  let _cycleTimer = null;

  // Map alert type → { icon, CSS modifier class }
  const TYPE_CONFIG = {
    danger:  { icon: "⚠",  mod: "alert-banner--danger"  },
    warning: { icon: "🔔", mod: "alert-banner--warning" },
    success: { icon: "✅", mod: "alert-banner--success" },
    info:    { icon: "💡", mod: "alert-banner--info"    }
  };

  function _clearTypeClasses() {
    Object.values(TYPE_CONFIG).forEach(c => banner.classList.remove(c.mod));
  }

  function _buildDots(count, activeIndex) {
    dotsEl.innerHTML = "";
    if (count <= 1) return;
    for (let i = 0; i < count; i++) {
      const dot = document.createElement("button");
      dot.className = "alert-dot" + (i === activeIndex ? " alert-dot--active" : "");
      dot.setAttribute("aria-label", `Alert ${i + 1}`);
      dot.addEventListener("click", () => show(i));
      dotsEl.appendChild(dot);
    }
  }

  function show(index) {
    if (!_alerts.length) return;
    _current = (index + _alerts.length) % _alerts.length;

    const alert = _alerts[_current];
    const config = TYPE_CONFIG[alert.type] || TYPE_CONFIG.info;

    _clearTypeClasses();
    banner.classList.add(config.mod);

    iconEl.textContent    = config.icon;
    labelEl.textContent   = alert.label;
    messageEl.textContent = alert.message;
    counterEl.textContent = _alerts.length > 1 ? `${_current + 1} / ${_alerts.length}` : "";

    prevBtn.style.visibility = _alerts.length > 1 ? "visible" : "hidden";
    nextBtn.style.visibility = _alerts.length > 1 ? "visible" : "hidden";

    _buildDots(_alerts.length, _current);
  }

  function _startCycle() {
    // Auto-advance every 8 seconds if there are multiple alerts
    _stopCycle();
    if (_alerts.length > 1) {
      _cycleTimer = setInterval(() => show(_current + 1), 8000);
    }
  }

  function _stopCycle() {
    if (_cycleTimer) {
      clearInterval(_cycleTimer);
      _cycleTimer = null;
    }
  }

  // Wire up nav buttons
  prevBtn.addEventListener("click", () => {
    _stopCycle();
    show(_current - 1);
    _startCycle();
  });

  nextBtn.addEventListener("click", () => {
    _stopCycle();
    show(_current + 1);
    _startCycle();
  });

  dismissBtn.addEventListener("click", () => {
    _stopCycle();
    banner.classList.add("alert-banner--hidden");
    banner.classList.remove("alert-banner--visible");
  });

  // Pause auto-cycle on hover
  banner.addEventListener("mouseenter", _stopCycle);
  banner.addEventListener("mouseleave", _startCycle);

  // Public API
  return {
    /**
     * Load a fresh set of alerts and display the banner.
     * @param {Array} alerts
     */
    load(alerts) {
      _alerts = alerts;
      _current = 0;

      if (!alerts.length) {
        banner.classList.add("alert-banner--hidden");
        banner.classList.remove("alert-banner--visible");
        return;
      }

      // Most severe alert first: danger > warning > success > info
      const priority = { danger: 0, warning: 1, success: 2, info: 3 };
      _alerts.sort((a, b) => (priority[a.type] ?? 99) - (priority[b.type] ?? 99));

      show(0);

      banner.classList.remove("alert-banner--hidden");
      // Trigger reflow so the CSS transition fires
      void banner.offsetWidth;
      banner.classList.add("alert-banner--visible");

      _startCycle();
    }
  };
})();

// ── Hook into app.js init cycle ───────────────────────────────────────────
/**
 * Called once app.js has finished loading data and rendered the dashboard.
 * We read the same in-memory variables that app.js populates so there is
 * zero extra network cost.
 *
 * app.js exposes:
 *   portfolios          — Array of portfolio objects
 *   assetsByPortfolio   — Object keyed by portfolio id → asset[]
 *
 * These are declared with `let` in app.js scope, which is the same global
 * scope in a classic script context, so alerts.js can read them directly.
 */
function runAlerts() {
  // Small guard: wait a tick so app.js's async init() has fully resolved
  // before we read the shared state variables.
  setTimeout(() => {
    try {
      const alerts = evaluateAlerts(
        typeof portfolios !== "undefined" ? portfolios : [],
        typeof assetsByPortfolio !== "undefined" ? assetsByPortfolio : {}
      );
      AlertBanner.load(alerts);
    } catch (err) {
      console.warn("Alert engine error:", err);
    }
  }, 300);
}

// Kick off as soon as the DOM (and app.js) have finished loading.
if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", runAlerts);
} else {
  runAlerts();
}
