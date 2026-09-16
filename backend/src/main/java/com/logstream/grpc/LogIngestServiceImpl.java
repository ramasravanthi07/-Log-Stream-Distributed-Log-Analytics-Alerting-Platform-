package com.logstream.grpc;

import com.logstream.index.LuceneIndexService;
import com.logstream.proto.*;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * gRPC entry point. Handler threads do no I/O: they hand each message to the
 * indexer queue and return, so a slow segment merge can never back-pressure the
 * wire beyond the queue depth.
 */
@Component
public class LogIngestServiceImpl extends LogIngestServiceGrpc.LogIngestServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(LogIngestServiceImpl.class);

    private final LuceneIndexService index;

    public LogIngestServiceImpl(LuceneIndexService index) {
        this.index = index;
    }

    /** Client-streaming: each microservice opens one connection and keeps pushing. */
    @Override
    public StreamObserver<LogMessage> ingest(StreamObserver<IngestSummary> responseObserver) {
        return new StreamObserver<>() {
            final long start = System.nanoTime();
            long accepted = 0;
            long rejected = 0;

            @Override
            public void onNext(LogMessage msg) {
                if (index.submit(msg)) accepted++;
                else rejected++;
            }

            @Override
            public void onError(Throwable t) {
                log.warn("Ingest stream failed after {} messages: {}", accepted, t.toString());
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(summary(accepted, rejected, start));
                responseObserver.onCompleted();
            }
        };
    }

    /** Unary batch path: one round trip per few thousand logs. */
    @Override
    public void ingestBatch(LogBatch request, StreamObserver<IngestSummary> responseObserver) {
        long start = System.nanoTime();
        int accepted = index.submitAll(request.getLogsList());
        int rejected = request.getLogsCount() - accepted;
        responseObserver.onNext(summary(accepted, rejected, start));
        responseObserver.onCompleted();
    }

    @Override
    public void health(HealthRequest request, StreamObserver<HealthReply> responseObserver) {
        responseObserver.onNext(HealthReply.newBuilder()
                .setIndexedDocs(index.docCount())
                .setQueueDepth(index.queueDepth())
                .setStatus("UP")
                .build());
        responseObserver.onCompleted();
    }

    private static IngestSummary summary(long accepted, long rejected, long startNanos) {
        long elapsedMs = Math.max(1, (System.nanoTime() - startNanos) / 1_000_000);
        return IngestSummary.newBuilder()
                .setAccepted(accepted)
                .setRejected(rejected)
                .setElapsedMs(elapsedMs)
                .setLogsPerSec(accepted * 1000.0 / elapsedMs)
                .build();
    }
}
