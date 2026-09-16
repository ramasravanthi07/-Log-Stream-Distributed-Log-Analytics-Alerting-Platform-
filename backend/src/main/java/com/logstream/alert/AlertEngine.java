package com.logstream.alert;

import com.logstream.search.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Runs every user-defined rule once a minute. Each evaluation is a Lucene
 * count() over the rule's window, so it costs a BKD range scan rather than
 * materialising documents -- a hundred rules still finish in milliseconds.
 *
 * A rule that stays above threshold is not re-notified every minute; it has to
 * recover below threshold first (simple hysteresis, keeps the demo quiet).
 */
@Service
public class AlertEngine {

    private static final Logger log = LoggerFactory.getLogger(AlertEngine.class);

    private final SearchService search;
    private final RestClient http = RestClient.create();

    private final Map<String, AlertRule> rules = new ConcurrentHashMap<>();
    private final Set<String> firing = ConcurrentHashMap.newKeySet();
    private final Deque<AlertEvent> history = new ConcurrentLinkedDeque<>();

    public AlertEngine(SearchService search) {
        this.search = search;
        seedDemoRules();
    }

    private void seedDemoRules() {
        AlertRule r = new AlertRule();
        r.setName("Billing API error surge");
        r.setQuery("level:ERROR AND service:billing-api");
        r.setThreshold(100);
        r.setWindowMinutes(5);
        save(r);

        AlertRule slow = new AlertRule();
        slow.setName("Slow responses across the fleet");
        slow.setQuery("response_time > 2000");
        slow.setThreshold(50);
        slow.setWindowMinutes(5);
        save(slow);
    }

    @Scheduled(fixedRateString = "${logstream.alerts.interval-ms:60000}", initialDelay = 15000)
    public void evaluateAll() {
        for (AlertRule rule : rules.values()) {
            if (!rule.isEnabled()) continue;
            try {
                evaluate(rule);
            } catch (Exception e) {
                log.warn("Rule '{}' failed to evaluate: {}", rule.getName(), e.toString());
            }
        }
    }

    public AlertEvent evaluate(AlertRule rule) throws Exception {
        long count = search.count(rule.windowedQuery());
        rule.setLastCount(count);
        rule.setLastEvaluatedAt(System.currentTimeMillis());

        boolean breach = count > rule.getThreshold();
        boolean wasFiring = firing.contains(rule.getId());

        if (breach && !wasFiring) {
            firing.add(rule.getId());
            rule.setLastFiredAt(System.currentTimeMillis());
            rule.setTimesFired(rule.getTimesFired() + 1);
            AlertEvent ev = new AlertEvent(rule.getId(), rule.getName(), count,
                    rule.getThreshold(), System.currentTimeMillis(), "FIRING");
            record(ev);
            notify(rule, ev);
            return ev;
        }
        if (!breach && wasFiring) {
            firing.remove(rule.getId());
            AlertEvent ev = new AlertEvent(rule.getId(), rule.getName(), count,
                    rule.getThreshold(), System.currentTimeMillis(), "RESOLVED");
            record(ev);
            notify(rule, ev);
            return ev;
        }
        return null;
    }

    private void notify(AlertRule rule, AlertEvent ev) {
        log.warn("[{}] {} -- {} matches in last {}m (threshold {})",
                ev.state(), rule.getName(), ev.count(), rule.getWindowMinutes(), rule.getThreshold());

        if (rule.getEmail() != null && !rule.getEmail().isBlank()) {
            log.info("Email would go to {} : {}", rule.getEmail(), ev.summary());
        }
        if (rule.getWebhookUrl() == null || rule.getWebhookUrl().isBlank()) return;

        try {
            http.post()
                    .uri(rule.getWebhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "text", ev.summary(),
                            "rule", rule.getName(),
                            "query", rule.getQuery(),
                            "count", ev.count(),
                            "threshold", ev.threshold(),
                            "state", ev.state()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Webhook to {} failed: {}", rule.getWebhookUrl(), e.toString());
        }
    }

    private void record(AlertEvent ev) {
        history.addFirst(ev);
        while (history.size() > 200) history.removeLast();
    }

    public AlertRule save(AlertRule rule) {
        if (rule.getId() == null || rule.getId().isBlank()) {
            rule.setId(UUID.randomUUID().toString().substring(0, 8));
        }
        rules.put(rule.getId(), rule);
        return rule;
    }

    public Collection<AlertRule> all() { return rules.values(); }

    public AlertRule get(String id) { return rules.get(id); }

    public boolean delete(String id) {
        firing.remove(id);
        return rules.remove(id) != null;
    }

    public List<AlertEvent> recentEvents() { return new ArrayList<>(history); }

    public Set<String> firingIds() { return firing; }

    public record AlertEvent(String ruleId, String ruleName, long count, long threshold,
                             long timestamp, String state) {
        public String summary() {
            return ("FIRING".equals(state) ? "Alert firing: " : "Alert resolved: ")
                    + ruleName + " (" + count + " vs threshold " + threshold + ")";
        }
    }
}
