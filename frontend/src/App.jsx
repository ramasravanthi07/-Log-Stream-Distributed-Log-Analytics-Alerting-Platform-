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
  /* =========================
     STATE
  ========================= */

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

  /* New alert form */

  const [alertForm, setAlertForm] = useState({
    name: "",
    query: "level:ERROR AND service:billing-api",
    threshold: 100,
    window: 5,
    webhookUrl: "",
  });


  /* =========================
     HELPER FUNCTIONS
  ========================= */

  const getValue = (obj, keys, fallback = 0) => {
    if (!obj || typeof obj !== "object") return fallback;

    for (const key of keys) {
      if (
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

    if (!data || typeof data !== "object") {
      return [];
    }

    if (Array.isArray(data.logs)) {
      return data.logs;
    }

    if (Array.isArray(data.results)) {
      return data.results;
    }

    if (Array.isArray(data.hits)) {
      return data.hits;
    }

    if (
      data.hits &&
      Array.isArray(data.hits.hits)
    ) {
      return data.hits.hits.map((item) => {
        return item._source || item;
      });
    }

    return [];
  };


  const extractAlerts = (data) => {
    if (Array.isArray(data)) {
      return data;
    }

    if (!data || typeof data !== "object") {
      return [];
    }

    if (Array.isArray(data.alerts)) {
      return data.alerts;
    }

    if (Array.isArray(data.rules)) {
      return data.rules;
    }

    if (Array.isArray(data.results)) {
      return data.results;
    }

    return [];
  };


  /* =========================
     LOAD DASHBOARD
  ========================= */

  const loadStats = async () => {
    try {
      setStatsLoading(true);

      const data = await stats();

      setDashboardStats(data || {});
    } catch (err) {
      console.error("Stats error:", err);
    } finally {
      setStatsLoading(false);
    }
  };


  const loadAlerts = async () => {
    try {
      const data = await listAlerts();

      setAlerts(extractAlerts(data));
    } catch (err) {
      console.error("Alerts error:", err);
    }
  };


  /* =========================
     INITIAL LOAD
  ========================= */

  useEffect(() => {
    loadStats();
    loadAlerts();
  }, []);


  /* =========================
     SEARCH
  ========================= */

  const runSearch = async (customQuery = null) => {
    const finalQuery =
      customQuery !== null
        ? customQuery
        : query;

    if (!finalQuery.trim()) {
      setError("Please enter a search query.");
      return;
    }

    try {
      setLoading(true);
      setError("");
      setSearchMessage("");

      const result = await search({
        q: finalQuery,
        from: 0,
        size: 50,
        buckets: 60,
      });

      const resultLogs = extractLogs(result);

      setLogs(resultLogs);

      if (resultLogs.length === 0) {
        setSearchMessage(
          "No matching logs found."
        );
      } else {
        setSearchMessage(
          `${resultLogs.length} matching logs found.`
        );
      }

    } catch (err) {
      console.error("Search error:", err);

      setError(
        err.message ||
        "Unable to search logs."
      );

      setLogs([]);

    } finally {
      setLoading(false);
    }
  };


  /* =========================
     QUERY VALIDATION
  ========================= */

  const validateQuery = async () => {
    if (!query.trim()) return;

    try {
      const result = await validate(query);

      /*
       * Backend may return:
       * { valid: true }
       * or another validation structure.
       */

      if (result && result.valid !== undefined) {
        setQueryValid(result.valid);
      } else {
        setQueryValid(true);
      }

    } catch (err) {
      console.error(
        "Validation error:",
        err
      );

      setQueryValid(false);
    }
  };


  /* =========================
     SEARCH EXAMPLE
  ========================= */

  const useQuery = (newQuery) => {
    setQuery(newQuery);
    setQueryValid(null);
    setError("");

    runSearch(newQuery);
  };


  /* =========================
     CREATE ALERT
  ========================= */

  const handleCreateAlert = async (event) => {
    event.preventDefault();

    if (!alertForm.name.trim()) {
      setError("Please enter an alert name.");
      return;
    }

    if (!alertForm.query.trim()) {
      setError("Please enter an alert query.");
      return;
    }

    try {
      setAlertLoading(true);
      setError("");

      const rule = {
        name: alertForm.name,
        query: alertForm.query,
        threshold: Number(
          alertForm.threshold
        ),
        window: Number(
          alertForm.window
        ),
      };

      if (
        alertForm.webhookUrl.trim()
      ) {
        rule.webhookUrl =
          alertForm.webhookUrl;
      }

      await createAlert(rule);

      setAlertForm({
        name: "",
        query:
          "level:ERROR AND service:billing-api",
        threshold: 100,
        window: 5,
        webhookUrl: "",
      });

      await loadAlerts();

    } catch (err) {
      console.error(
        "Create alert error:",
        err
      );

      setError(
        err.message ||
        "Unable to create alert."
      );

    } finally {
      setAlertLoading(false);
    }
  };


  /* =========================
     DELETE ALERT
  ========================= */

  const handleDeleteAlert = async (id) => {
    if (!id) return;

    try {
      setAlertLoading(true);
      setError("");

      await deleteAlert(id);

      await loadAlerts();

    } catch (err) {
      console.error(
        "Delete alert error:",
        err
      );

      setError(
        err.message ||
        "Unable to delete alert."
      );

    } finally {
      setAlertLoading(false);
    }
  };


  /* =========================
     EVALUATE ALERT
  ========================= */

  const handleEvaluateAlert = async (id) => {
    if (!id) return;

    try {
      setAlertLoading(true);
      setError("");

      await evaluateAlert(id);

      await loadAlerts();
      await loadStats();

    } catch (err) {
      console.error(
        "Evaluate alert error:",
        err
      );

      setError(
        err.message ||
        "Unable to evaluate alert."
      );

    } finally {
      setAlertLoading(false);
    }
  };


  /* =========================
     FORMAT LOG
  ========================= */

  const formatLog = (log) => {
    if (!log) {
      return {
        time: "-",
        level: "INFO",
        service: "-",
        message: "-",
        response: "-",
      };
    }

    const source =
      log._source || log;

    return {
      time: getValue(
        source,
        [
          "timestamp",
          "time",
          "@timestamp",
          "created_at",
        ],
        "-"
      ),

      level: String(
        getValue(
          source,
          ["level", "severity"],
          "INFO"
        )
      ).toUpperCase(),

      service: getValue(
        source,
        [
          "service",
          "service_name",
          "serviceName",
        ],
        "-"
      ),

      message: getValue(
        source,
        [
          "message",
          "msg",
          "log",
        ],
        "-"
      ),

      response: getValue(
        source,
        [
          "response_time",
          "responseTime",
          "latency",
        ],
        "-"
      ),
    };
  };


  /* =========================
     STAT VALUES
  ========================= */

  const totalLogs = getValue(
    dashboardStats,
    [
      "total",
      "totalLogs",
      "total_logs",
      "count",
      "indexed",
      "lines",
    ],
    0
  );

  const errorCount = getValue(
    dashboardStats,
    [
      "errors",
      "errorCount",
      "error_count",
    ],
    0
  );

  const warningCount = getValue(
    dashboardStats,
    [
      "warnings",
      "warningCount",
      "warning_count",
      "warn",
    ],
    0
  );

  const ingestionRate = getValue(
    dashboardStats,
    [
      "ingestionRate",
      "ingestion_rate",
      "logsPerSecond",
      "logs_per_second",
      "throughput",
    ],
    0
  );

  const averageResponse = getValue(
    dashboardStats,
    [
      "avgResponse",
      "avg_response",
      "averageResponseTime",
      "average_response_time",
    ],
    0
  );


  /* =========================
     RENDER
  ========================= */

  return (
    <div className="app">

      {/* =====================================================
          SIDEBAR
      ===================================================== */}

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

          <button
            className={
              activePage === "dashboard"
                ? "nav-item active"
                : "nav-item"
            }
            onClick={() =>
              setActivePage("dashboard")
            }
          >
            <span>▦</span>
            Dashboard
          </button>


          <button
            className={
              activePage === "search"
                ? "nav-item active"
                : "nav-item"
            }
            onClick={() =>
              setActivePage("search")
            }
          >
            <span>⌕</span>
            Log Search
          </button>


          <button
            className={
              activePage === "alerts"
                ? "nav-item active"
                : "nav-item"
            }
            onClick={() =>
              setActivePage("alerts")
            }
          >
            <span>♢</span>
            Alerts
          </button>


          <button
            className={
              activePage === "services"
                ? "nav-item active"
                : "nav-item"
            }
            onClick={() =>
              setActivePage("services")
            }
          >
            <span>◈</span>
            Services
          </button>


          <button
            className={
              activePage === "analytics"
                ? "nav-item active"
                : "nav-item"
            }
            onClick={() =>
              setActivePage("analytics")
            }
          >
            <span>◌</span>
            Analytics
          </button>

        </nav>


        <div className="sidebar-bottom">

          <div className="system-status">

            <span className="status-dot"></span>

            <div>

              <strong>
                System Operational
              </strong>

              <small>
                Backend connected
              </small>

            </div>

          </div>


          <div className="version">
            LOGSTREAM v1.0
          </div>

        </div>

      </aside>


      {/* =====================================================
          MAIN
      ===================================================== */}

      <main className="main">

        {/* TOP BAR */}

        <header className="topbar">

          <div>

            <span className="breadcrumb">
              OBSERVABILITY /
            </span>

            <strong>
              {" "}
              {activePage.toUpperCase()}
            </strong>

          </div>


          <div className="top-actions">

            <span className="live-indicator">

              <span></span>

              LIVE

            </span>


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
                D
              </div>

              <span>
                DevOps Engineer
              </span>

            </div>

          </div>

        </header>


        {/* =====================================================
            DASHBOARD
        ===================================================== */}

        {activePage === "dashboard" && (

          <section className="content">

            {/* HERO */}

            <div className="hero">

              <div>

                <p className="eyebrow">
                  REAL-TIME LOG INTELLIGENCE
                </p>

                <h1>
                  SEE EVERYTHING.
                  <br />

                  <span>
                    MISS NOTHING.
                  </span>
                </h1>

                <p className="hero-description">
                  Search, analyze and monitor
                  millions of application logs
                  in real time with LogStream.
                </p>

              </div>


              <div className="hero-metric">

                <span>
                  INGESTION RATE
                </span>

                <strong>
                  {statsLoading
                    ? "..."
                    : Number(
                        ingestionRate || 0
                      ).toLocaleString()}
                </strong>

                <small>
                  logs / second
                </small>

              </div>

            </div>


            {/* SEARCH */}

            <div className="search-section">

              <div className="search-label">

                <span>⌕</span>

                QUERY LOGS

              </div>


              <div className="search-box">

                <input
                  value={query}
                  onChange={(e) => {
                    setQuery(
                      e.target.value
                    );

                    setQueryValid(null);
                  }}
                  onKeyDown={(e) => {
                    if (
                      e.key === "Enter"
                    ) {
                      runSearch();
                    }
                  }}
                  placeholder="level:ERROR AND service:billing-api AND response_time > 1000"
                />


                <button
                  onClick={() =>
                    runSearch()
                  }
                  disabled={loading}
                >
                  {loading
                    ? "SEARCHING..."
                    : "SEARCH"}
                </button>

              </div>


              <div className="query-examples">

                <span>
                  Try:
                </span>


                <button
                  onClick={() =>
                    useQuery(
                      "level:ERROR"
                    )
                  }
                >
                  level:ERROR
                </button>


                <button
                  onClick={() =>
                    useQuery(
                      "service:billing-api"
                    )
                  }
                >
                  service:billing-api
                </button>


                <button
                  onClick={() =>
                    useQuery(
                      "response_time > 1000"
                    )
                  }
                >
                  response_time &gt; 1000
                </button>


                <button
                  onClick={() =>
                    useQuery(
                      "level:ERROR AND service:billing-api AND response_time > 1000"
                    )
                  }
                >
                  Complex query
                </button>

              </div>


              <div className="query-actions">

                <button
                  className="secondary-button"
                  onClick={validateQuery}
                >
                  Validate Query
                </button>


                {queryValid === true && (
                  <span className="query-valid">
                    ✓ Query valid
                  </span>
                )}


                {queryValid === false && (
                  <span className="query-invalid">
                    ✕ Invalid query
                  </span>
                )}

              </div>


              {error && (
                <div className="error-message">
                  {error}
                </div>
              )}

            </div>


            {/* STATS */}

            <div className="stats-grid">

              <div className="stat-card">

                <div className="stat-top">
                  <span>
                    TOTAL LOGS
                  </span>

                  <span className="stat-icon">
                    ◉
                  </span>
                </div>

                <strong>
                  {Number(
                    totalLogs || 0
                  ).toLocaleString()}
                </strong>

                <small>
                  Indexed records
                </small>

              </div>


              <div className="stat-card error-card">

                <div className="stat-top">

                  <span>
                    ERRORS
                  </span>

                  <span className="stat-icon">
                    !
                  </span>

                </div>

                <strong>
                  {Number(
                    errorCount || 0
                  ).toLocaleString()}
                </strong>

                <small className="negative">
                  ERROR logs
                </small>

              </div>


              <div className="stat-card">

                <div className="stat-top">

                  <span>
                    WARNINGS
                  </span>

                  <span className="stat-icon">
                    △
                  </span>

                </div>

                <strong>
                  {Number(
                    warningCount || 0
                  ).toLocaleString()}
                </strong>

                <small>
                  Warning logs
                </small>

              </div>


              <div className="stat-card">

                <div className="stat-top">

                  <span>
                    AVG RESPONSE
                  </span>

                  <span className="stat-icon">
                    ◷
                  </span>

                </div>

                <strong>
                  {averageResponse || 0} ms
                </strong>

                <small>
                  Average latency
                </small>

              </div>

            </div>


            {/* ANALYTICS */}

            <div className="dashboard-grid">

              <div className="panel large-panel">

                <div className="panel-header">

                  <div>

                    <span className="panel-label">
                      LOG VOLUME
                    </span>

                    <h2>
                      Logs over time
                    </h2>

                  </div>

                </div>


                <div className="chart">

                  <div className="chart-grid">

                    <span>20K</span>
                    <span>15K</span>
                    <span>10K</span>
                    <span>5K</span>
                    <span>0</span>

                  </div>


                  <div className="bars">

                    {[40, 55, 48, 72, 62, 85, 68, 92, 74, 96, 82, 100, 88, 95, 78, 91].map(
                      (height, index) => (

                        <div
                          className="bar"
                          style={{
                            height:
                              `${height}%`,
                          }}
                          key={index}
                        ></div>

                      )
                    )}

                  </div>

                </div>

              </div>


              {/* ACTIVE ALERTS */}

              <div className="panel">

                <div className="panel-header">

                  <div>

                    <span className="panel-label">
                      ALERTING
                    </span>

                    <h2>
                      Active rules
                    </h2>

                  </div>

                </div>


                <div className="service-list">

                  {alerts.length === 0 ? (

                    <div className="empty-state">
                      No alert rules configured.
                    </div>

                  ) : (

                    alerts.slice(0, 4).map(
                      (alert, index) => (

                        <div
                          className="service"
                          key={
                            alert.id ||
                            alert._id ||
                            index
                          }
                        >

                          <div className="service-info">

                            <span className="service-dot healthy"></span>

                            <div>

                              <strong>
                                {alert.name ||
                                  `Alert ${index + 1}`}
                              </strong>

                              <small>
                                {alert.query ||
                                  "Query rule"}
                              </small>

                            </div>

                          </div>

                          <span className="health healthy">
                            ACTIVE
                          </span>

                        </div>

                      )
                    )

                  )}

                </div>

              </div>

            </div>


            {/* RECENT LOGS */}

            <div className="panel logs-panel">

              <div className="panel-header">

                <div>

                  <span className="panel-label">
                    SEARCH RESULTS
                  </span>

                  <h2>
                    Recent logs
                  </h2>

                </div>


                <button
                  className="view-all"
                  onClick={() =>
                    setActivePage(
                      "search"
                    )
                  }
                >
                  View all logs →
                </button>

              </div>


              {searchMessage && (
                <div className="search-result-message">
                  {searchMessage}
                </div>
              )}


              <div className="log-table">

                <div className="table-head">

                  <span>TIME</span>
                  <span>LEVEL</span>
                  <span>SERVICE</span>
                  <span>MESSAGE</span>
                  <span>RESPONSE</span>

                </div>


                {logs.length === 0 ? (

                  <div className="empty-logs">
                    Run a search to display
                    matching logs.
                  </div>

                ) : (

                  logs.slice(0, 10).map(
                    (log, index) => {

                      const item =
                        formatLog(log);

                      return (

                        <div
                          className="log-row"
                          key={index}
                        >

                          <span className="log-time">
                            {item.time}
                          </span>

                          <span
                            className={`level ${item.level.toLowerCase()}`}
                          >
                            {item.level}
                          </span>

                          <span className="service-name">
                            {item.service}
                          </span>

                          <span className="message">
                            {item.message}
                          </span>

                          <span className="response">
                            {item.response}
                            {item.response !== "-" &&
                              " ms"}
                          </span>

                        </div>

                      );
                    }
                  )

                )}

              </div>

            </div>


            {/* ALERT */}

            {alerts.length > 0 && (

              <div className="alert-banner">

                <div className="alert-symbol">
                  !
                </div>

                <div>

                  <span>
                    ALERTING ENGINE
                  </span>

                  <strong>
                    {alerts.length} alert rule
                    {alerts.length !== 1 &&
                      "s"} configured
                  </strong>

                  <p>
                    Your alerting engine is
                    monitoring the indexed logs.
                  </p>

                </div>

                <button
                  onClick={() =>
                    setActivePage(
                      "alerts"
                    )
                  }
                >
                  MANAGE ALERTS →
                </button>

              </div>

            )}

          </section>

        )}


        {/* =====================================================
            SEARCH PAGE
        ===================================================== */}

        {activePage === "search" && (

          <section className="content">

            <div className="page-heading">

              <p className="eyebrow">
                LUCENE SEARCH
              </p>

              <h1 className="page-title">
                SEARCH YOUR LOGS.
              </h1>

              <p className="hero-description">
                Query indexed application logs
                using LogStream's search syntax.
              </p>

            </div>


            <div className="search-section">

              <div className="search-box">

                <input
                  value={query}
                  onChange={(e) =>
                    setQuery(
                      e.target.value
                    )
                  }
                  onKeyDown={(e) => {
                    if (
                      e.key === "Enter"
                    ) {
                      runSearch();
                    }
                  }}
                  placeholder="Enter Lucene query..."
                />

                <button
                  onClick={() =>
                    runSearch()
                  }
                  disabled={loading}
                >
                  {loading
                    ? "SEARCHING..."
                    : "SEARCH"}
                </button>

              </div>


              <div className="query-actions">

                <button
                  className="secondary-button"
                  onClick={
                    validateQuery
                  }
                >
                  Validate
                </button>

                {queryValid === true && (
                  <span className="query-valid">
                    ✓ Valid query
                  </span>
                )}

                {queryValid === false && (
                  <span className="query-invalid">
                    ✕ Invalid query
                  </span>
                )}

              </div>

            </div>


            <div className="panel logs-panel">

              <div className="panel-header">

                <div>

                  <span className="panel-label">
                    RESULTS
                  </span>

                  <h2>
                    {logs.length} logs
                  </h2>

                </div>

              </div>


              <div className="log-table">

                <div className="table-head">

                  <span>TIME</span>
                  <span>LEVEL</span>
                  <span>SERVICE</span>
                  <span>MESSAGE</span>
                  <span>RESPONSE</span>

                </div>


                {logs.map(
                  (log, index) => {

                    const item =
                      formatLog(log);

                    return (

                      <div
                        className="log-row"
                        key={index}
                      >

                        <span className="log-time">
                          {item.time}
                        </span>

                        <span
                          className={`level ${item.level.toLowerCase()}`}
                        >
                          {item.level}
                        </span>

                        <span className="service-name">
                          {item.service}
                        </span>

                        <span className="message">
                          {item.message}
                        </span>

                        <span className="response">
                          {item.response}
                        </span>

                      </div>

                    );

                  }
                )}

              </div>

            </div>

          </section>

        )}


        {/* =====================================================
            ALERTS PAGE
        ===================================================== */}

        {activePage === "alerts" && (

          <section className="content">

            <div className="page-heading">

              <p className="eyebrow">
                ALERTING ENGINE
              </p>

              <h1 className="page-title">
                NEVER MISS AN INCIDENT.
              </h1>

              <p className="hero-description">
                Create rules that continuously
                monitor your application logs.
              </p>

            </div>


            <div className="alerts-layout">

              {/* EXISTING RULES */}

              <div>

                <div className="section-title">
                  <span>RULES</span>
                </div>


                {alerts.length === 0 ? (

                  <div className="panel empty-state">
                    No alert rules yet.
                  </div>

                ) : (

                  alerts.map(
                    (alert, index) => {

                      const id =
                        alert.id ||
                        alert._id;

                      return (

                        <div
                          className="alert-card"
                          key={
                            id || index
                          }
                        >

                          <div className="alert-card-header">

                            <div>

                              <h3>
                                {alert.name ||
                                  `Alert ${index + 1}`}
                              </h3>

                              <span className="alert-status">
                                ACTIVE
                              </span>

                            </div>

                          </div>


                          <div className="alert-query">

                            {alert.query ||
                              "No query"}

                          </div>


                          <div className="alert-details">

                            Fires above{" "}
                            <strong>
                              {alert.threshold ??
                                alert.firesAbove ??
                                0}
                            </strong>

                            {" "}in{" "}

                            <strong>
                              {alert.window ??
                                alert.windowMinutes ??
                                5}
                            </strong>

                            {" "}minutes

                          </div>


                          <div className="alert-buttons">

                            <button
                              className="secondary-button"
                              onClick={() =>
                                handleEvaluateAlert(
                                  id
                                )
                              }
                              disabled={
                                alertLoading
                              }
                            >
                              Check now
                            </button>


                            <button
                              className="secondary-button"
                              onClick={() => {
                                setQuery(
                                  alert.query ||
                                    ""
                                );

                                setActivePage(
                                  "search"
                                );

                                runSearch(
                                  alert.query ||
                                    ""
                                );
                              }}
                            >
                              View matching logs
                            </button>


                            <button
                              className="delete-button"
                              onClick={() =>
                                handleDeleteAlert(
                                  id
                                )
                              }
                              disabled={
                                alertLoading
                              }
                            >
                              Delete
                            </button>

                          </div>

                        </div>

                      );

                    }
                  )

                )}

              </div>


              {/* CREATE RULE */}

              <div>

                <div className="section-title">
                  <span>NEW RULE</span>
                </div>


                <form
                  className="panel alert-form"
                  onSubmit={
                    handleCreateAlert
                  }
                >

                  <label>
                    Name

                    <input
                      value={
                        alertForm.name
                      }
                      onChange={(e) =>
                        setAlertForm({
                          ...alertForm,
                          name:
                            e.target.value,
                        })
                      }
                      placeholder="Checkout 500s"
                    />

                  </label>


                  <label>
                    Query

                    <input
                      value={
                        alertForm.query
                      }
                      onChange={(e) =>
                        setAlertForm({
                          ...alertForm,
                          query:
                            e.target.value,
                        })
                      }
                    />

                  </label>


                  <div className="form-row">

                    <label>
                      Fires above

                      <input
                        type="number"
                        value={
                          alertForm.threshold
                        }
                        onChange={(e) =>
                          setAlertForm({
                            ...alertForm,
                            threshold:
                              e.target.value,
                          })
                        }
                      />

                    </label>


                    <label>
                      Window (minutes)

                      <input
                        type="number"
                        value={
                          alertForm.window
                        }
                        onChange={(e) =>
                          setAlertForm({
                            ...alertForm,
                            window:
                              e.target.value,
                          })
                        }
                      />

                    </label>

                  </div>


                  <label>
                    Webhook URL (optional)

                    <input
                      value={
                        alertForm.webhookUrl
                      }
                      onChange={(e) =>
                        setAlertForm({
                          ...alertForm,
                          webhookUrl:
                            e.target.value,
                        })
                      }
                      placeholder="https://hooks.slack.com/..."
                    />

                  </label>


                  <button
                    className="create-button"
                    type="submit"
                    disabled={
                      alertLoading
                    }
                  >
                    {alertLoading
                      ? "CREATING..."
                      : "CREATE RULE"}
                  </button>

                </form>

              </div>

            </div>

          </section>

        )}


        {/* =====================================================
            SERVICES PAGE
        ===================================================== */}

        {activePage === "services" && (

          <section className="content">

            <div className="page-heading">

              <p className="eyebrow">
                MICROSERVICES
              </p>

              <h1 className="page-title">
                SERVICE HEALTH.
              </h1>

              <p className="hero-description">
                Monitor application activity
                across your distributed services.
              </p>

            </div>


            <div className="stats-grid">

              <div className="stat-card">
                <span className="panel-label">
                  TOTAL LOGS
                </span>

                <strong>
                  {Number(
                    totalLogs || 0
                  ).toLocaleString()}
                </strong>
              </div>


              <div className="stat-card error-card">
                <span className="panel-label">
                  ERRORS
                </span>

                <strong>
                  {Number(
                    errorCount || 0
                  ).toLocaleString()}
                </strong>
              </div>


              <div className="stat-card">
                <span className="panel-label">
                  WARNINGS
                </span>

                <strong>
                  {Number(
                    warningCount || 0
                  ).toLocaleString()}
                </strong>
              </div>

            </div>


            <div className="panel">

              <div className="panel-header">

                <div>

                  <span className="panel-label">
                    DISTRIBUTED SYSTEM
                  </span>

                  <h2>
                    Service activity
                  </h2>

                </div>

              </div>


              <div className="service-list">

                {[
                  "billing-api",
                  "payment-api",
                  "auth-service",
                  "user-service",
                  "order-service",
                ].map(
                  (service) => (

                    <div
                      className="service"
                      key={service}
                    >

                      <div className="service-info">

                        <span className="service-dot healthy"></span>

                        <div>

                          <strong>
                            {service}
                          </strong>

                          <small>
                            Connected to
                            LogStream
                          </small>

                        </div>

                      </div>


                      <span className="health healthy">
                        MONITORED
                      </span>

                    </div>

                  )
                )}

              </div>

            </div>

          </section>

        )}


        {/* =====================================================
            ANALYTICS PAGE
        ===================================================== */}

        {activePage === "analytics" && (

          <section className="content">

            <div className="page-heading">

              <p className="eyebrow">
                LOG ANALYTICS
              </p>

              <h1 className="page-title">
                FIND THE SIGNAL.
              </h1>

              <p className="hero-description">
                Analyze the behavior of your
                distributed applications.
              </p>

            </div>


            <div className="stats-grid">

              <div className="stat-card">

                <span className="panel-label">
                  INDEXED LOGS
                </span>

                <strong>
                  {Number(
                    totalLogs || 0
                  ).toLocaleString()}
                </strong>

              </div>


              <div className="stat-card error-card">

                <span className="panel-label">
                  ERROR LOGS
                </span>

                <strong>
                  {Number(
                    errorCount || 0
                  ).toLocaleString()}
                </strong>

              </div>


              <div className="stat-card">

                <span className="panel-label">
                  WARNINGS
                </span>

                <strong>
                  {Number(
                    warningCount || 0
                  ).toLocaleString()}
                </strong>

              </div>


              <div className="stat-card">

                <span className="panel-label">
                  THROUGHPUT
                </span>

                <strong>
                  {Number(
                    ingestionRate || 0
                  ).toLocaleString()}
                </strong>

                <small>
                  logs/sec
                </small>

              </div>

            </div>


            <div className="dashboard-grid">

              <div className="panel large-panel">

                <div className="panel-header">

                  <div>

                    <span className="panel-label">
                      TIME SERIES
                    </span>

                    <h2>
                      Log volume
                    </h2>

                  </div>

                </div>


                <div className="chart">

                  <div className="chart-grid">

                    <span>20K</span>
                    <span>15K</span>
                    <span>10K</span>
                    <span>5K</span>
                    <span>0</span>

                  </div>


                  <div className="bars">

                    {[35, 45, 50, 42, 70, 62, 80, 72, 91, 84, 96, 88, 100, 82, 94, 90].map(
                      (height, index) => (

                        <div
                          className="bar"
                          style={{
                            height:
                              `${height}%`,
                          }}
                          key={index}
                        ></div>

                      )
                    )}

                  </div>

                </div>

              </div>


              <div className="panel">

                <div className="panel-header">

                  <div>

                    <span className="panel-label">
                      ALERTS
                    </span>

                    <h2>
                      Monitoring
                    </h2>

                  </div>

                </div>


                <div className="analytics-alert">

                  <strong>
                    {alerts.length}
                  </strong>

                  <span>
                    active alert rules
                  </span>

                </div>


                <button
                  className="create-button"
                  onClick={() =>
                    setActivePage(
                      "alerts"
                    )
                  }
                >
                  MANAGE ALERTS
                </button>

              </div>

            </div>

          </section>

        )}


        {/* FOOTER */}

        <footer>

          <span>
            LOGSTREAM
          </span>

          <span>
            Distributed Log Analytics &
            Alerting Platform
          </span>

          <span>
            ● Backend Connected
          </span>

        </footer>

      </main>

    </div>
  );
}


export default App;