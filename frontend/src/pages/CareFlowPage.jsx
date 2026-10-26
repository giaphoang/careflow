import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { useAuth } from "../context/AuthContext";
import { authApi } from "../lib/api";
import { allPages, careflow, fulfillmentKey } from "../lib/careflow";
import Icon from "../components/CareFlowIcon";
import "../careflow.css";

const stateLabels = {
  REQUESTED: "Awaiting review",
  APPROVED: "Approved",
  RESERVED: "Reserved",
  FULFILLED: "Fulfilled",
  REJECTED: "Rejected",
  CANCELLED: "Cancelled",
  EXPIRED: "Expired",
};
const departments = [
  "Cardiac services",
  "Surgical services",
  "Diabetes services",
  "Neuroscience",
  "Central supply",
];
const fmtDate = (value) =>
  new Date(value).toLocaleDateString(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
const fmtTime = (value) =>
  new Date(value).toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
function Badge({ state, children }) {
  return (
    <span className={`cf-badge ${state?.toLowerCase()}`}>
      <span />
      {children || stateLabels[state] || state}
    </span>
  );
}
function stockStatus(item) {
  return item.quarantined
    ? "Quarantined"
    : item.expiresOn <= new Date().toISOString().slice(0, 10)
      ? "Expired"
      : item.availableStock <= item.reorderPoint
        ? "Low stock"
        : "In stock";
}

function Modal({ title, subtitle, onClose, children }) {
  const ref = useRef(null);
  useEffect(() => {
    const prior = document.activeElement;
    const dialog = ref.current;
    dialog.showModal();
    const cancel = (e) => {
      e.preventDefault();
      onClose();
    };
    dialog.addEventListener("cancel", cancel);
    return () => {
      dialog.removeEventListener("cancel", cancel);
      dialog.close();
      prior?.focus();
    };
  }, []);
  return (
    <dialog className="cf-modal cf" ref={ref} aria-labelledby="cf-modal-title">
      <div className="cf-modal-heading">
        <div>
          <span className="cf-kicker">CAREFLOW WORKSPACE</span>
          <h2 id="cf-modal-title">{title}</h2>
          {subtitle && <p>{subtitle}</p>}
        </div>
        <button
          className="cf-icon-button"
          onClick={onClose}
          aria-label="Close dialog"
        >
          <Icon name="close" />
        </button>
      </div>
      {children}
    </dialog>
  );
}

export default function CareFlowPage() {
  const { token, role, username, logout } = useAuth();
  const [view, setView] = useState("Overview"),
    [items, setItems] = useState([]),
    [requests, setRequests] = useState([]),
    [audits, setAudits] = useState([]),
    [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [notice, setNotice] = useState("");
  const [search, setSearch] = useState(""),
    [filter, setFilter] = useState("ALL"),
    [page, setPage] = useState(0),
    [modal, setModal] = useState(null),
    [history, setHistory] = useState([]);
  const canManage = role === "ADMIN" || role === "INVENTORY_MANAGER";
  const canReview = role === "ADMIN" || role === "CLINICAL_STAFF";
  const canRequest = ["ADMIN", "REQUESTER", "CLINICAL_STAFF"].includes(role);
  const careflowRole = [
    "ADMIN",
    "REQUESTER",
    "CLINICAL_STAFF",
    "INVENTORY_MANAGER",
  ].includes(role);
  const reload = useCallback(async () => {
    if (!careflowRole) {
      setLoading(false);
      return;
    }
    const [nextItems, nextRequests] = await Promise.all([
      allPages(token, "/inventory"),
      allPages(token, "/requests"),
    ]);
    setItems(nextItems);
    setRequests(nextRequests);
    if (role === "ADMIN") {
      const [nextAudit, nextUsers] = await Promise.all([
        allPages(token, "/audit"),
        authApi.getUsers(token),
      ]);
      setAudits(nextAudit);
      setUsers(nextUsers);
    }
  }, [token, role, careflowRole]);
  useEffect(() => {
    reload()
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, [reload]);
  useEffect(() => {
    setPage(0);
  }, [search, filter, view]);
  function navigate(next) {
    setView(next);
    setSearch("");
    setFilter("ALL");
  }
  async function run(action, message, close = true) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const result = await action();
      setNotice(typeof message === "function" ? message(result) : message);
      if (close) setModal(null);
      await reload();
      return result;
    } catch (e) {
      setError(e.message);
    } finally {
      setBusy(false);
    }
  }
  async function openRequest(request) {
    setError("");
    setHistory([]);
    setModal({ type: "detail", request });
    try {
      setHistory(await allPages(token, `/audit?requestId=${request.id}`));
    } catch (e) {
      setError(e.message);
    }
  }
  const lowStock = items.filter((i) => stockStatus(i) === "Low stock");
  const pending = requests.filter((r) => r.state === "REQUESTED");
  const active = requests.filter((r) =>
    ["REQUESTED", "APPROVED", "RESERVED"].includes(r.state),
  );
  const fulfilled = requests.filter((r) => r.state === "FULFILLED");
  const filteredRequests = useMemo(
    () =>
      requests.filter(
        (r) =>
          (filter === "ALL" || r.state === filter) &&
          `${r.id} ${r.itemName} ${r.department} ${r.requestedBy} ${r.lot}`
            .toLowerCase()
            .includes(search.toLowerCase()),
      ),
    [requests, filter, search],
  );
  const filteredItems = items.filter(
    (i) =>
      `${i.name} ${i.udi} ${i.lot} ${i.category} ${i.location}`
        .toLowerCase()
        .includes(search.toLowerCase()) &&
      (filter === "ALL" || stockStatus(i) === filter),
  );
  function exportCsv() {
    const rows =
      view === "Inventory"
        ? [
            ["Name", "UDI", "Lot", "Available", "Reserved", "Total", "Status"],
            ...filteredItems.map((i) => [
              i.name,
              i.udi,
              i.lot,
              i.availableStock,
              i.reservedStock,
              i.totalStock,
              stockStatus(i),
            ]),
          ]
        : [
            [
              "Request",
              "Item",
              "Lot",
              "Quantity",
              "Department",
              "State",
              "Requested by",
              "Created",
            ],
            ...filteredRequests.map((r) => [
              r.id,
              r.itemName,
              r.lot,
              r.quantity,
              r.department,
              r.state,
              r.requestedBy,
              r.createdAt,
            ]),
          ];
    const content = rows
      .map((row) =>
        row
          .map(
            (v) =>
              `"${String(v)
                .replace(/^[=+@-]/, "'$&")
                .replaceAll('"', '""')}"`,
          )
          .join(","),
      )
      .join("\n");
    const url = URL.createObjectURL(
      new Blob([content], { type: "text/csv;charset=utf-8" }),
    );
    const a = document.createElement("a");
    a.href = url;
    a.download = `careflow-${view.toLowerCase()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
  function requestTable(rows) {
    return (
      <div className="cf-table-scroll">
        <table className="cf-table">
          <thead>
            <tr>
              <th>REQUEST / RESOURCE</th>
              <th>DEPARTMENT</th>
              <th>QUANTITY</th>
              <th>STATUS</th>
              <th>REQUESTED</th>
              <th aria-label="Open request" />
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.id}>
                <td>
                  <button
                    className="cf-table-link"
                    onClick={() => openRequest(r)}
                  >
                    CF-{String(r.id).padStart(4, "0")}
                  </button>
                  <span className="cf-cell-sub">{r.itemName}</span>
                </td>
                <td>
                  {r.department}
                  <span className="cf-cell-sub">{r.requestedBy}</span>
                </td>
                <td>
                  {r.quantity} <span className="cf-muted">units</span>
                </td>
                <td>
                  <Badge state={r.state} />
                </td>
                <td className="cf-date">{fmtDate(r.createdAt)}</td>
                <td>
                  <button
                    className="cf-icon-button"
                    aria-label={`Open request CF-${r.id}`}
                    onClick={() => openRequest(r)}
                  >
                    <Icon name="arrow" size={17} />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        {!rows.length && (
          <div className="cf-empty">
            <Icon name="requests" size={30} />
            <h3>No requests here yet</h3>
            <p>New supply requests will appear in this workspace.</p>
          </div>
        )}
      </div>
    );
  }
  return (
    <div className="cf cf-app">
      <aside className="cf-sidebar">
        <a className="cf-brand" href="/">
          <span className="cf-brand-mark">
            <Icon name="pulse" size={24} />
          </span>
          CareFlow<span className="cf-brand-dot">.</span>
        </a>
        <div className="cf-workspace">
          <span className="cf-workspace-avatar">N</span>
          <div>
            Northstar Medical Center<small>Supply operations · Demo</small>
          </div>
          <span className="cf-muted">⌄</span>
        </div>
        <span className="cf-nav-label">WORKSPACE</span>
        <nav>
          {[
            ["Overview", "grid"],
            ["Supply requests", "requests"],
            ["Inventory", "box"],
            ["Reports", "chart"],
            ...(role === "ADMIN"
              ? [
                  ["Audit trail", "shield"],
                  ["Team & access", "users"],
                ]
              : []),
          ].map(([title, icon]) => (
            <button
              key={title}
              className={view === title ? "active" : ""}
              onClick={() => navigate(title)}
            >
              <Icon name={icon} size={19} />
              {title}
              {title === "Supply requests" && pending.length > 0 && (
                <span className="cf-nav-count">{pending.length}</span>
              )}
            </button>
          ))}
        </nav>
        <div className="cf-sidebar-bottom">
          <div className="cf-simulation-card">
            <span className="cf-dot" /> SIMULATION ENVIRONMENT
            <p>
              Connected workflows.
              <br />
              Traceable resources.
              <br />
              Synthetic data only.
            </p>
            <a href="/hospital">
              Hospital workspace <span>↗</span>
            </a>
          </div>
          <div className="cf-profile">
            <span className="cf-avatar">
              {username.slice(0, 2).toUpperCase()}
            </span>
            <div>
              <strong>{username}</strong>
              <small>{role.toLowerCase().replaceAll("_", " ")}</small>
            </div>
            <button aria-label="Sign out" onClick={logout}>
              <Icon name="logout" size={18} />
            </button>
          </div>
        </div>
      </aside>
      <div className="cf-main">
        <header className="cf-topbar">
          <span>
            Workspace <span className="cf-breadcrumb">/</span>{" "}
            <strong>{view}</strong>
          </span>
          <div>
            <span className="cf-live">
              <span className="cf-dot" /> Synthetic demo
            </span>
            <span className="cf-topbar-divider" />
            <button
              className="cf-icon-button"
              aria-label="View stock alerts"
              onClick={() => {
                navigate("Inventory");
                setFilter("Low stock");
              }}
            >
              <Icon name="bell" />
              {lowStock.length > 0 && <i />}
            </button>
            <span className="cf-avatar small">
              {username.slice(0, 2).toUpperCase()}
            </span>
          </div>
        </header>
        <main className="cf-content">
          <div className="cf-page-heading">
            <div>
              <span className="cf-kicker">
                {view === "Overview"
                  ? "YOUR OPERATIONS, IN FOCUS"
                  : "NORTHSTAR MEDICAL CENTER"}
              </span>
              <h1>
                {view === "Overview" ? "A clearer view. Better flow." : view}
              </h1>
              <p>
                {
                  {
                    Overview:
                      "Keep resources moving and every handoff connected.",
                    "Supply requests":
                      "From resource need to confirmed delivery. Every step, accounted for.",
                    Inventory:
                      "Know what’s on hand, what’s reserved, and what needs attention.",
                    Reports: "A live view of your simulated supply operations.",
                    "Audit trail": "Every action has a story. Follow it here.",
                    "Team & access":
                      "The right access for every responsibility.",
                  }[view]
                }
              </p>
            </div>
            <div className="cf-heading-actions">
              {(view === "Supply requests" || view === "Inventory") && (
                <button className="cf-button secondary" onClick={exportCsv}>
                  <Icon name="download" size={16} />
                  Export
                </button>
              )}
              {view === "Inventory" && canManage ? (
                <button
                  className="cf-button primary"
                  onClick={() => setModal({ type: "item" })}
                >
                  <Icon name="plus" size={18} />
                  Add inventory
                </button>
              ) : (
                canRequest && (
                  <button
                    className="cf-button primary"
                    onClick={() => setModal({ type: "request" })}
                  >
                    <Icon name="plus" size={18} />
                    New request
                  </button>
                )
              )}
            </div>
          </div>
          {error && (
            <div className="cf-notice error" role="alert">
              {error}
              <button aria-label="Dismiss error" onClick={() => setError("")}>
                ×
              </button>
            </div>
          )}
          {notice && (
            <div className="cf-notice" role="status">
              {notice}
              <button
                aria-label="Dismiss notification"
                onClick={() => setNotice("")}
              >
                ×
              </button>
            </div>
          )}
          {!careflowRole ? (
            <div className="cf-panel cf-empty">
              <h2>Your account has a hospital role</h2>
              <p>
                An administrator can assign a CareFlow role in Team & access.
              </p>
              <a className="cf-button primary" href="/hospital">
                Open hospital workspace
              </a>
            </div>
          ) : loading ? (
            <div className="cf-panel cf-empty" aria-live="polite">
              <span className="cf-loader" />
              <p>Connecting your operations workspace…</p>
            </div>
          ) : (
            <>
              {view === "Overview" && (
                <>
                  <section className="cf-metrics">
                    {[
                      [
                        "Active requests",
                        active.length,
                        "requests",
                        "Across the supply workflow",
                        "teal",
                      ],
                      [
                        "Awaiting review",
                        pending.length,
                        "clock",
                        "Ready for clinical confirmation",
                        "amber",
                      ],
                      [
                        "Resources available",
                        items.reduce(
                          (sum, i) =>
                            sum +
                            (stockStatus(i) === "Expired" || i.quarantined
                              ? 0
                              : i.availableStock),
                          0,
                        ),
                        "box",
                        `${items.length} tracked inventory lots`,
                        "blue",
                      ],
                      [
                        "Fulfilled requests",
                        fulfilled.length,
                        "check",
                        "Distribution confirmed",
                        "purple",
                      ],
                    ].map(([label, value, icon, sub, color]) => (
                      <article className="cf-metric" key={label}>
                        <div>
                          <span>{label}</span>
                          <span className={`cf-metric-icon ${color}`}>
                            <Icon name={icon} size={19} />
                          </span>
                        </div>
                        <strong>{value.toLocaleString()}</strong>
                        <small>{sub}</small>
                      </article>
                    ))}
                  </section>
                  <div className="cf-overview-grid">
                    <section className="cf-panel cf-flow-panel">
                      <div className="cf-panel-heading">
                        <div>
                          <h2>Every request. One connected flow.</h2>
                          <p>Track the handoffs that keep resources moving.</p>
                        </div>
                        <span className="cf-tag">LIVE WORKFLOW</span>
                      </div>
                      <div className="cf-workflow">
                        {[
                          ["REQUESTED", "requests"],
                          ["APPROVED", "shield"],
                          ["RESERVED", "box"],
                          ["FULFILLED", "check"],
                        ].map(([state, icon], i) => (
                          <React.Fragment key={state}>
                            <button
                              onClick={() => {
                                navigate("Supply requests");
                                setFilter(state);
                              }}
                            >
                              <span className={`cf-workflow-icon step-${i}`}>
                                <Icon name={icon} size={22} />
                              </span>
                              <strong>
                                {
                                  requests.filter((r) => r.state === state)
                                    .length
                                }
                              </strong>
                              <span>
                                {state === "REQUESTED"
                                  ? "Requested"
                                  : stateLabels[state]}
                              </span>
                              <small>
                                {
                                  [
                                    "Submit & validate",
                                    "Review & confirm",
                                    "Allocate stock",
                                    "Confirm delivery",
                                  ][i]
                                }
                              </small>
                            </button>
                            {i < 3 && (
                              <span className="cf-workflow-connector">→</span>
                            )}
                          </React.Fragment>
                        ))}
                      </div>
                      <div className="cf-flow-footer">
                        <Icon name="shield" size={16} />
                        <span>
                          Role-based approvals. Protected allocations. Traceable
                          handoffs.
                        </span>
                      </div>
                    </section>
                    <section className="cf-attention">
                      <div className="cf-attention-heading">
                        <span className="cf-attention-icon">
                          <Icon name="bell" size={19} />
                        </span>
                        <span>NEEDS ATTENTION</span>
                        <span className="cf-attention-count">
                          {lowStock.length}
                        </span>
                      </div>
                      <h2>
                        A little attention.
                        <br />A smoother tomorrow.
                      </h2>
                      <p>
                        {lowStock.length
                          ? `${lowStock.length} inventory lots are at or below their reorder point.`
                          : "Your inventory is above its configured reorder points."}
                      </p>
                      {lowStock.slice(0, 2).map((i) => (
                        <div className="cf-alert-item" key={i.id}>
                          <span>
                            {i.name}
                            <small>{i.lot}</small>
                          </span>
                          <strong>
                            {i.availableStock}
                            <small>available</small>
                          </strong>
                        </div>
                      ))}
                      <button
                        onClick={() => {
                          navigate("Inventory");
                          setFilter("Low stock");
                        }}
                      >
                        Review inventory <Icon name="arrow" size={17} />
                      </button>
                    </section>
                  </div>
                  <section className="cf-panel">
                    <div className="cf-panel-heading">
                      <div>
                        <h2>Recent supply requests</h2>
                        <p>A pulse on the latest resource needs.</p>
                      </div>
                      <button
                        className="cf-text-button"
                        onClick={() => navigate("Supply requests")}
                      >
                        View all requests <Icon name="arrow" size={16} />
                      </button>
                    </div>
                    {requestTable(requests.slice(0, 5))}
                  </section>
                  <div className="cf-bottom-note">
                    <Icon name="shield" size={15} />
                    <span>Every lot traceable. Every transition recorded.</span>
                    <span>SIMULATED HEALTHCARE OPERATIONS</span>
                  </div>
                </>
              )}
              {view === "Supply requests" && (
                <section className="cf-panel">
                  <div className="cf-list-tools">
                    <div className="cf-tabs">
                      {[
                        ["ALL", "All requests"],
                        ["REQUESTED", "Awaiting review"],
                        ["RESERVED", "Reserved"],
                        ["FULFILLED", "Fulfilled"],
                      ].map(([key, label]) => (
                        <button
                          className={filter === key ? "active" : ""}
                          onClick={() => setFilter(key)}
                          key={key}
                        >
                          {label}
                          <span>
                            {key === "ALL"
                              ? requests.length
                              : requests.filter((r) => r.state === key).length}
                          </span>
                        </button>
                      ))}
                    </div>
                    <label className="cf-search">
                      <Icon name="search" size={17} />
                      <input
                        aria-label="Search requests"
                        placeholder="Search requests…"
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                      />
                    </label>
                  </div>
                  {!["ALL", "REQUESTED", "RESERVED", "FULFILLED"].includes(
                    filter,
                  ) && (
                    <div className="cf-filter-note">
                      Filtering: {stateLabels[filter]}{" "}
                      <button onClick={() => setFilter("ALL")}>Clear</button>
                    </div>
                  )}
                  {requestTable(
                    filteredRequests.slice(page * 8, (page + 1) * 8),
                  )}
                  <div className="cf-pagination">
                    <span>
                      {filteredRequests.length
                        ? `${page * 8 + 1}–${Math.min((page + 1) * 8, filteredRequests.length)}`
                        : "0"}{" "}
                      of {filteredRequests.length} requests
                    </span>
                    <div>
                      <button
                        disabled={page === 0}
                        onClick={() => setPage((p) => p - 1)}
                      >
                        ← Previous
                      </button>
                      <button
                        disabled={(page + 1) * 8 >= filteredRequests.length}
                        onClick={() => setPage((p) => p + 1)}
                      >
                        Next →
                      </button>
                    </div>
                  </div>
                </section>
              )}
              {view === "Inventory" && (
                <>
                  <div className="cf-inventory-summary">
                    <span>
                      <i className="cf-dot" />
                      {items.length} tracked lots
                    </span>
                    <span>
                      {items.filter((i) => i.quarantined).length} quality holds
                    </span>
                    <span>{lowStock.length} below reorder point</span>
                  </div>
                  <section className="cf-panel">
                    <div className="cf-list-tools">
                      <div className="cf-tabs">
                        {[
                          "ALL",
                          "In stock",
                          "Low stock",
                          "Quarantined",
                          "Expired",
                        ].map((s) => (
                          <button
                            className={filter === s ? "active" : ""}
                            key={s}
                            onClick={() => setFilter(s)}
                          >
                            {s === "ALL" ? "All inventory" : s}
                          </button>
                        ))}
                      </div>
                      <label className="cf-search">
                        <Icon name="search" size={17} />
                        <input
                          aria-label="Search inventory"
                          placeholder="Search name, UDI or lot…"
                          value={search}
                          onChange={(e) => setSearch(e.target.value)}
                        />
                      </label>
                    </div>
                    <div className="cf-table-scroll">
                      <table className="cf-table">
                        <thead>
                          <tr>
                            <th>RESOURCE / IDENTIFIER</th>
                            <th>LOT / EXPIRY</th>
                            <th>LOCATION</th>
                            <th>AVAILABLE</th>
                            <th>RESERVED / TOTAL</th>
                            <th>STATUS</th>
                            {canManage && <th />}
                          </tr>
                        </thead>
                        <tbody>
                          {filteredItems
                            .slice(page * 8, (page + 1) * 8)
                            .map((i) => (
                              <tr key={i.id}>
                                <td>
                                  <strong>{i.name}</strong>
                                  <span className="cf-cell-sub">
                                    {i.udi} · {i.category}
                                  </span>
                                </td>
                                <td>
                                  {i.lot}
                                  <span className="cf-cell-sub">
                                    {fmtDate(i.expiresOn + "T00:00:00")}
                                  </span>
                                </td>
                                <td>{i.location}</td>
                                <td>
                                  <strong>{i.availableStock}</strong>
                                  <div className="cf-stock-bar">
                                    <span
                                      style={{
                                        width: `${i.totalStock ? (i.availableStock / i.totalStock) * 100 : 0}%`,
                                      }}
                                    />
                                  </div>
                                </td>
                                <td>
                                  {i.reservedStock}{" "}
                                  <span className="cf-muted">
                                    / {i.totalStock}
                                  </span>
                                </td>
                                <td>
                                  <Badge
                                    state={stockStatus(i).replaceAll(" ", "-")}
                                  >
                                    {stockStatus(i)}
                                  </Badge>
                                </td>
                                {canManage && (
                                  <td>
                                    <button
                                      className="cf-text-button"
                                      onClick={() =>
                                        setModal({ type: "adjust", item: i })
                                      }
                                    >
                                      Manage
                                    </button>
                                  </td>
                                )}
                              </tr>
                            ))}
                        </tbody>
                      </table>
                      {!filteredItems.length && (
                        <div className="cf-empty">
                          <p>No inventory matches this view.</p>
                        </div>
                      )}
                    </div>
                    <div className="cf-pagination">
                      <span>{filteredItems.length} matching lots</span>
                      <div>
                        <button
                          disabled={page === 0}
                          onClick={() => setPage((p) => p - 1)}
                        >
                          ← Previous
                        </button>
                        <button
                          disabled={(page + 1) * 8 >= filteredItems.length}
                          onClick={() => setPage((p) => p + 1)}
                        >
                          Next →
                        </button>
                      </div>
                    </div>
                  </section>
                </>
              )}
              {view === "Audit trail" && (
                <section className="cf-panel">
                  <div className="cf-panel-heading">
                    <div>
                      <h2>Activity ledger</h2>
                      <p>
                        Persistent records from successful operations, newest
                        first.
                      </p>
                    </div>
                    <span className="cf-tag">{audits.length} EVENTS</span>
                  </div>
                  <div className="cf-timeline">
                    {audits.slice(page * 20, (page + 1) * 20).map((a) => (
                      <div key={a.id}>
                        <span className="cf-timeline-dot">
                          <Icon
                            name={
                              a.resourceType === "INVENTORY" ? "box" : "check"
                            }
                            size={15}
                          />
                        </span>
                        <section>
                          <strong>
                            {a.action.replaceAll("_", " ").toLowerCase()}{" "}
                            <span className="cf-muted">
                              · {a.resourceType.toLowerCase()} #{a.resourceId}
                            </span>
                          </strong>
                          <p>
                            {a.previousState || "Created"} → {a.newState}
                          </p>
                          <small>
                            {a.actor} · {fmtTime(a.occurredAt)}
                          </small>
                          <p>{a.detail}</p>
                        </section>
                      </div>
                    ))}
                  </div>
                  <div className="cf-pagination">
                    <span>{audits.length} recorded events</span>
                    <div>
                      <button
                        disabled={page === 0}
                        onClick={() => setPage((p) => p - 1)}
                      >
                        ← Previous
                      </button>
                      <button
                        disabled={(page + 1) * 20 >= audits.length}
                        onClick={() => setPage((p) => p + 1)}
                      >
                        Next →
                      </button>
                    </div>
                  </div>
                </section>
              )}
              {view === "Reports" && (
                <div className="cf-report-grid">
                  <section className="cf-panel">
                    <div className="cf-panel-heading">
                      <div>
                        <h2>Request distribution</h2>
                        <p>
                          Current states of {requests.length} visible requests.
                        </p>
                      </div>
                    </div>
                    <div className="cf-report-body">
                      {Object.entries(stateLabels).map(([state, label]) => (
                        <div className="cf-report-row" key={state}>
                          <span>{label}</span>
                          <div>
                            <i
                              style={{
                                width: `${requests.length ? (requests.filter((r) => r.state === state).length / requests.length) * 100 : 0}%`,
                              }}
                            />
                          </div>
                          <strong>
                            {requests.filter((r) => r.state === state).length}
                          </strong>
                        </div>
                      ))}
                    </div>
                  </section>
                  <section className="cf-panel">
                    <div className="cf-panel-heading">
                      <div>
                        <h2>Device traceability</h2>
                        <p>Lot-level accountability across the workspace.</p>
                      </div>
                    </div>
                    <div className="cf-report-body">
                      <div className="cf-report-number">
                        {items.length
                          ? Math.round(
                              (items.filter(
                                (i) => i.udi && i.lot && i.expiresOn,
                              ).length /
                                items.length) *
                                100,
                            )
                          : 0}
                        <span>%</span>
                      </div>
                      <p>
                        Tracked lots with a UDI, lot number, and expiry date.
                      </p>
                      <hr />
                      <p>
                        <strong>
                          {items.filter((i) => i.quarantined).length}
                        </strong>{" "}
                        lots on quality hold
                      </p>
                      <p>
                        <strong>
                          {
                            items.filter((i) => stockStatus(i) === "Expired")
                              .length
                          }
                        </strong>{" "}
                        expired lots blocked
                      </p>
                      <p className="cf-muted">
                        Operational figures use visible records. Performance
                        benchmarks and verification evidence are documented in
                        the project’s benchmark report.
                      </p>
                    </div>
                  </section>
                </div>
              )}
              {view === "Team & access" && (
                <section className="cf-panel">
                  <div className="cf-panel-heading">
                    <div>
                      <h2>Workspace access</h2>
                      <p>
                        Role changes apply to subsequent API requests. Users
                        should sign in again to refresh their interface.
                      </p>
                    </div>
                  </div>
                  <div className="cf-table-scroll">
                    <table className="cf-table">
                      <thead>
                        <tr>
                          <th>USERNAME</th>
                          <th>ROLE</th>
                          <th>UPDATE ACCESS</th>
                        </tr>
                      </thead>
                      <tbody>
                        {users.map((u) => (
                          <tr key={u.id}>
                            <td>
                              <strong>{u.username}</strong>
                              {u.username === username && (
                                <span className="cf-cell-sub">
                                  Your account
                                </span>
                              )}
                            </td>
                            <td>{u.role.replaceAll("_", " ")}</td>
                            <td>
                              <select
                                aria-label={`Role for ${u.username}`}
                                value={u.role}
                                disabled={busy || u.username === username}
                                onChange={(e) =>
                                  run(
                                    () =>
                                      authApi.updateUserRole(
                                        token,
                                        u.id,
                                        e.target.value,
                                      ),
                                    "Role updated. Ask the user to sign in again.",
                                    false,
                                  )
                                }
                              >
                                {[
                                  "ADMIN",
                                  "REQUESTER",
                                  "CLINICAL_STAFF",
                                  "INVENTORY_MANAGER",
                                  ...(u.role === "DOCTOR" ? ["DOCTOR"] : []),
                                  "RECEPTIONIST",
                                ].map((r) => (
                                  <option key={r}>{r}</option>
                                ))}
                              </select>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </section>
              )}
            </>
          )}
          <footer className="cf-footer">
            <span>
              CareFlow <span>·</span> Connected supply operations
            </span>
            <span>Independent portfolio simulation · No patient data</span>
          </footer>
        </main>
      </div>
      {modal && (
        <Modal
          title={
            {
              request: "New supply request",
              item: "Add an inventory lot",
              adjust: "Manage inventory lot",
              detail: `Request CF-${String(modal.request?.id).padStart(4, "0")}`,
              review: "Review supply request",
              cancel: "Cancel supply request",
            }[modal.type]
          }
          subtitle={
            modal.type === "detail"
              ? modal.request.itemName
              : "Every change is recorded in the audit trail."
          }
          onClose={() => !busy && setModal(null)}
        >
          {error && (
            <div className="cf-notice error" role="alert">
              {error}
            </div>
          )}
          {modal.type === "request" && (
            <form
              className="cf-form"
              onSubmit={(e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                run(
                  () =>
                    careflow(token, "/requests", "POST", {
                      itemId: Number(f.get("itemId")),
                      quantity: Number(f.get("quantity")),
                      department: f.get("department"),
                      purpose: f.get("purpose"),
                    }),
                  "Supply request submitted for review.",
                );
              }}
            >
              <label>
                Supply resource
                <select name="itemId" required autoFocus>
                  <option value="">Select a resource and lot</option>
                  {items
                    .filter(
                      (i) =>
                        !["Quarantined", "Expired"].includes(stockStatus(i)),
                    )
                    .map((i) => (
                      <option key={i.id} value={i.id}>
                        {i.name} · {i.lot} · {i.availableStock} available
                      </option>
                    ))}
                </select>
              </label>
              <div className="cf-form-row">
                <label>
                  Quantity
                  <input
                    name="quantity"
                    type="number"
                    min="1"
                    max="2147483647"
                    defaultValue="1"
                    required
                  />
                </label>
                <label>
                  Department
                  <select name="department">
                    {departments.map((d) => (
                      <option key={d}>{d}</option>
                    ))}
                  </select>
                </label>
              </div>
              <label>
                Purpose
                <textarea
                  name="purpose"
                  maxLength="500"
                  required
                  placeholder="Describe the simulated operational need"
                />
              </label>
              <p className="cf-form-note">
                Submitting a request does not reserve stock. A clinical reviewer
                must confirm the need before inventory allocation.
              </p>
              <button
                className="cf-button primary"
                disabled={busy || !items.length}
              >
                {busy ? "Submitting…" : "Submit request"}
                <Icon name="arrow" size={17} />
              </button>
            </form>
          )}
          {modal.type === "item" && (
            <form
              className="cf-form"
              onSubmit={(e) => {
                e.preventDefault();
                const f = Object.fromEntries(new FormData(e.currentTarget));
                f.totalStock = Number(f.totalStock);
                f.reorderPoint = Number(f.reorderPoint);
                run(
                  () => careflow(token, "/inventory", "POST", f),
                  "Synthetic inventory lot created.",
                );
              }}
            >
              <label>
                Resource name
                <input name="name" maxLength="160" required autoFocus />
              </label>
              <div className="cf-form-row">
                <label>
                  Synthetic UDI
                  <input
                    name="udi"
                    maxLength="80"
                    placeholder="SYN-UDI-10009"
                    required
                  />
                </label>
                <label>
                  Lot number
                  <input name="lot" maxLength="80" required />
                </label>
              </div>
              <div className="cf-form-row">
                <label>
                  Category
                  <select name="category">
                    {[
                      "Cardiovascular",
                      "Diabetes care",
                      "Surgical",
                      "Neuroscience",
                      "Monitoring",
                    ].map((c) => (
                      <option key={c}>{c}</option>
                    ))}
                  </select>
                </label>
                <label>
                  Storage location
                  <input name="location" maxLength="100" required />
                </label>
              </div>
              <div className="cf-form-row">
                <label>
                  Total stock
                  <input
                    name="totalStock"
                    type="number"
                    min="0"
                    max="2147483647"
                    required
                  />
                </label>
                <label>
                  Reorder point
                  <input
                    name="reorderPoint"
                    type="number"
                    min="0"
                    max="2147483647"
                    required
                  />
                </label>
              </div>
              <label>
                Expiry date
                <input
                  name="expiresOn"
                  type="date"
                  min={new Date(Date.now() + 86400000)
                    .toISOString()
                    .slice(0, 10)}
                  required
                />
              </label>
              <button className="cf-button primary" disabled={busy}>
                {busy ? "Saving…" : "Create inventory lot"}
              </button>
            </form>
          )}
          {modal.type === "adjust" && (
            <form
              className="cf-form"
              onSubmit={(e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                run(
                  () =>
                    careflow(token, `/inventory/${modal.item.id}`, "PATCH", {
                      stockDelta: Number(f.get("stockDelta")),
                      quarantined: f.get("quarantined") === "on",
                      reason: f.get("reason"),
                    }),
                  "Inventory update recorded.",
                );
              }}
            >
              <div className="cf-detail-summary">
                <strong>{modal.item.name}</strong>
                <span>
                  {modal.item.udi} · {modal.item.lot}
                </span>
                <p>
                  {modal.item.totalStock} total · {modal.item.reservedStock}{" "}
                  reserved · {modal.item.availableStock} available
                </p>
              </div>
              <label>
                Stock adjustment
                <input
                  name="stockDelta"
                  type="number"
                  min="-2147483648"
                  max="2147483647"
                  defaultValue="0"
                  required
                  autoFocus
                />
                <small>
                  Use a positive quantity to replenish or a negative quantity to
                  remove stock.
                </small>
              </label>
              <label className="cf-check-label">
                <input
                  type="checkbox"
                  name="quarantined"
                  defaultChecked={modal.item.quarantined}
                />
                Place this lot on quality hold
              </label>
              <label>
                Reason
                <textarea
                  name="reason"
                  maxLength="500"
                  required
                  placeholder="Record the reason for this stock or quality change"
                />
              </label>
              <button className="cf-button primary" disabled={busy}>
                {busy ? "Saving…" : "Save inventory update"}
              </button>
            </form>
          )}
          {["review", "cancel"].includes(modal.type) && (
            <form
              className="cf-form"
              onSubmit={(e) => {
                e.preventDefault();
                const f = new FormData(e.currentTarget);
                const review = modal.type === "review";
                run(
                  () =>
                    careflow(
                      token,
                      `/requests/${modal.request.id}/${review ? "review" : "cancel"}`,
                      "POST",
                      review
                        ? {
                            approve: f.get("decision") === "approve",
                            reason: f.get("reason"),
                          }
                        : { reason: f.get("reason") },
                    ),
                  review
                    ? "Clinical review recorded."
                    : "Request cancelled and any reserved stock released.",
                );
              }}
            >
              {modal.type === "review" && (
                <label>
                  Decision
                  <select name="decision">
                    <option value="approve">Approve resource need</option>
                    <option value="reject">Reject request</option>
                  </select>
                </label>
              )}
              <label>
                Reason
                <textarea name="reason" maxLength="500" required autoFocus />
              </label>
              <button className="cf-button primary" disabled={busy}>
                Record {modal.type === "review" ? "review" : "cancellation"}
              </button>
            </form>
          )}
          {modal.type === "detail" && (
            <>
              <div className="cf-detail-summary">
                <Badge state={modal.request.state} />
                <div className="cf-detail-grid">
                  <span>
                    Quantity<strong>{modal.request.quantity} units</strong>
                  </span>
                  <span>
                    Department<strong>{modal.request.department}</strong>
                  </span>
                  <span>
                    Requested by<strong>{modal.request.requestedBy}</strong>
                  </span>
                  <span>
                    Traceability<strong>{modal.request.lot}</strong>
                  </span>
                </div>
                <p>{modal.request.purpose}</p>
                {modal.request.reservedUntil &&
                  modal.request.state === "RESERVED" && (
                    <p>
                      Reservation expires {fmtTime(modal.request.reservedUntil)}
                    </p>
                  )}
              </div>
              <div className="cf-detail-actions">
                {modal.request.state === "REQUESTED" &&
                  canReview &&
                  username !== modal.request.requestedBy && (
                    <button
                      className="cf-button primary"
                      onClick={() => setModal({ ...modal, type: "review" })}
                    >
                      Review request
                    </button>
                  )}
                {modal.request.state === "APPROVED" && canManage && (
                  <button
                    className="cf-button primary"
                    disabled={busy}
                    onClick={() =>
                      run(
                        () =>
                          careflow(
                            token,
                            `/requests/${modal.request.id}/reserve`,
                            "POST",
                          ),
                        "Inventory reserved atomically.",
                      )
                    }
                  >
                    Reserve inventory
                  </button>
                )}
                {modal.request.state === "RESERVED" && canManage && (
                  <button
                    className="cf-button primary"
                    disabled={busy}
                    onClick={() =>
                      run(
                        () =>
                          careflow(
                            token,
                            `/requests/${modal.request.id}/fulfill`,
                            "POST",
                            undefined,
                            fulfillmentKey(modal.request.id),
                          ),
                        (r) =>
                          `Fulfillment confirmed. Receipt #${r.id}; ${r.quantity} units distributed.`,
                      )
                    }
                  >
                    Confirm fulfillment
                  </button>
                )}
                {modal.request.state === "RESERVED" &&
                  canManage &&
                  new Date(modal.request.reservedUntil) <= new Date() && (
                    <button
                      className="cf-button secondary"
                      disabled={busy}
                      onClick={() =>
                        run(
                          () =>
                            careflow(
                              token,
                              `/requests/${modal.request.id}/expire`,
                              "POST",
                            ),
                          "Expired reservation released.",
                        )
                      }
                    >
                      Release expired reservation
                    </button>
                  )}
                {["REQUESTED", "APPROVED", "RESERVED"].includes(
                  modal.request.state,
                ) &&
                  (canManage || username === modal.request.requestedBy) && (
                    <button
                      className="cf-button secondary"
                      onClick={() => setModal({ ...modal, type: "cancel" })}
                    >
                      Cancel request
                    </button>
                  )}
              </div>
              <h3 className="cf-history-title">Request history</h3>
              <div className="cf-timeline compact">
                {history.map((a) => (
                  <div key={a.id}>
                    <span className="cf-timeline-dot">
                      <Icon name="check" size={14} />
                    </span>
                    <section>
                      <strong>{a.newState.toLowerCase()}</strong>
                      <p>{a.detail}</p>
                      <small>
                        {a.actor} · {fmtTime(a.occurredAt)}
                      </small>
                    </section>
                  </div>
                ))}
                {!history.length && (
                  <p className="cf-muted">Loading audit history…</p>
                )}
              </div>
            </>
          )}
        </Modal>
      )}
    </div>
  );
}
