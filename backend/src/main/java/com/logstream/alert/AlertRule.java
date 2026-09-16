package com.logstream.alert;

/**
 * A user-defined rule, e.g. "more than 100 ERRORs from billing-api in 5 minutes".
 * The window is folded into the query at evaluation time as timestamp >= now-Nm.
 */
public class AlertRule {
    private String id;
    private String name;
    private String query;          // e.g. level:ERROR AND service:billing-api
    private long threshold = 100;  // fire when count exceeds this
    private int windowMinutes = 5;
    private String webhookUrl;     // optional
    private String email;          // optional, logged rather than sent in the demo
    private boolean enabled = true;

    private long lastEvaluatedAt;
    private long lastCount;
    private long lastFiredAt;
    private int timesFired;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public long getThreshold() { return threshold; }
    public void setThreshold(long threshold) { this.threshold = threshold; }

    public int getWindowMinutes() { return windowMinutes; }
    public void setWindowMinutes(int windowMinutes) { this.windowMinutes = windowMinutes; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public long getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(long v) { this.lastEvaluatedAt = v; }

    public long getLastCount() { return lastCount; }
    public void setLastCount(long v) { this.lastCount = v; }

    public long getLastFiredAt() { return lastFiredAt; }
    public void setLastFiredAt(long v) { this.lastFiredAt = v; }

    public int getTimesFired() { return timesFired; }
    public void setTimesFired(int v) { this.timesFired = v; }

    /** Restricts the rule's query to its evaluation window. */
    public String windowedQuery() {
        return "(" + query + ") AND timestamp >= now-" + windowMinutes + "m";
    }
}
