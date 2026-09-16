package com.logstream.bench;

import com.logstream.proto.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Mid-review evidence generator. Streams synthetic microservice logs over gRPC
 * and reports sustained throughput.
 *
 *   mvn exec:java -Dexec.mainClass=com.logstream.bench.LoadGenerator \
 *       -Dexec.args="--total=1000000 --threads=8 --batch=2000"
 *
 * Roughly 4% of generated lines are ERRORs from billing-api with a response
 * time above 1000ms, which is exactly what the demo query looks for.
 */
public class LoadGenerator {

    static final String[] SERVICES = {
            "billing-api", "auth-service", "checkout-api", "inventory-svc",
            "notification-svc", "search-api", "user-profile", "payment-gateway"
    };
    static final String[] LEVELS = {"DEBUG", "INFO", "INFO", "INFO", "WARN", "ERROR"};
    static final String[] TEMPLATES = {
            "Request completed successfully for endpoint /v1/invoices",
            "Cache miss for key user:%d, falling back to database",
            "Connection pool exhausted, waiting for a free connection",
            "Timeout while calling downstream service payment-gateway",
            "Null pointer dereferenced while mapping invoice line items",
            "Slow database query detected on table transactions",
            "Retrying request after transient failure, attempt %d",
            "User session validated for tenant %d",
            "Deadlock detected and rolled back on table ledger_entries",
            "Disk usage on volume /var/log crossed 85 percent"
    };

    public static void main(String[] args) throws Exception {
        long total = argLong(args, "--total", 1_000_000);
        int threads = (int) argLong(args, "--threads", 8);
        int batch = (int) argLong(args, "--batch", 2000);
        String host = argStr(args, "--host", "localhost");
        int port = (int) argLong(args, "--port", 9090);
        long spreadMinutes = argLong(args, "--spread-minutes", 60);

        System.out.printf("Sending %,d logs over %d connections (batch=%d) to %s:%d%n",
                total, threads, batch, host, port);

        List<ManagedChannel> channels = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        AtomicLong sent = new AtomicLong();
        CountDownLatch done = new CountDownLatch(threads);
        long perThread = total / threads;
        long now = System.currentTimeMillis();
        long spreadMs = spreadMinutes * 60_000L;

        long start = System.nanoTime();
        for (int t = 0; t < threads; t++) {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                    .usePlaintext()
                    .maxInboundMessageSize(32 * 1024 * 1024)
                    .build();
            channels.add(channel);
            LogIngestServiceGrpc.LogIngestServiceBlockingStub stub =
                    LogIngestServiceGrpc.newBlockingStub(channel);

            pool.submit(() -> {
                Random rnd = new Random(Thread.currentThread().getId());
                try {
                    long remaining = perThread;
                    while (remaining > 0) {
                        int n = (int) Math.min(batch, remaining);
                        LogBatch.Builder b = LogBatch.newBuilder();
                        for (int i = 0; i < n; i++) {
                            b.addLogs(randomLog(rnd, now, spreadMs));
                        }
                        stub.ingestBatch(b.build());
                        remaining -= n;
                        long s = sent.addAndGet(n);
                        if (s % 100_000 < n) {
                            double secs = (System.nanoTime() - start) / 1e9;
                            System.out.printf("  %,d sent  |  %,.0f logs/sec%n", s, s / secs);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Sender failed: " + e);
                } finally {
                    done.countDown();
                }
            });
        }

        done.await();
        double elapsed = (System.nanoTime() - start) / 1e9;
        pool.shutdown();
        channels.forEach(c -> c.shutdownNow());

        System.out.println("---------------------------------------------");
        System.out.printf("Sent      : %,d logs%n", sent.get());
        System.out.printf("Elapsed   : %.2f s%n", elapsed);
        System.out.printf("Throughput: %,.0f logs/sec%n", sent.get() / elapsed);
        System.out.println("---------------------------------------------");
        System.out.println("Now run:  curl -X POST http://localhost:8080/api/refresh");
        System.out.println("Then   :  curl 'http://localhost:8080/api/search?q=level:ERROR%20AND%20service:billing-api%20AND%20response_time%20%3E%201000'");
    }

    static LogMessage randomLog(Random rnd, long now, long spreadMs) {
        String service = SERVICES[rnd.nextInt(SERVICES.length)];
        String level = LEVELS[rnd.nextInt(LEVELS.length)];
        // Make billing-api noisier so the demo query returns a meaningful slice.
        if (service.equals("billing-api") && rnd.nextInt(100) < 35) level = "ERROR";

        long responseTime = level.equals("ERROR")
                ? 800 + rnd.nextInt(4000)
                : 5 + rnd.nextInt(600);
        int status = switch (level) {
            case "ERROR" -> 500;
            case "WARN" -> 429;
            default -> 200;
        };

        String template = TEMPLATES[rnd.nextInt(TEMPLATES.length)];
        String message = template.contains("%d")
                ? String.format(template, rnd.nextInt(100_000))
                : template;

        return LogMessage.newBuilder()
                .setTimestamp(now - (long) (rnd.nextDouble() * spreadMs))
                .setLevel(level)
                .setService(service)
                .setHost(service + "-" + rnd.nextInt(6))
                .setMessage(message)
                .setResponseTime(responseTime)
                .setStatusCode(status)
                .setTraceId(Long.toHexString(rnd.nextLong()))
                .build();
    }

    static long argLong(String[] args, String key, long def) {
        String v = argStr(args, key, null);
        return v == null ? def : Long.parseLong(v);
    }

    static String argStr(String[] args, String key, String def) {
        for (String a : args) {
            if (a.startsWith(key + "=")) return a.substring(key.length() + 1);
        }
        return def;
    }
}
