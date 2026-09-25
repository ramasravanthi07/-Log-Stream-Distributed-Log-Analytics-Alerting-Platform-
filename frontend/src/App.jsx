import { useEffect, useState } from "react";
import "./styles.css";

import {
  search,
  validate,
  stats,
  listAlerts,
  createAlert,
  deleteAlert,
  evaluateAlert,
} from "./api";

function App() {
  const [activePage, setActivePage] = useState("dashboard");

  const [query, setQuery] = useState(
    "level:ERROR AND service:billing-api"
  );

  const [logs, setLogs] = useState([]);
  const [dashboardStats, setDashboardStats] = useState({});
  const [alerts, setAlerts] = useState([]);

  const [loading, setLoading] = useState(false);
  const [statsLoading, setStatsLoading] = useState(false);
  const [alertLoading, setAlertLoading] = useState(false);

  const [error, setError] = useState("");
  const [searchMessage, setSearchMessage] = useState("");

  const [queryValid, setQueryValid] = useState(null);

  const [lastUpdated, setLastUpdated] = useState(null);

  const [alertForm, setAlertForm] = useState({
    name: "",
    query: "level:ERROR",
    threshold: 10,
    window: 5,
    webhookUrl: "",
  });

  // =========================================================
  // HELPERS
  // =========================================================

  const getValue = (obj, keys, fallback = 0) => {
    for (const key of keys) {
      if (
        obj &&
        obj[key] !== undefined &&
        obj[key] !== null
      ) {
        return obj[key];
      }
    }

    return fallback;
  };

  const extractLogs = (data) => {
    if (Array.isArray(data)) {
      return data;
    }

    if (Array.isArray(data?.logs)) {
      return data.logs;
    }

    if (Array.isArray(data?.results)) {
      return data.results;
    }

    if (Array.isArray(data?.hits)) {
      return data.hits;
    }

    return [];
  };

  const extractAlerts = (data) => {
    if (Array.isArray(data)) {
      return data;
    }

    if (Array.isArray(data?.alerts)) {
      return data.alerts;
    }

    if (Array.isArray(data?.results)) {
      return data.results;
    }

    return [];
  };

  // =========================================================
  // LOAD STATS
  // =========================================================

  const loadStats = async () => {
    try {
      setStatsLoading(true);
      setError("");

      const data = await stats();

      setDashboardStats(data || {});
      setLastUpdated(new Date());
    } catch (err) {
      console.error(err);
      setError("Unable to load dashboard statistics.");
    } finally {
      setStatsLoading(false);
    }
  };

  // =========================================================
  // LOAD ALERTS
  // =========================================================

  const loadAlerts = async () => {
    try {
      const data = await listAlerts();

      setAlerts(extractAlerts(data));
    } catch (err) {
      console.error(err);
    }
  };

  // =========================================================
  // INITIAL LOAD
  // =========================================================

  useEffect(() => {
    loadStats();
    loadAlerts();
  }, []);

  // =========================================================
  // SEARCH
  // =========================================================

  const runSearch = async (event) => {
    if (event) {
      event.preventDefault();
    }

    if (!query.trim()) {
      setSearchMessage("Please enter a query.");
      return;
    }

    try {
      setLoading(true);
      setError("");
      setSearchMessage("");

      const start = performance.now();

      const data = await search(query);

      const end = performance.now();

      const resultLogs = extractLogs(data);

      setLogs(resultLogs);

      setSearchMessage(
        `${resultLogs.length} logs found in ${Math.round(
          end - start
        )} ms`
      );
    } catch (err) {
      console.error(err);

      setLogs([]);
      setError(
        err?.message || "Search failed. Please check the backend."
      );
    } finally {
      setLoading(false);
    }
  };

  // =========================================================
  // VALIDATE QUERY
  // =========================================================

  const validateQuery = async () => {
    if (!query.trim()) {
      setQueryValid(false);
      return;
    }

    try {
      const result = await validate(query);

      if (
        result === true ||
        result?.valid === true ||
        result?.isValid === true
      ) {
        setQueryValid(true);
      } else {
        setQueryValid(false);
      }
    } catch (err) {
      console.error(err);
      setQueryValid(false);
    }
  };

  // =========================================================
  // USE QUERY
  // =========================================================

  const useQuery = (value) => {
    setQuery(value);
    setQueryValid(null);
    setSearchMessage("");
    setError("");
  };

  // =========================================================
  // ALERT FORM
  // =========================================================

  const updateAlertForm = (field, value) => {
    setAlertForm((previous) => ({
      ...previous,
      [field]: value,
    }));
  };

  // =========================================================
  // CREATE ALERT
  // =========================================================

  const handleCreateAlert = async (event) => {
    event.preventDefault();

    if (!alertForm.name.trim()) {
      setError("Alert name is required.");
      return;
    }

    if (!alertForm.query.trim()) {
      setError("Alert query is required.");
      return;
    }

    try {
      setAlertLoading(true);
      setError("");

      await createAlert({
        name: alertForm.name,
        query: alertForm.query,
        threshold: Number(alertForm.threshold),
        window: Number(alertForm.window),
        webhookUrl: alertForm.webhookUrl,
      });

      setAlertForm({
        name: "",
        query: "level:ERROR",
        threshold: 10,
        window: 5,
        webhookUrl: "",
      });

      await loadAlerts();
    } catch (err) {
      console.error(err);

      setError(
        err?.message || "Unable to create alert."
      );
    } finally {
      setAlertLoading(false);
    }
  };

  // =========================================================
  // DELETE ALERT
  // =========================================================

  const handleDeleteAlert = async (id) => {
    try {
      setAlertLoading(true);
      setError("");

      await deleteAlert(id);

      await loadAlerts();
    } catch (err) {
      console.error(err);

      setError(
        err?.message || "Unable to delete alert."
      );
    } finally {
      setAlertLoading(false);
    }
  };

  // =========================================================
  // EVALUATE ALERT
  // =========================================================

  const handleEvaluateAlert = async (id) => {
    try {
      setAlertLoading(true);
      setError("");

      await evaluateAlert(id);

      await loadAlerts();
    } catch (err) {
      console.error(err);

      setError(
        err?.message || "Unable to evaluate alert."
      );
    } finally {
      setAlertLoading(false);
    }
  };

  // =========================================================
  // FORMAT LOG
  // =========================================================

  const formatLog = (log) => {
    return {
      timestamp: getValue(
        log,
        ["timestamp", "time", "@timestamp"],
        "-"
      ),

      level: getValue(
        log,
        ["level", "logLevel"],
        "INFO"
      ),

      service: getValue(
        log,
        ["service", "serviceName"],
        "-"
      ),

      message: getValue(
        log,
        ["message", "msg"],
        "-"
      ),

      host: getValue(
        log,
        ["host", "hostname"],
        "-"
      ),
    };
  };

  // =========================================================
  // STAT VALUES
  // =========================================================

  const totalLogs = getValue(
    dashboardStats,
    ["totalLogs", "total", "count", "logCount"],
    0
  );

  const errorCount = getValue(
    dashboardStats,
    ["errorCount", "errors", "errorLogs"],
    0
  );

  const warningCount = getValue(
    dashboardStats,
    ["warningCount", "warnings", "warningLogs"],
    0
  );

  const ingestionRate = getValue(
    dashboardStats,
    ["ingestionRate", "logsPerSecond", "rate"],
    0
  );

  const averageResponse = getValue(
    dashboardStats,
    [
      "averageResponse",
      "averageResponseTime",
      "avgResponseTime",
    ],
    0
  );

  // =========================================================
  // SAMPLE CHART DATA
  // =========================================================

  const chartValues = [
    32,
    45,
    38,
    58,
    44,
    66,
    51,
    72,
    61,
    80,
    63,
    91,
    75,
    68,
    84,
    73,
    88,
    69,
    95,
    78,
  ];

  // =========================================================
  // NAVIGATION
  // =========================================================

  const navigation = [
    {
      id: "dashboard",
      icon: "▦",
      label: "Dashboard",
    },
    {
      id: "search",
      icon: "⌕",
      label: "Log Search",
    },
    {
      id: "alerts",
      icon: "◈",
      label: "Alerts",
    },
    {
      id: "services",
      icon: "◇",
      label: "Services",
    },
    {
      id: "analytics",
      icon: "◒",
      label: "Analytics",
    },
  ];

  // =========================================================
  // DASHBOARD
  // =========================================================

  const renderDashboard = () => (
    <>
      <section className="hero">
        <div>
          <div className="eyebrow">
            DISTRIBUTED LOG OBSERVABILITY
          </div>

          <h1>
            LOG<span>STREAM</span>
          </h1>

          <p className="hero-description">
            A distributed log analytics and alerting platform
            built for high-volume application observability,
            fast search, indexing, and real-time monitoring.
          </p>

          {lastUpdated && (
            <p className="last-updated">
              Last updated:{" "}
              {lastUpdated.toLocaleTimeString()}
            </p>
          )}
        </div>

        <div className="hero-metric">
          <span>INGESTION RATE</span>

          <strong>
            {Number(ingestionRate).toLocaleString()}
          </strong>

          <small>LOGS / SECOND</small>
        </div>
      </section>

      <section className="stats-grid">
        <div className="stat-card">
          <div className="stat-top">
            <span>TOTAL LOGS</span>
            <span className="stat-icon">◉</span>
          </div>

          <strong>
            {statsLoading
              ? "..."
              : Number(totalLogs).toLocaleString()}
          </strong>

          <small>INDEXED LOG RECORDS</small>
        </div>

        <div className="stat-card error-card">
          <div className="stat-top">
            <span>ERRORS</span>
            <span className="stat-icon">!</span>
          </div>

          <strong>
            {statsLoading
              ? "..."
              : Number(errorCount).toLocaleString()}
          </strong>

          <small>ERROR LOG ENTRIES</small>
        </div>

        <div className="stat-card">
          <div className="stat-top">
            <span>WARNINGS</span>
            <span className="stat-icon">△</span>
          </div>

          <strong>
            {statsLoading
              ? "..."
              : Number(warningCount).toLocaleString()}
          </strong>

          <small>WARNING LOG ENTRIES</small>
        </div>

        <div className="stat-card">
          <div className="stat-top">
            <span>AVG RESPONSE</span>
            <span className="stat-icon">◷</span>
          </div>

          <strong>
            {statsLoading
              ? "..."
              : `${averageResponse} ms`}
          </strong>

          <small>AVERAGE RESPONSE TIME</small>
        </div>
      </section>

      <section className="dashboard-grid">
        <div className="panel large-panel">
          <div className="panel-header">
            <div>
              <div className="panel-label">
                INGESTION
              </div>

              <h2>Log Volume</h2>
            </div>

            <select defaultValue="24h">
              <option value="1h">1 HOUR</option>
              <option value="6h">6 HOURS</option>
              <option value="24h">24 HOURS</option>
              <option value="7d">7 DAYS</option>
            </select>
          </div>

          <div className="chart">
            <div className="chart-grid">
              <span>100</span>
              <span>75</span>
              <span>50</span>
              <span>25</span>
              <span>0</span>
            </div>

            <div className="bars">
              {chartValues.map((value, index) => (
                <div
                  key={index}
                  className="bar"
                  style={{
                    height: `${value}%`,
                  }}
                  title={`${value} units`}
                />
              ))}
            </div>
          </div>
        </div>

        <div className="panel">
          <div className="panel-header">
            <div>
              <div className="panel-label">
                SERVICES
              </div>

              <h2>System Status</h2>
            </div>
          </div>

          <div className="service-list">
            <div className="service">
              <div>
                <strong>billing-api</strong>
              </div>

              <span className="positive">
                ● ONLINE
              </span>
            </div>

            <div className="service">
              <div>
                <strong>auth-service</strong>
              </div>

              <span className="positive">
                ● ONLINE
              </span>
            </div>

            <div className="service">
              <div>
                <strong>payment-service</strong>
              </div>

              <span className="positive">
                ● ONLINE
              </span>
            </div>

            <div className="service">
              <div>
                <strong>notification-service</strong>
              </div>

              <span className="positive">
                ● ONLINE
              </span>
            </div>
          </div>
        </div>
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <div className="panel-label">
              RECENT ACTIVITY
            </div>

            <h2>Latest Logs</h2>
          </div>

          <button
            className="secondary-button"
            onClick={() => setActivePage("search")}
          >
            VIEW ALL
          </button>
        </div>

        <div className="logs-table-wrapper">
          <table className="logs-table">
            <thead>
              <tr>
                <th>TIME</th>
                <th>LEVEL</th>
                <th>SERVICE</th>
                <th>HOST</th>
                <th>MESSAGE</th>
              </tr>
            </thead>

            <tbody>
              {logs.length > 0 ? (
                logs.slice(0, 8).map((log, index) => {
                  const item = formatLog(log);

                  return (
                    <tr key={index}>
                      <td>{item.timestamp}</td>

                      <td>
                        <span
                          className={
                            item.level === "ERROR"
                              ? "log-level error"
                              : item.level === "WARN"
                              ? "log-level warning"
                              : "log-level"
                          }
                        >
                          {item.level}
                        </span>
                      </td>

                      <td>{item.service}</td>

                      <td>{item.host}</td>

                      <td>{item.message}</td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td
                    colSpan="5"
                    className="empty-state"
                  >
                    No logs loaded. Run a search to view
                    log records.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );

  // =========================================================
  // SEARCH PAGE
  // =========================================================

  const renderSearch = () => (
    <>
      <section className="page-heading">
        <div className="eyebrow">
          QUERY ENGINE
        </div>

        <h1 className="page-title">
          LOG SEARCH
        </h1>
      </section>

      <section className="search-section">
        <div className="search-label">
          <span>⌕</span>
          QUERY
        </div>

        <form
          className="search-box"
          onSubmit={runSearch}
        >
          <input
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setQueryValid(null);
              setSearchMessage("");
            }}
            placeholder="Enter a log query..."
          />

          <button
            type="submit"
            disabled={loading}
          >
            {loading ? "SEARCHING..." : "SEARCH"}
          </button>
        </form>

        <div className="query-examples">
          <span>EXAMPLES:</span>

          <button
            type="button"
            onClick={() =>
              useQuery("level:ERROR")
            }
          >
            level:ERROR
          </button>

          <button
            type="button"
            onClick={() =>
              useQuery("service:billing-api")
            }
          >
            service:billing-api
          </button>

          <button
            type="button"
            onClick={() =>
              useQuery(
                "level:ERROR AND service:billing-api"
              )
            }
          >
            level:ERROR AND service:billing-api
          </button>

          <button
            type="button"
            onClick={() =>
              useQuery("status:500")
            }
          >
            status:500
          </button>
        </div>

        <div className="query-actions">
          <button
            className="secondary-button"
            type="button"
            onClick={validateQuery}
          >
            VALIDATE QUERY
          </button>

          {queryValid === true && (
            <span className="query-valid">
              ✓ QUERY VALID
            </span>
          )}

          {queryValid === false && (
            <span className="query-invalid">
              ✕ QUERY INVALID
            </span>
          )}
        </div>

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        {searchMessage && (
          <div className="search-result-message">
            {searchMessage}
          </div>
        )}
      </section>

      <section className="panel">
        <div className="panel-header">
          <div>
            <div className="panel-label">
              SEARCH RESULTS
            </div>

            <h2>
              {logs.length} Log Records
            </h2>
          </div>
        </div>

        <div className="logs-table-wrapper">
          <table className="logs-table">
            <thead>
              <tr>
                <th>TIME</th>
                <th>LEVEL</th>
                <th>SERVICE</th>
                <th>HOST</th>
                <th>MESSAGE</th>
              </tr>
            </thead>

            <tbody>
              {logs.length > 0 ? (
                logs.map((log, index) => {
                  const item = formatLog(log);

                  return (
                    <tr key={index}>
                      <td>{item.timestamp}</td>

                      <td>
                        <span
                          className={
                            item.level === "ERROR"
                              ? "log-level error"
                              : item.level === "WARN"
                              ? "log-level warning"
                              : "log-level"
                          }
                        >
                          {item.level}
                        </span>
                      </td>

                      <td>{item.service}</td>

                      <td>{item.host}</td>

                      <td>{item.message}</td>
                    </tr>
                  );
                })
              ) : (
                <tr>
                  <td
                    colSpan="5"
                    className="empty-state"
                  >
                    No search results.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </section>
    </>
  );

  // =========================================================
  // ALERTS PAGE
  // =========================================================

  const renderAlerts = () => (
    <>
      <section className="page-heading">
        <div className="eyebrow">
          REAL-TIME MONITORING
        </div>

        <h1 className="page-title">
          ALERTS
        </h1>
      </section>

      {error && (
        <div className="error-message">
          {error}
        </div>
      )}

      <section className="alerts-layout">
        <div className="panel">
          <div className="panel-label">
            CREATE ALERT
          </div>

          <h2>New Alert Rule</h2>

          <form
            className="alert-form"
            onSubmit={handleCreateAlert}
          >
            <label>
              NAME

              <input
                value={alertForm.name}
                onChange={(event) =>
                  updateAlertForm(
                    "name",
                    event.target.value
                  )
                }
                placeholder="High Error Rate"
              />
            </label>

            <label>
              QUERY

              <input
                value={alertForm.query}
                onChange={(event) =>
                  updateAlertForm(
                    "query",
                    event.target.value
                  )
                }
                placeholder="level:ERROR"
              />
            </label>

            <div className="form-row">
              <label>
                THRESHOLD

                <input
                  type="number"
                  min="1"
                  value={alertForm.threshold}
                  onChange={(event) =>
                    updateAlertForm(
                      "threshold",
                      event.target.value
                    )
                  }
                />
              </label>

              <label>
                WINDOW (MIN)

                <input
                  type="number"
                  min="1"
                  value={alertForm.window}
                  onChange={(event) =>
                    updateAlertForm(
                      "window",
                      event.target.value
                    )
                  }
                />
              </label>
            </div>

            <label>
              WEBHOOK URL

              <input
                value={alertForm.webhookUrl}
                onChange={(event) =>
                  updateAlertForm(
                    "webhookUrl",
                    event.target.value
                  )
                }
                placeholder="https://hooks.slack.com/..."
              />
            </label>

            <button
              className="create-button"
              type="submit"
              disabled={alertLoading}
            >
              {alertLoading
                ? "CREATING..."
                : "CREATE ALERT"}
            </button>
          </form>
        </div>

        <div className="panel">
          <div className="panel-label">
            ACTIVE RULES
          </div>

          <h2>
            {alerts.length} Alerts
          </h2>

          <div className="alert-list">
            {alerts.length > 0 ? (
              alerts.map((alert, index) => {
                const id =
                  alert.id ??
                  alert.alertId ??
                  index;

                return (
                  <div
                    className="alert-item"
                    key={id}
                  >
                    <div className="alert-item-top">
                      <div>
                        <strong>
                          {alert.name ||
                            "Unnamed Alert"}
                        </strong>

                        <span>
                          {alert.query ||
                            "No query"}
                        </span>
                      </div>

                      <span className="alert-status">
                        ● ACTIVE
                      </span>
                    </div>

                    <div className="alert-details">
                      <span>
                        Threshold:{" "}
                        {alert.threshold ?? "-"}
                      </span>

                      <span>
                        Window:{" "}
                        {alert.window ?? "-"} min
                      </span>
                    </div>

                    <div className="alert-actions">
                      <button
                        className="secondary-button"
                        onClick={() =>
                          handleEvaluateAlert(id)
                        }
                        disabled={alertLoading}
                      >
                        EVALUATE
                      </button>

                      <button
                        className="delete-button"
                        onClick={() =>
                          handleDeleteAlert(id)
                        }
                        disabled={alertLoading}
                      >
                        DELETE
                      </button>
                    </div>
                  </div>
                );
              })
            ) : (
              <div className="empty-state">
                No alerts configured.
              </div>
            )}
          </div>
        </div>
      </section>
    </>
  );

  // =========================================================
  // SERVICES PAGE
  // =========================================================

  const renderServices = () => {
    const services = [
      {
        name: "billing-api",
        port: "8081",
        status: "ONLINE",
        description:
          "Billing and transaction processing service",
      },
      {
        name: "auth-service",
        port: "8082",
        status: "ONLINE",
        description:
          "Authentication and authorization service",
      },
      {
        name: "payment-service",
        port: "8083",
        status: "ONLINE",
        description:
          "Payment processing service",
      },
      {
        name: "notification-service",
        port: "8084",
        status: "ONLINE",
        description:
          "Notification and messaging service",
      },
    ];

    return (
      <>
        <section className="page-heading">
          <div className="eyebrow">
            SYSTEM COMPONENTS
          </div>

          <h1 className="page-title">
            SERVICES
          </h1>
        </section>

        <section className="service-grid">
          {services.map((service) => (
            <div
              className="panel service-card"
              key={service.name}
            >
              <div className="service-card-header">
                <div>
                  <div className="panel-label">
                    SERVICE
                  </div>

                  <h2>{service.name}</h2>
                </div>

                <span className="positive">
                  ● {service.status}
                </span>
              </div>

              <p>
                {service.description}
              </p>

              <div className="service-meta">
                <span>
                  PORT: {service.port}
                </span>

                <span>
                  HEALTH: OK
                </span>
              </div>
            </div>
          ))}
        </section>
      </>
    );
  };

  // =========================================================
  // ANALYTICS PAGE
  // =========================================================

  const renderAnalytics = () => {
    const errorRate =
      totalLogs > 0
        ? ((Number(errorCount) / Number(totalLogs)) *
            100
          ).toFixed(2)
        : "0.00";

    const warningRate =
      totalLogs > 0
        ? ((Number(warningCount) /
            Number(totalLogs)) *
            100
          ).toFixed(2)
        : "0.00";

    return (
      <>
        <section className="page-heading">
          <div className="eyebrow">
            OBSERVABILITY METRICS
          </div>

          <h1 className="page-title">
            ANALYTICS
          </h1>
        </section>

        <section className="stats-grid">
          <div className="stat-card">
            <div className="stat-top">
              <span>ERROR RATE</span>
              <span className="stat-icon">%</span>
            </div>

            <strong>{errorRate}%</strong>

            <small>
              OF TOTAL LOGS
            </small>
          </div>

          <div className="stat-card">
            <div className="stat-top">
              <span>WARNING RATE</span>
              <span className="stat-icon">%</span>
            </div>

            <strong>{warningRate}%</strong>

            <small>
              OF TOTAL LOGS
            </small>
          </div>

          <div className="stat-card">
            <div className="stat-top">
              <span>INGESTION</span>
              <span className="stat-icon">↗</span>
            </div>

            <strong>
              {Number(
                ingestionRate
              ).toLocaleString()}
            </strong>

            <small>
              LOGS / SECOND
            </small>
          </div>

          <div className="stat-card">
            <div className="stat-top">
              <span>RESPONSE</span>
              <span className="stat-icon">◷</span>
            </div>

            <strong>
              {averageResponse} ms
            </strong>

            <small>
              AVERAGE LATENCY
            </small>
          </div>
        </section>

        <section className="dashboard-grid">
          <div className="panel">
            <div className="panel-header">
              <div>
                <div className="panel-label">
                  LOG DISTRIBUTION
                </div>

                <h2>
                  Error Monitoring
                </h2>
              </div>
            </div>

            <div className="analytics-alert">
              <strong>
                {errorCount}
              </strong>

              <span>
                TOTAL ERROR LOGS
              </span>
            </div>
          </div>

          <div className="panel">
            <div className="panel-header">
              <div>
                <div className="panel-label">
                  WARNING MONITORING
                </div>

                <h2>
                  Warning Activity
                </h2>
              </div>
            </div>

            <div className="analytics-alert">
              <strong>
                {warningCount}
              </strong>

              <span>
                TOTAL WARNING LOGS
              </span>
            </div>
          </div>
        </section>
      </>
    );
  };

  // =========================================================
  // PAGE RENDERER
  // =========================================================

  const renderPage = () => {
    switch (activePage) {
      case "search":
        return renderSearch();

      case "alerts":
        return renderAlerts();

      case "services":
        return renderServices();

      case "analytics":
        return renderAnalytics();

      case "dashboard":
      default:
        return renderDashboard();
    }
  };

  // =========================================================
  // MAIN RETURN
  // =========================================================

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">
            L
          </div>

          <div>
            <h2>LOGSTREAM</h2>

            <span>
              OBSERVABILITY PLATFORM
            </span>
          </div>
        </div>

        <nav className="navigation">
          {navigation.map((item) => (
            <button
              key={item.id}
              className={`nav-item ${
                activePage === item.id
                  ? "active"
                  : ""
              }`}
              onClick={() =>
                setActivePage(item.id)
              }
            >
              <span>{item.icon}</span>

              {item.label}
            </button>
          ))}
        </nav>
      </aside>

      <main className="main">
        <header className="topbar">
          <div className="breadcrumb">
            LOGSTREAM
            <span>/</span>
            {navigation.find(
              (item) =>
                item.id === activePage
            )?.label || "Dashboard"}
          </div>

          <div className="topbar-right">
            <button
              className="icon-button"
              onClick={() => {
                loadStats();
                loadAlerts();
              }}
              title="Refresh"
            >
              ↻
            </button>

            <div className="profile">
              <div className="avatar">
                L
              </div>

              <span>
                LOGSTREAM ADMIN
              </span>
            </div>
          </div>
        </header>

        <div className="content">
          {renderPage()}
        </div>

        <footer>
          <span>
            LOGSTREAM · DISTRIBUTED LOG ANALYTICS
          </span>

          <span>
            ● SYSTEM OPERATIONAL
          </span>
        </footer>
      </main>
    </div>
  );
}

export default App;