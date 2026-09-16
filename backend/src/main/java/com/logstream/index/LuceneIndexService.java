package com.logstream.index;

import com.logstream.proto.LogMessage;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Owns the single Lucene index: one IndexWriter, a bounded hand-off queue and a
 * pool of indexer threads. gRPC handler threads never touch Lucene directly --
 * they drop documents on the queue and return, which is what keeps ingest
 * latency flat under load.
 *
 * Reads go through a SearcherManager kept near-real-time by a reopen thread, so
 * a log line is searchable within ~200ms of arriving without ever calling
 * commit() on the hot path.
 */
@Service
public class LuceneIndexService {

    private static final Logger log = LoggerFactory.getLogger(LuceneIndexService.class);

    @Value("${logstream.index.path:./data/index}")
    private String indexPath;

    @Value("${logstream.index.writer-threads:4}")
    private int writerThreads;

    @Value("${logstream.index.queue-capacity:200000}")
    private int queueCapacity;

    @Value("${logstream.index.ram-buffer-mb:512}")
    private double ramBufferMb;

    private IndexWriter writer;
    private SearcherManager searcherManager;
    private ControlledRealTimeReopenThread<IndexSearcher> reopenThread;
    private BlockingQueue<LogMessage> queue;
    private ExecutorService indexers;
    private ScheduledExecutorService committer;

    private final Analyzer analyzer = new StandardAnalyzer();
    private final AtomicLong indexed = new AtomicLong();
    private final AtomicLong dropped = new AtomicLong();
    private volatile boolean running = true;

    @PostConstruct
    public void start() throws Exception {
        Path path = Paths.get(indexPath);
        FSDirectory dir = FSDirectory.open(path);

        IndexWriterConfig cfg = new IndexWriterConfig(analyzer);
        cfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        cfg.setRAMBufferSizeMB(ramBufferMb);
        // Logs are append-only, so we never need to look up an existing doc id.
        cfg.setMaxBufferedDocs(IndexWriterConfig.DISABLE_AUTO_FLUSH);
        cfg.setUseCompoundFile(false);

        writer = new IndexWriter(dir, cfg);
        searcherManager = new SearcherManager(writer, true, true, new SearcherFactory());

        // Refresh at most every 200ms, at least every 5s.
        reopenThread = new ControlledRealTimeReopenThread<>(writer, searcherManager, 5.0, 0.2);
        reopenThread.setDaemon(true);
        reopenThread.setName("lucene-nrt-reopen");
        reopenThread.start();

        queue = new ArrayBlockingQueue<>(queueCapacity);
        indexers = Executors.newFixedThreadPool(writerThreads, r -> {
            Thread t = new Thread(r);
            t.setName("lucene-indexer-" + t.getId());
            t.setDaemon(true);
            return t;
        });
        for (int i = 0; i < writerThreads; i++) {
            indexers.submit(this::drainLoop);
        }

        committer = Executors.newSingleThreadScheduledExecutor();
        committer.scheduleWithFixedDelay(this::commitQuietly, 10, 10, TimeUnit.SECONDS);

        log.info("Lucene index ready at {} ({} indexer threads, {}MB RAM buffer, {} docs already on disk)",
                path.toAbsolutePath(), writerThreads, (long) ramBufferMb, docCount());
    }

    /** Non-blocking offer used by the gRPC ingest path. Returns false if the queue is saturated. */
    public boolean submit(LogMessage msg) {
        if (!running) return false;
        boolean ok = queue.offer(msg);
        if (!ok) dropped.incrementAndGet();
        return ok;
    }

    public int submitAll(List<LogMessage> msgs) {
        int accepted = 0;
        for (LogMessage m : msgs) {
            if (submit(m)) accepted++;
        }
        return accepted;
    }

    private void drainLoop() {
        while (running || !queue.isEmpty()) {
            try {
                LogMessage msg = queue.poll(200, TimeUnit.MILLISECONDS);
                if (msg == null) continue;
                writer.addDocument(toDocument(msg));
                indexed.incrementAndGet();
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Indexing failed for one document: {}", e.toString());
            }
        }
    }

    /**
     * Field strategy, which is the whole trick to sub-50ms queries:
     *  - StringField  -> exact keyword terms (level, service, host, trace_id)
     *  - TextField    -> analyzed full text (message)
     *  - LongPoint    -> BKD-tree range search for numerics
     *  - DocValues    -> column store for sorting and histogram bucketing
     *  - StoredField  -> only what we need to render a result row
     */
    public static Document toDocument(LogMessage m) {
        Document doc = new Document();

        long ts = m.getTimestamp() > 0 ? m.getTimestamp() : System.currentTimeMillis();
        doc.add(new LongPoint(Fields.TIMESTAMP, ts));
        doc.add(new SortedNumericDocValuesField(Fields.TIMESTAMP, ts));
        doc.add(new StoredField(Fields.TIMESTAMP, ts));

        String level = m.getLevel().isEmpty() ? "INFO" : m.getLevel().toUpperCase();
        doc.add(new StringField(Fields.LEVEL, level, Field.Store.YES));
        doc.add(new SortedDocValuesField(Fields.LEVEL, new BytesRef(level)));

        if (!m.getService().isEmpty()) {
            doc.add(new StringField(Fields.SERVICE, m.getService(), Field.Store.YES));
            doc.add(new SortedDocValuesField(Fields.SERVICE, new BytesRef(m.getService())));
        }
        if (!m.getHost().isEmpty()) {
            doc.add(new StringField(Fields.HOST, m.getHost(), Field.Store.YES));
        }
        if (!m.getTraceId().isEmpty()) {
            doc.add(new StringField(Fields.TRACE_ID, m.getTraceId(), Field.Store.YES));
        }
        if (!m.getMessage().isEmpty()) {
            doc.add(new TextField(Fields.MESSAGE, m.getMessage(), Field.Store.YES));
        }

        doc.add(new LongPoint(Fields.RESPONSE_TIME, m.getResponseTime()));
        doc.add(new SortedNumericDocValuesField(Fields.RESPONSE_TIME, m.getResponseTime()));
        doc.add(new StoredField(Fields.RESPONSE_TIME, m.getResponseTime()));

        doc.add(new LongPoint(Fields.STATUS_CODE, m.getStatusCode()));
        doc.add(new StoredField(Fields.STATUS_CODE, m.getStatusCode()));

        return doc;
    }

    public IndexSearcher acquire() throws Exception {
        return searcherManager.acquire();
    }

    public void release(IndexSearcher s) {
        try {
            searcherManager.release(s);
        } catch (Exception e) {
            log.warn("Searcher release failed: {}", e.toString());
        }
    }

    public Analyzer analyzer() {
        return analyzer;
    }

    public long indexedCount() {
        return indexed.get();
    }

    public long droppedCount() {
        return dropped.get();
    }

    public int queueDepth() {
        return queue == null ? 0 : queue.size();
    }

    public long docCount() {
        try {
            IndexSearcher s = acquire();
            try {
                return s.getIndexReader().numDocs();
            } finally {
                release(s);
            }
        } catch (Exception e) {
            return -1;
        }
    }

    /** Waits until the queue has drained -- used by the benchmark to time a full run honestly. */
    public void awaitDrain() throws InterruptedException {
        while (!queue.isEmpty()) Thread.sleep(20);
    }

    public void forceRefresh() {
        try {
            searcherManager.maybeRefreshBlocking();
        } catch (Exception e) {
            log.warn("Refresh failed: {}", e.toString());
        }
    }

    private void commitQuietly() {
        try {
            if (writer.hasUncommittedChanges()) writer.commit();
        } catch (Exception e) {
            log.warn("Commit failed: {}", e.toString());
        }
    }

    @PreDestroy
    public void stop() throws Exception {
        running = false;
        if (indexers != null) {
            indexers.shutdown();
            indexers.awaitTermination(30, TimeUnit.SECONDS);
        }
        if (committer != null) committer.shutdownNow();
        if (reopenThread != null) reopenThread.close();
        if (searcherManager != null) searcherManager.close();
        if (writer != null) {
            writer.commit();
            writer.close();
        }
        log.info("Index closed cleanly. Indexed={} dropped={}", indexed.get(), dropped.get());
    }
}
