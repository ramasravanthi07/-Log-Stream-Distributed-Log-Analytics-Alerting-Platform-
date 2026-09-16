package com.logstream.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Runs the gRPC server on its own port next to the Spring MVC REST API. */
@Component
public class GrpcServerRunner {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerRunner.class);

    @Value("${logstream.grpc.port:9090}")
    private int port;

    private final LogIngestServiceImpl service;
    private Server server;

    public GrpcServerRunner(LogIngestServiceImpl service) {
        this.service = service;
    }

    @PostConstruct
    public void start() throws Exception {
        server = ServerBuilder.forPort(port)
                .addService(service)
                .maxInboundMessageSize(32 * 1024 * 1024)   // batches of 10k log lines
                .build()
                .start();
        log.info("gRPC ingest listening on port {}", port);
    }

    @PreDestroy
    public void stop() throws Exception {
        if (server != null) {
            server.shutdown().awaitTermination(15, TimeUnit.SECONDS);
        }
    }
}
