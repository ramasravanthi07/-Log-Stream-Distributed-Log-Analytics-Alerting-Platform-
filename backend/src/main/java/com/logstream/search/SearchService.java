package com.logstream.search;

import com.logstream.index.Fields;
import com.logstream.index.LogQueryParser;
import com.logstream.index.LuceneIndexService;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/** Everything the dashboard asks of the index: hit list, histogram, facets. */
@Service
public class SearchService {

    private final LuceneIndexService index;

    public SearchService(LuceneIndexService index) {
        this.index = index;
    }

    public SearchResult search(String queryString, int from, int size, boolean withHistogram, int buckets) throws Exception {
        long t0 = System.nanoTime();
        Query query = new LogQueryParser(index.analyzer()).parse(queryString);

        IndexSearcher searcher = index.acquire();
        try {
            Sort sort = new Sort(new SortedNumericSortField(Fields.TIMESTAMP, SortField.Type.LONG, true));
            int need = Math.min(from + size, 10_000);
            TopFieldCollector top = TopFieldCollector.create(sort, Math.max(need, 1), Integer.MAX_VALUE);

            HistogramCollector hist = null;
            FacetCollector facets = new FacetCollector();
            Collector collector;
            if (withHistogram) {
                hist = new HistogramCollector(buckets);
                collector = MultiCollector.wrap(top, hist, facets);
            } else {
                collector = MultiCollector.wrap(top, facets);
            }

            searcher.search(query, collector);

            TopDocs docs = top.topDocs();
            List<LogHit> hits = new ArrayList<>();
            StoredFields stored = searcher.storedFields();
            for (int i = from; i < docs.scoreDocs.length; i++) {
                Document d = stored.document(docs.scoreDocs[i].doc);
                hits.add(LogHit.from(d));
            }

            SearchResult result = new SearchResult();
            result.setQuery(queryString);
            result.setTotal(docs.totalHits.value);
            result.setHits(hits);
            result.setLevelCounts(facets.levels());
            result.setServiceCounts(facets.services());
            if (hist != null) result.setHistogram(hist.toBuckets());
            result.setTookMs((System.nanoTime() - t0) / 1_000_000.0);
            return result;
        } finally {
            index.release(searcher);
        }
    }

    /** Used by the alerting engine: count only, no documents materialised. */
    public long count(String queryString) throws Exception {
        Query query = new LogQueryParser(index.analyzer()).parse(queryString);
        IndexSearcher searcher = index.acquire();
        try {
            return searcher.count(query);
        } finally {
            index.release(searcher);
        }
    }

    // ------------------------------------------------------------ collectors

    /**
     * Buckets matching documents by timestamp in one pass over the hits. Reads
     * from the DocValues column store, so it never loads a stored document.
     */
    static final class HistogramCollector implements Collector {
        private final int bucketCount;
        private long[] values = new long[4096];
        private int size = 0;
        private long min = Long.MAX_VALUE;
        private long max = Long.MIN_VALUE;

        HistogramCollector(int bucketCount) {
            this.bucketCount = Math.max(1, Math.min(bucketCount, 200));
        }

        private void add(long ts) {
            if (size == values.length) values = Arrays.copyOf(values, values.length * 2);
            values[size++] = ts;
            if (ts < min) min = ts;
            if (ts > max) max = ts;
        }

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
            SortedNumericDocValues dv = context.reader().getSortedNumericDocValues(Fields.TIMESTAMP);
            return new LeafCollector() {
                @Override public void setScorer(Scorable scorer) {}

                @Override
                public void collect(int doc) throws IOException {
                    if (dv == null || !dv.advanceExact(doc)) return;
                    add(dv.nextValue());
                }
            };
        }

        @Override
        public ScoreMode scoreMode() {
            return ScoreMode.COMPLETE_NO_SCORES;
        }

        List<Bucket> toBuckets() {
            List<Bucket> out = new ArrayList<>();
            if (size == 0) return out;
            long span = Math.max(1, max - min + 1);
            long width = Math.max(1, (long) Math.ceil(span / (double) bucketCount));
            long[] counts = new long[bucketCount];
            for (int i = 0; i < size; i++) {
                int idx = (int) Math.min(bucketCount - 1, (values[i] - min) / width);
                counts[idx]++;
            }
            for (int i = 0; i < bucketCount; i++) {
                out.add(new Bucket(min + i * width, counts[i]));
            }
            return out;
        }
    }

    /** Counts hits per level and per service so the UI can show a breakdown. */
    static final class FacetCollector implements Collector {
        private final Map<String, Long> levels = new HashMap<>();
        private final Map<String, Long> services = new HashMap<>();

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext context) throws IOException {
            var levelDv = context.reader().getSortedDocValues(Fields.LEVEL);
            var serviceDv = context.reader().getSortedDocValues(Fields.SERVICE);
            return new LeafCollector() {
                @Override public void setScorer(Scorable scorer) {}

                @Override
                public void collect(int doc) throws IOException {
                    if (levelDv != null && levelDv.advanceExact(doc)) {
                        String v = levelDv.lookupOrd(levelDv.ordValue()).utf8ToString();
                        levels.merge(v, 1L, Long::sum);
                    }
                    if (serviceDv != null && serviceDv.advanceExact(doc)) {
                        String v = serviceDv.lookupOrd(serviceDv.ordValue()).utf8ToString();
                        services.merge(v, 1L, Long::sum);
                    }
                }
            };
        }

        @Override
        public ScoreMode scoreMode() {
            return ScoreMode.COMPLETE_NO_SCORES;
        }

        Map<String, Long> levels() { return levels; }

        Map<String, Long> services() {
            return services.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(12)
                    .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
        }
    }

    public record Bucket(long timestamp, long count) {}
}
