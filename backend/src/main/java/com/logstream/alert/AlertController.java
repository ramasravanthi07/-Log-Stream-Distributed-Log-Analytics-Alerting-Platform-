package com.logstream.alert;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertEngine engine;

    public AlertController(AlertEngine engine) {
        this.engine = engine;
    }

    @GetMapping
    public Map<String, Object> list() {
        Collection<AlertRule> rules = engine.all();
        return Map.of(
                "rules", rules,
                "firing", engine.firingIds(),
                "events", engine.recentEvents());
    }

    @PostMapping
    public AlertRule create(@RequestBody AlertRule rule) {
        return engine.save(rule);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody AlertRule rule) {
        if (engine.get(id) == null) return ResponseEntity.notFound().build();
        rule.setId(id);
        return ResponseEntity.ok(engine.save(rule));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        return engine.delete(id)
                ? ResponseEntity.ok(Map.of("deleted", id))
                : ResponseEntity.notFound().build();
    }

    /** Runs one rule immediately -- handy when a reviewer wants to see an alert fire on demand. */
    @PostMapping("/{id}/evaluate")
    public ResponseEntity<?> evaluateNow(@PathVariable String id) {
        AlertRule rule = engine.get(id);
        if (rule == null) return ResponseEntity.notFound().build();
        try {
            AlertEngine.AlertEvent ev = engine.evaluate(rule);
            return ResponseEntity.ok(Map.of(
                    "rule", rule,
                    "stateChanged", ev != null,
                    "event", ev == null ? Map.of() : ev));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/events")
    public List<AlertEngine.AlertEvent> events() {
        return engine.recentEvents();
    }
}
