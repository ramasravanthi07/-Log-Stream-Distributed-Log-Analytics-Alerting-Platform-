package com.logstream.search;

import java.util.List;
import java.util.Map;

public class SearchResult {
    private String query;
    private long total;
    private double tookMs;
    private List<LogHit> hits = List.of();
    private List<SearchService.Bucket> histogram = List.of();
    private Map<String, Long> levelCounts = Map.of();
    private Map<String, Long> serviceCounts = Map.of();

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }

    public double getTookMs() { return tookMs; }
    public void setTookMs(double tookMs) { this.tookMs = tookMs; }

    public List<LogHit> getHits() { return hits; }
    public void setHits(List<LogHit> hits) { this.hits = hits; }

    public List<SearchService.Bucket> getHistogram() { return histogram; }
    public void setHistogram(List<SearchService.Bucket> histogram) { this.histogram = histogram; }

    public Map<String, Long> getLevelCounts() { return levelCounts; }
    public void setLevelCounts(Map<String, Long> levelCounts) { this.levelCounts = levelCounts; }

    public Map<String, Long> getServiceCounts() { return serviceCounts; }
    public void setServiceCounts(Map<String, Long> serviceCounts) { this.serviceCounts = serviceCounts; }
}
