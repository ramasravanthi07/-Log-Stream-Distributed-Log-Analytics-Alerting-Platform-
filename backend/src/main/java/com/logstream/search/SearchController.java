package com.logstream.search;

import com.logstream.index.LogQueryParser;
import com.logstream.index.LuceneIndexService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService search;
    private final LuceneIndexService index;

    public SearchController(SearchService search, LuceneIndexService index) {
        this.search = search;
        this.index = index;
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam(defaultValue = "") String q,
                                    @RequestParam(defaultValue = "0") int from,
                                    @RequestParam(defaultValue = "50") int size,
                                    @RequestParam(defaultValue = "true") boolean histogram,
                                    @RequestParam(defaultValue = "60") int buckets) {
        try {
            return ResponseEntity.ok(search.search(q, from, Math.min(size, 500), histogram, buckets));
        } catch (LogQueryParser.QuerySyntaxException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage(), "query", q));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.toString()));
        }
    }

    /** Lets the search bar show a syntax error before the user hits enter. */
    @GetMapping("/validate")
    public Map<String, Object> validate(@RequestParam String q) {
        try {
            new LogQueryParser(index.analyzer()).parse(q);
            return Map.of("valid", true);
        } catch (LogQueryParser.QuerySyntaxException e) {
            return Map.of("valid", false, "error", e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("documents", index.docCount());
        m.put("indexedThisRun", index.indexedCount());
        m.put("dropped", index.droppedCount());
        m.put("queueDepth", index.queueDepth());
        return m;
    }

    /** Forces an NRT refresh; the benchmark calls this before measuring search latency. */
    @PostMapping("/refresh")
    public Map<String, Object> refresh() {
        index.forceRefresh();
        return Map.of("documents", index.docCount());
    }
}
