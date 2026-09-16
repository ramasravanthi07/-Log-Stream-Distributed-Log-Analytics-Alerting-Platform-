package com.logstream.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.62.2)",
    comments = "Source: log.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class LogIngestServiceGrpc {

  private LogIngestServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "logstream.LogIngestService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.logstream.proto.LogMessage,
      com.logstream.proto.IngestSummary> getIngestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Ingest",
      requestType = com.logstream.proto.LogMessage.class,
      responseType = com.logstream.proto.IngestSummary.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<com.logstream.proto.LogMessage,
      com.logstream.proto.IngestSummary> getIngestMethod() {
    io.grpc.MethodDescriptor<com.logstream.proto.LogMessage, com.logstream.proto.IngestSummary> getIngestMethod;
    if ((getIngestMethod = LogIngestServiceGrpc.getIngestMethod) == null) {
      synchronized (LogIngestServiceGrpc.class) {
        if ((getIngestMethod = LogIngestServiceGrpc.getIngestMethod) == null) {
          LogIngestServiceGrpc.getIngestMethod = getIngestMethod =
              io.grpc.MethodDescriptor.<com.logstream.proto.LogMessage, com.logstream.proto.IngestSummary>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Ingest"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.logstream.proto.LogMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.logstream.proto.IngestSummary.getDefaultInstance()))
              .setSchemaDescriptor(new LogIngestServiceMethodDescriptorSupplier("Ingest"))
              .build();
        }
      }
    }
    return getIngestMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.logstream.proto.LogBatch,
      com.logstream.proto.IngestSummary> getIngestBatchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "IngestBatch",
      requestType = com.logstream.proto.LogBatch.class,
      responseType = com.logstream.proto.IngestSummary.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.logstream.proto.LogBatch,
      com.logstream.proto.IngestSummary> getIngestBatchMethod() {
    io.grpc.MethodDescriptor<com.logstream.proto.LogBatch, com.logstream.proto.IngestSummary> getIngestBatchMethod;
    if ((getIngestBatchMethod = LogIngestServiceGrpc.getIngestBatchMethod) == null) {
      synchronized (LogIngestServiceGrpc.class) {
        if ((getIngestBatchMethod = LogIngestServiceGrpc.getIngestBatchMethod) == null) {
          LogIngestServiceGrpc.getIngestBatchMethod = getIngestBatchMethod =
              io.grpc.MethodDescriptor.<com.logstream.proto.LogBatch, com.logstream.proto.IngestSummary>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "IngestBatch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.logstream.proto.LogBatch.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.logstream.proto.IngestSummary.getDefaultInstance()))
              .setSchemaDescriptor(new LogIngestServiceMethodDescriptorSupplier("IngestBatch"))
              .build();
        }
      }
    }
    return getIngestBatchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.logstream.proto.HealthRequest,
      com.logstream.proto.HealthReply> getHealthMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Health",
      requestType = com.logstream.proto.HealthRequest.class,
      responseType = com.logstream.proto.HealthReply.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.logstream.proto.HealthRequest,
      com.logstream.proto.HealthReply> getHealthMethod() {
    io.grpc.MethodDescriptor<com.logstream.proto.HealthRequest, com.logstream.proto.HealthReply> getHealthMethod;
    if ((getHealthMethod = LogIngestServiceGrpc.getHealthMethod) == null) {
      synchronized (LogIngestServiceGrpc.class) {
        if ((getHealthMethod = LogIngestServiceGrpc.getHealthMethod) == null) {
          LogIngestServiceGrpc.getHealthMethod = getHealthMethod =
              io.grpc.MethodDescriptor.<com.logstream.proto.HealthRequest, com.logstream.proto.HealthReply>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Health"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.logstream.proto.HealthRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.logstream.proto.HealthReply.getDefaultInstance()))
              .setSchemaDescriptor(new LogIngestServiceMethodDescriptorSupplier("Health"))
              .build();
        }
      }
    }
    return getHealthMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static LogIngestServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LogIngestServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LogIngestServiceStub>() {
        @java.lang.Override
        public LogIngestServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LogIngestServiceStub(channel, callOptions);
        }
      };
    return LogIngestServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static LogIngestServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LogIngestServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LogIngestServiceBlockingStub>() {
        @java.lang.Override
        public LogIngestServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LogIngestServiceBlockingStub(channel, callOptions);
        }
      };
    return LogIngestServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static LogIngestServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<LogIngestServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<LogIngestServiceFutureStub>() {
        @java.lang.Override
        public LogIngestServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new LogIngestServiceFutureStub(channel, callOptions);
        }
      };
    return LogIngestServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * Client-streaming: one long-lived connection per microservice.
     * </pre>
     */
    default io.grpc.stub.StreamObserver<com.logstream.proto.LogMessage> ingest(
        io.grpc.stub.StreamObserver<com.logstream.proto.IngestSummary> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getIngestMethod(), responseObserver);
    }

    /**
     * <pre>
     * Unary batch: fewer round trips, used by the load generator.
     * </pre>
     */
    default void ingestBatch(com.logstream.proto.LogBatch request,
        io.grpc.stub.StreamObserver<com.logstream.proto.IngestSummary> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getIngestBatchMethod(), responseObserver);
    }

    /**
     */
    default void health(com.logstream.proto.HealthRequest request,
        io.grpc.stub.StreamObserver<com.logstream.proto.HealthReply> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHealthMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service LogIngestService.
   */
  public static abstract class LogIngestServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return LogIngestServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service LogIngestService.
   */
  public static final class LogIngestServiceStub
      extends io.grpc.stub.AbstractAsyncStub<LogIngestServiceStub> {
    private LogIngestServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LogIngestServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LogIngestServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * Client-streaming: one long-lived connection per microservice.
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.logstream.proto.LogMessage> ingest(
        io.grpc.stub.StreamObserver<com.logstream.proto.IngestSummary> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getIngestMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Unary batch: fewer round trips, used by the load generator.
     * </pre>
     */
    public void ingestBatch(com.logstream.proto.LogBatch request,
        io.grpc.stub.StreamObserver<com.logstream.proto.IngestSummary> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getIngestBatchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void health(com.logstream.proto.HealthRequest request,
        io.grpc.stub.StreamObserver<com.logstream.proto.HealthReply> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHealthMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service LogIngestService.
   */
  public static final class LogIngestServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<LogIngestServiceBlockingStub> {
    private LogIngestServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LogIngestServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LogIngestServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Unary batch: fewer round trips, used by the load generator.
     * </pre>
     */
    public com.logstream.proto.IngestSummary ingestBatch(com.logstream.proto.LogBatch request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getIngestBatchMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.logstream.proto.HealthReply health(com.logstream.proto.HealthRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHealthMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service LogIngestService.
   */
  public static final class LogIngestServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<LogIngestServiceFutureStub> {
    private LogIngestServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected LogIngestServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new LogIngestServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Unary batch: fewer round trips, used by the load generator.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.logstream.proto.IngestSummary> ingestBatch(
        com.logstream.proto.LogBatch request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getIngestBatchMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.logstream.proto.HealthReply> health(
        com.logstream.proto.HealthRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHealthMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_INGEST_BATCH = 0;
  private static final int METHODID_HEALTH = 1;
  private static final int METHODID_INGEST = 2;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_INGEST_BATCH:
          serviceImpl.ingestBatch((com.logstream.proto.LogBatch) request,
              (io.grpc.stub.StreamObserver<com.logstream.proto.IngestSummary>) responseObserver);
          break;
        case METHODID_HEALTH:
          serviceImpl.health((com.logstream.proto.HealthRequest) request,
              (io.grpc.stub.StreamObserver<com.logstream.proto.HealthReply>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_INGEST:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.ingest(
              (io.grpc.stub.StreamObserver<com.logstream.proto.IngestSummary>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getIngestMethod(),
          io.grpc.stub.ServerCalls.asyncClientStreamingCall(
            new MethodHandlers<
              com.logstream.proto.LogMessage,
              com.logstream.proto.IngestSummary>(
                service, METHODID_INGEST)))
        .addMethod(
          getIngestBatchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.logstream.proto.LogBatch,
              com.logstream.proto.IngestSummary>(
                service, METHODID_INGEST_BATCH)))
        .addMethod(
          getHealthMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.logstream.proto.HealthRequest,
              com.logstream.proto.HealthReply>(
                service, METHODID_HEALTH)))
        .build();
  }

  private static abstract class LogIngestServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    LogIngestServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.logstream.proto.LogProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("LogIngestService");
    }
  }

  private static final class LogIngestServiceFileDescriptorSupplier
      extends LogIngestServiceBaseDescriptorSupplier {
    LogIngestServiceFileDescriptorSupplier() {}
  }

  private static final class LogIngestServiceMethodDescriptorSupplier
      extends LogIngestServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    LogIngestServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (LogIngestServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new LogIngestServiceFileDescriptorSupplier())
              .addMethod(getIngestMethod())
              .addMethod(getIngestBatchMethod())
              .addMethod(getHealthMethod())
              .build();
        }
      }
    }
    return result;
  }
}
